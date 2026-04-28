package org.shimakuro.streetLifeRP.shops;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.entity.Player;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class ShopListener implements Listener {
    private final StreetLifeRPContext ctx;

    public ShopListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null || !ctx.shop().isShopInventory(holder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        if (ctx.shop().isBackButton(clicked)) {
            ctx.phoneMenu().open(player, ctx.config().prefix());
            return;
        }
        ctx.shop().tryBuy(player, clicked, ctx.config().prefix());
    }
}
