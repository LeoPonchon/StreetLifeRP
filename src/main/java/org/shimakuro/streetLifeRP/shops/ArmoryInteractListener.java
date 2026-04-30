package org.shimakuro.streetLifeRP.shops;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class ArmoryInteractListener implements Listener {
    private final StreetLifeRPContext ctx;

    public ArmoryInteractListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

        // Armurerie open = main vide
        ItemStack item = event.getItem();
        if (item != null && !item.getType().isAir()) return;

        Player player = event.getPlayer();
        if (!isNearArmory(player)) return;

        ConfigurationSection armorySection = ctx.config().shopsRaw().getConfigurationSection("armory_shop");
        if (armorySection == null) return;

        event.setCancelled(true);
        ctx.shop().open(player, armorySection, ctx.config().prefix());
    }

    private boolean isNearArmory(Player player) {
        ConfigurationSection section = ctx.config().armoriesRaw().getConfigurationSection("armories");
        if (section == null) return false;
        ConfigurationSection all = section.getConfigurationSection("list");
        if (all == null) return false;

        org.bukkit.Location loc = player.getLocation();
        String world = (loc.getWorld() != null) ? loc.getWorld().getName() : "world";

        for (String key : all.getKeys(false)) {
            ConfigurationSection a = all.getConfigurationSection(key);
            if (a == null) continue;
            ConfigurationSection t = a.getConfigurationSection("terminal");
            if (t == null) continue;
            String w = t.getString("world");
            if (w == null || w.isBlank()) continue;
            if (!world.equalsIgnoreCase(w)) continue;

            if (!t.contains("x1") || !t.contains("y1") || !t.contains("z1")
                    || !t.contains("x2") || !t.contains("y2") || !t.contains("z2")) {
                continue;
            }
            double x1 = t.getDouble("x1");
            double y1 = t.getDouble("y1");
            double z1 = t.getDouble("z1");
            double x2 = t.getDouble("x2");
            double y2 = t.getDouble("y2");
            double z2 = t.getDouble("z2");
            double minX = Math.min(x1, x2);
            double minY = Math.min(y1, y2);
            double minZ = Math.min(z1, z2);
            double maxX = Math.max(x1, x2);
            double maxY = Math.max(y1, y2);
            double maxZ = Math.max(z1, z2);
            if (loc.getX() >= minX && loc.getX() <= maxX
                    && loc.getY() >= minY && loc.getY() <= maxY
                    && loc.getZ() >= minZ && loc.getZ() <= maxZ) return true;
        }
        return false;
    }
}
