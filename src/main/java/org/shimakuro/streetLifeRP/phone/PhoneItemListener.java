package org.shimakuro.streetLifeRP.phone;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class PhoneItemListener implements Listener {
    private final StreetLifeRPContext ctx;

    public PhoneItemListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.PHYSICAL) return;
        ItemStack item = event.getItem();
        if (!ctx.phoneItems().isPhone(item)) return;
        if (ctx.justice().isCuffed(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
        ctx.phoneMenu().open(event.getPlayer(), ctx.config().prefix());
    }
}
