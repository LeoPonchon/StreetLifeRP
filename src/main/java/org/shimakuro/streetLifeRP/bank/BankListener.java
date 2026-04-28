package org.shimakuro.streetLifeRP.bank;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class BankListener implements Listener {
    private final StreetLifeRPContext ctx;

    public BankListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;
        Player player = event.getPlayer();
        BankService.BankDef bank = ctx.bank().findBankNear(player);
        if (bank == null) return;

        event.setCancelled(true);
        ctx.bank().open(player, bank, ctx.config().prefix());
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (holder == null || !ctx.bank().isBankInventory(holder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        ctx.bank().handleClick(player, clicked, ctx.config().prefix());
    }
}

