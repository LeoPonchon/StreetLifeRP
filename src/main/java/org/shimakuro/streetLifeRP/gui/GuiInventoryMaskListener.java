package org.shimakuro.streetLifeRP.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.InventoryHolder;

public final class GuiInventoryMaskListener implements Listener {
    private final GuiInventoryMaskService mask;

    public GuiInventoryMaskListener(GuiInventoryMaskService mask) {
        this.mask = mask;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!isStreetLifeHolder(holder)) return;
        mask.startMask(player, event.getView().getTopInventory().getSize());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        mask.stopMask(player);
    }

    private boolean isStreetLifeHolder(InventoryHolder holder) {
        if (holder == null) return false;
        String name = holder.getClass().getName();
        return name != null && name.startsWith("org.shimakuro.streetLifeRP");
    }
}

