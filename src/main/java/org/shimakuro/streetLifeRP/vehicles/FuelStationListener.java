package org.shimakuro.streetLifeRP.vehicles;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class FuelStationListener implements Listener {
    private final StreetLifeRPContext ctx;

    public FuelStationListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();

        if (!isNearFuelStation(player.getLocation())) return;

        Object vehicle = QavVehicleReflection.vehicleForPlayer(player);
        if (vehicle == null) return;

        int maxFuel = maxFuel();
        if (maxFuel <= 0) return;

        int current = QavVehicleReflection.getFuel(vehicle);
        if (current >= maxFuel) {
            player.sendMessage(ctx.config().prefix() + ChatColor.DARK_GRAY + "Réservoir déjà plein.");
            event.setCancelled(true);
            return;
        }

        int missing = maxFuel - Math.max(0, current);
        double pricePer = pricePerUnit();
        double cost = missing * pricePer;

        if (!ctx.economy().spendCash(player.getUniqueId(), cost, "fuel:" + missing)) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Fonds insuffisants (" + ctx.economy().format(cost) + ").");
            event.setCancelled(true);
            return;
        }

        QavVehicleReflection.setFuel(vehicle, maxFuel);
        player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Plein effectué: -" + ctx.economy().format(cost));
        event.setCancelled(true);
    }

    private int maxFuel() {
        return ctx.config().raw().getInt("vehicles.fuel.max", 10000);
    }

    private double pricePerUnit() {
        return ctx.config().raw().getDouble("vehicles.fuel.price_per_unit", 6.0);
    }

    private boolean isNearFuelStation(Location loc) {
        ConfigurationSection section = ctx.config().raw().getConfigurationSection("vehicles.fuel_stations.list");
        if (section == null) return false;
        String world = (loc.getWorld() != null) ? loc.getWorld().getName() : "world";

        for (String key : section.getKeys(false)) {
            ConfigurationSection s = section.getConfigurationSection(key);
            if (s == null) continue;
            ConfigurationSection t = s.getConfigurationSection("terminal");
            if (t == null) continue;
            String w = t.getString("world", "world");
            if (!world.equalsIgnoreCase(w)) continue;
            double x = t.getDouble("x", 0.0);
            double y = t.getDouble("y", 64.0);
            double z = t.getDouble("z", 0.0);
            double radius = t.getDouble("radius", 4.0);

            double dx = Math.abs(loc.getX() - x);
            double dy = Math.abs(loc.getY() - y);
            double dz = Math.abs(loc.getZ() - z);
            if (dx <= radius && dz <= radius && dy <= radius) return true;
        }
        return false;
    }
}

