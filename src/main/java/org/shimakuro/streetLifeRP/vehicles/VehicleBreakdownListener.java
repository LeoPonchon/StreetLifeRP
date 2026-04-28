package org.shimakuro.streetLifeRP.vehicles;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;
import org.shimakuro.streetLifeRP.jobs.JobType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VehicleBreakdownListener implements Listener {
    private final StreetLifeRPContext ctx;
    private final Map<UUID, Integer> impacts = new HashMap<>();
    private final Map<UUID, Boolean> broken = new HashMap<>();
    private final org.bukkit.NamespacedKey customItemKey;

    public VehicleBreakdownListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
        JavaPlugin plugin = (JavaPlugin) ctx.plugin();
        this.customItemKey = new org.bukkit.NamespacedKey(plugin, "custom_item");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onVehicleDamage(EntityDamageEvent event) {
        Entity entity = event.getEntity();
        if (!QavVehicleReflection.isVehicleEntity(entity)) return;

        Object v = QavVehicleReflection.vehicleEntityByEntity(entity);
        if (v == null) return;
        UUID uuid = QavVehicleReflection.vehicleUuid(v);
        if (uuid == null) return;

        int threshold = breakdownImpactsThreshold();
        if (threshold <= 0) return;

        int next = impacts.getOrDefault(uuid, 0) + 1;
        impacts.put(uuid, next);

        if (next < threshold) return;

        broken.put(uuid, true);
        QavVehicleReflection.setFuel(v, 0);
        ctx.plugin().getLogger().fine("Vehicle broken uuid=" + uuid + " impacts=" + next);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractVehicle(PlayerInteractEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractVehicleAt(PlayerInteractAtEntityEvent event) {
        handleInteract(event.getPlayer(), event.getRightClicked(), event);
    }

    private void handleInteract(Player player, Entity clicked, org.bukkit.event.Cancellable cancellable) {
        Object v = QavVehicleReflection.vehicleEntityByEntity(clicked);
        if (v == null) return;
        UUID uuid = QavVehicleReflection.vehicleUuid(v);
        if (uuid == null) return;

        if (isRepairKit(player.getInventory().getItemInMainHand())) {
            if (ctx.jobs().get(player.getUniqueId()) != JobType.MECHANIC) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Métier requis: MECHANIC");
                cancellable.setCancelled(true);
                return;
            }
            broken.remove(uuid);
            impacts.remove(uuid);
            double max = QavVehicleReflection.maxHealth(v);
            if (max > 0.0) QavVehicleReflection.setHealth(v, max);
            player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Véhicule réparé.");
            cancellable.setCancelled(true);
            return;
        }

        if (Boolean.TRUE.equals(broken.get(uuid))) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Véhicule en panne: appelle un mécano.");
            cancellable.setCancelled(true);
        }
    }

    private boolean isRepairKit(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        String id = meta.getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
        return "repair_kit".equals(id);
    }

    private int breakdownImpactsThreshold() {
        ConfigurationSection section = ctx.config().raw().getConfigurationSection("vehicles.mechanic");
        if (section == null) return 0;
        return section.getInt("breakdown_impacts", 0);
    }
}

