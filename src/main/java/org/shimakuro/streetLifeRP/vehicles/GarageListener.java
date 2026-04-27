package org.shimakuro.streetLifeRP.vehicles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class GarageListener implements Listener {
    private final StreetLifeRPContext ctx;

    public GarageListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!ctx.garage().isGarageInventory(holder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        if (holder instanceof GarageService.DealerHolder) {
            ctx.garage().tryBuyFromMenu(player, clicked, ctx.config().prefix());
            return;
        }
        ctx.garage().handleGarageMenuClick(player, clicked, ctx.config().prefix());
    }
}
