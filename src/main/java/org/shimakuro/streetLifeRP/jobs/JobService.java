package org.shimakuro.streetLifeRP.jobs;

import org.bukkit.configuration.ConfigurationSection;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseAction;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseService;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.economy.EconomyService;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class JobService {
    private final PlayerDataRepository repo;
    private final AntiAbuseService antiAbuse;
    private final EconomyService economy;
    private volatile Map<JobType, JobConfig> jobConfigs = new EnumMap<>(JobType.class);

    public JobService(PlayerDataRepository repo, AntiAbuseService antiAbuse, EconomyService economy) {
        this.repo = repo;
        this.antiAbuse = antiAbuse;
        this.economy = economy;
    }

    public void reloadFromConfig(ConfigurationSection section) {
        EnumMap<JobType, JobConfig> next = new EnumMap<>(JobType.class);
        for (JobType type : JobType.values()) {
            ConfigurationSection s = section != null ? section.getConfigurationSection(type.name().toLowerCase()) : null;
            double salary = s != null ? s.getDouble("salary", 0.0) : 0.0;
            long cooldownSeconds = s != null ? s.getLong("cooldown_seconds", 300L) : 300L;
            next.put(type, new JobConfig(salary, cooldownSeconds));
        }
        jobConfigs = next;
    }

    public JobType get(UUID uuid) {
        String raw = repo.get(uuid).job();
        if (raw == null || raw.isBlank()) return JobType.UNEMPLOYED;
        try {
            return JobType.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return JobType.UNEMPLOYED;
        }
    }

    public void set(UUID uuid, JobType type) {
        PlayerData data = repo.get(uuid);
        data.setJob(type.name());
        repo.save(data);
    }

    public WorkResult work(UUID uuid) {
        if (!antiAbuse.allowAndMark(uuid, AntiAbuseAction.JOB_WORK)) {
            return WorkResult.TOO_FAST;
        }

        JobType type = get(uuid);
        JobConfig cfg = jobConfigs.getOrDefault(type, new JobConfig(0.0, 300L));

        long now = System.currentTimeMillis();
        PlayerData data = repo.get(uuid);
        long last = data.lastWorkAtMillis();
        if (last > 0 && (now - last) < (cfg.cooldownSeconds() * 1000L)) {
            long remaining = (cfg.cooldownSeconds() * 1000L - (now - last) + 999) / 1000;
            return WorkResult.cooldown(remaining);
        }

        data.setLastWorkAtMillis(now);
        repo.save(data);
        if (cfg.salary() > 0) {
            economy.addCash(uuid, cfg.salary(), "job_work:" + type.name());
        }
        return WorkResult.paid(cfg.salary());
    }

    private record JobConfig(double salary, long cooldownSeconds) {}

    public sealed interface WorkResult permits WorkResultPaid, WorkResultCooldown, WorkResultTooFast {
        static WorkResult paid(double amount) {
            return new WorkResultPaid(amount);
        }

        static WorkResult cooldown(long secondsRemaining) {
            return new WorkResultCooldown(secondsRemaining);
        }

        WorkResult TOO_FAST = new WorkResultTooFast();
    }

    public record WorkResultPaid(double amount) implements WorkResult {}

    public record WorkResultCooldown(long secondsRemaining) implements WorkResult {}

    public static final class WorkResultTooFast implements WorkResult {}
}

