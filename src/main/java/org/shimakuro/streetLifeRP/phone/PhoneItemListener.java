package org.shimakuro.streetLifeRP.phone;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class PhoneItemListener implements Listener {
    private final StreetLifeRPContext ctx;

    public PhoneItemListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        ItemStack inHand = event.getPlayer().getInventory().getItemInMainHand();
        if (!ctx.phoneItems().isPhone(inHand)) return;

        event.setCancelled(true);
        ctx.phoneMenu().open(event.getPlayer(), ctx.config().prefix());
    }
}

