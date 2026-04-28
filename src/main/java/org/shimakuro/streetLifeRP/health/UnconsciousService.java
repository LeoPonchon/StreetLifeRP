package org.shimakuro.streetLifeRP.health;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;

public final class UnconsciousService implements Runnable {
    private static final int SLOWNESS_AMP = 255;
    private static final double UNCONSCIOUS_HEALTH = 1.0;

    private final JavaPlugin plugin;
    private final PlayerDataRepository repo;
    private final String prefix;
    private final UnconsciousPoseService pose;

    private int taskId = -1;

    public UnconsciousService(JavaPlugin plugin, PlayerDataRepository repo, String prefix) {
        this.plugin = plugin;
        this.repo = repo;
        this.prefix = prefix;
        this.pose = new UnconsciousPoseService();
    }

    public void enable() {
        disable();
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 20L, 20L);
    }

    public void disable() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public boolean isUnconscious(Player player) {
        return repo.get(player.getUniqueId()).unconscious();
    }

    public void knockOut(Player player) {
        PlayerData data = repo.get(player.getUniqueId());
        if (data.unconscious()) return;

        data.setUnconscious(true);
        data.setUnconsciousAtMillis(System.currentTimeMillis());
        repo.save(data);

        applyState(player);
        pose.setCrawlPose(player, true);
        player.sendMessage(prefix + ChatColor.RED + "Vous êtes inconscient.");
        player.sendMessage(prefix + ChatColor.DARK_GRAY + "Choix: respawn (bouton téléphone) ou attendre un EMS (911).");
    }

    public void reviveToConscious(Player player) {
        PlayerData data = repo.get(player.getUniqueId());
        if (!data.unconscious()) return;

        data.setUnconscious(false);
        data.setUnconsciousAtMillis(0L);
        repo.save(data);

        player.removePotionEffect(PotionEffectType.SLOWNESS);
        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        // Don't force the local player's pose server-side (can cause bobbing/collision corrections).
        pose.setCrawlPose(player, false);
        player.setWalkSpeed(0.2f);
        player.setFlySpeed(0.1f);
        if (player.getHealth() > 0.0) {
            player.setHealth(Math.max(UNCONSCIOUS_HEALTH, Math.min(player.getHealth(), maxHealth(player))));
        }
        player.sendMessage(prefix + ChatColor.GREEN + "Vous reprenez conscience.");
    }

    public void respawnNow(Player player) {
        reviveToConscious(player);
        player.teleport(player.getWorld().getSpawnLocation());
        double max = maxHealth(player);
        player.setHealth(Math.min(max, max));
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.sendMessage(prefix + ChatColor.GREEN + "Respawn.");
    }

    @Override
    public void run() {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            PlayerData data = repo.get(p.getUniqueId());
            if (!data.unconscious()) continue;
            applyState(p);
            // Some clients/versions will drop forced pose; refresh it.
            pose.setCrawlPose(p, true);
        }
    }

    private void applyState(Player player) {
        if (player.getHealth() <= 0.0) return;
        player.setHealth(Math.min(maxHealth(player), UNCONSCIOUS_HEALTH));
        player.setFoodLevel(20);

        PotionEffect slow = new PotionEffect(PotionEffectType.SLOWNESS, 20 * 3, SLOWNESS_AMP, true, false, false);
        player.addPotionEffect(slow);
        // Pose is handled via ProtocolLib for other viewers (avoids local bobbing).
        player.setWalkSpeed(0.0f);
        player.setFlySpeed(0.0f);
    }

    private double maxHealth(Player player) {
        return player.getAttribute(Attribute.MAX_HEALTH) != null
                ? player.getAttribute(Attribute.MAX_HEALTH).getValue()
                : 20.0;
    }
}
