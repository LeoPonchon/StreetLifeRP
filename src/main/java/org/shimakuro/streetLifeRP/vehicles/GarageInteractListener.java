package org.shimakuro.streetLifeRP.vehicles;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class GarageInteractListener implements Listener {
    private final StreetLifeRPContext ctx;

    public GarageInteractListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK && action != Action.RIGHT_CLICK_AIR) return;

        // Garage open = main vide
        ItemStack item = event.getItem();
        if (item != null && !item.getType().isAir()) return;

        Player player = event.getPlayer();
        GarageService.Garage garage = ctx.garage().findGarageNearTerminal(player);
        if (garage == null) return;

        event.setCancelled(true);
        ctx.garage().openGarageMenu(player, garage, ctx.config().prefix());
    }
}
