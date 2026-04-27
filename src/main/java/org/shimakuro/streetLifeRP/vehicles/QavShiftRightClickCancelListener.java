package org.shimakuro.streetLifeRP.vehicles;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Disable QAV2 default interaction when player is sneaking and right-clicks a vehicle entity.
 */
public final class QavShiftRightClickCancelListener implements Listener {
    private final StreetLifeRPContext ctx;
    private final Map<UUID, Long> blockedOverviewUntil = new HashMap<>();

    public QavShiftRightClickCancelListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteractEntityLowest(PlayerInteractEntityEvent event) {
        handle(event.getPlayer(), event.getHand(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        handle(event.getPlayer(), event.getHand(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        handle(event.getPlayer(), event.getHand(), event.getRightClicked(), event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (ctx.justice().isCuffed(player.getUniqueId())) return;
        if (ctx.phoneItems().isPhone(player.getInventory().getItemInMainHand())) return;

        if (!isQavVehicleEntity(event.getRightClicked())) return;
        event.setCancelled(true);
    }

    private void handle(Player player, EquipmentSlot hand, Entity clicked, Cancellable cancellable) {
        if (hand != EquipmentSlot.HAND) return;
        if (!player.isSneaking()) return;
        if (ctx.justice().isCuffed(player.getUniqueId())) return;
        if (ctx.phoneItems().isPhone(player.getInventory().getItemInMainHand())) return;

        if (!isQavVehicleEntity(clicked)) return;
        cancellable.setCancelled(true);
        blockedOverviewUntil.put(player.getUniqueId(), System.currentTimeMillis() + 1500L);
    }

    private boolean isQavVehicleEntity(Entity entity) {
        try {
            Class<?> api = Class.forName("me.zombie_striker.qav.api.QualityArmoryVehicles");
            try {
                Method isQav = api.getMethod("isQAVEntity", Entity.class);
                Object out = isQav.invoke(null, entity);
                if (out instanceof Boolean b && b) return true;
            } catch (Throwable ignored) {
                // fallback below
            }
            try {
                Method isVehicle = api.getMethod("isVehicle", Entity.class);
                Object out = isVehicle.invoke(null, entity);
                if (out instanceof Boolean b && b) return true;
            } catch (Throwable ignored) {
                // fallback below
            }
            Method m = api.getMethod("getVehicleEntityByEntity", Entity.class);
            Object vehicle = m.invoke(null, entity);
            return vehicle != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (!shouldBlockOverview(player)) return;
        String title = event.getView().getTitle();
        if (title == null) return;
        if (!isOverviewTitle(title)) return;

        event.setCancelled(true);
        player.closeInventory();
    }

    private boolean shouldBlockOverview(Player player) {
        Long until = blockedOverviewUntil.get(player.getUniqueId());
        if (until != null) {
            if (System.currentTimeMillis() <= until) return true;
            blockedOverviewUntil.remove(player.getUniqueId());
        }
        return player.isSneaking();
    }

    private boolean isOverviewTitle(String title) {
        String plain = org.bukkit.ChatColor.stripColor(title);
        if (plain == null) plain = title;
        String normalized = plain.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.endsWith(": overview") || normalized.equals("overview") || normalized.contains("overview");
    }
}
