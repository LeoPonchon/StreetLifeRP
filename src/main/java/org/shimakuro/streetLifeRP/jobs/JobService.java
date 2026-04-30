package org.shimakuro.streetLifeRP.jobs;

import org.bukkit.configuration.ConfigurationSection;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class JobService {
    private final PlayerDataRepository repo;
    private volatile Map<JobType, JobConfig> jobConfigs = new EnumMap<>(JobType.class);

    public JobService(PlayerDataRepository repo) {
        this.repo = repo;
    }

    public void reloadFromConfig(ConfigurationSection section) {
        EnumMap<JobType, JobConfig> next = new EnumMap<>(JobType.class);
        for (JobType type : JobType.values()) {
            ConfigurationSection s = section != null ? section.getConfigurationSection(type.name().toLowerCase()) : null;
            double salary = s != null ? s.getDouble("salary", 0.0) : 0.0;
            long cooldownSeconds = s != null ? s.getLong("cooldown_seconds", 300L) : 300L;
            String displayName = s != null ? s.getString("display_name") : null;
            if (displayName == null) displayName = type.name();
            next.put(type, new JobConfig(salary, cooldownSeconds, displayName));
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
        var data = repo.get(uuid);
        data.setJob(type.name());
        repo.save(data);
    }

    public double salary(JobType type) {
        return jobConfigs.getOrDefault(type, new JobConfig(0.0, 300L, type.name())).salary();
    }

    public long cooldownSeconds(JobType type) {
        return jobConfigs.getOrDefault(type, new JobConfig(0.0, 300L, type.name())).cooldownSeconds();
    }

    public String displayName(JobType type) {
        return jobConfigs.getOrDefault(type, new JobConfig(0.0, 300L, type.name())).displayName();
    }

    private record JobConfig(double salary, long cooldownSeconds, String displayName) {}
}
