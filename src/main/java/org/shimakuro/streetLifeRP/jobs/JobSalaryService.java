package org.shimakuro.streetLifeRP.jobs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.shimakuro.streetLifeRP.core.config.ConfigService;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.economy.EconomyService;

public final class JobSalaryService implements Runnable {
    private static final long DEFAULT_INTERVAL_MILLIS = 30L * 60L * 1000L;
    private static final long DEFAULT_TICK_PERIOD_TICKS = 20L * 60L;

    private final JavaPlugin plugin;
    private final ConfigService config;
    private final PlayerDataRepository repo;
    private final JobService jobs;
    private final EconomyService economy;

    private int taskId = -1;

    public JobSalaryService(JavaPlugin plugin, ConfigService config, PlayerDataRepository repo, JobService jobs, EconomyService economy) {
        this.plugin = plugin;
        this.config = config;
        this.repo = repo;
        this.jobs = jobs;
        this.economy = economy;
    }

    public void enable() {
        disable();
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, DEFAULT_TICK_PERIOD_TICKS, DEFAULT_TICK_PERIOD_TICKS);
    }

    public void disable() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        String prefix = config.prefix();

        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerData data = repo.get(player.getUniqueId());
            if (!data.hasCharacter()) continue;

            JobType job = jobs.get(player.getUniqueId());
            if (job == JobType.DEALER) continue;
            double salary = jobs.salary(job);
            if (salary == 0.0) continue;

            long last = data.lastWorkAtMillis();
            if (last <= 0L) {
                data.setLastWorkAtMillis(now);
                repo.save(data);
                continue;
            }

            if ((now - last) < DEFAULT_INTERVAL_MILLIS) continue;

            data.setLastWorkAtMillis(now);
            repo.save(data);

            double applied = economy.addCashSigned(player.getUniqueId(), salary, "job_salary:" + job.name());
            if (applied == 0.0) continue;

            String sign = applied < 0 ? "-" : "+";
            player.sendMessage(prefix + ChatColor.DARK_GRAY + "Salaire (" + jobs.displayName(job) + "): " + sign + economy.format(Math.abs(applied)));
        }
    }
}
