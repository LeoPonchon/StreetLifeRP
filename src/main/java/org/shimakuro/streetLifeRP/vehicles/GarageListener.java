package org.shimakuro.streetLifeRP.vehicles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class GarageListener implements Listener {
    private final StreetLifeRPContext ctx;

    public GarageListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) return;
        if (ctx.phoneItems().isPhone(event.getItem())) return;
        if (event.getPlayer().getInventory().getItemInMainHand() != null
                && !event.getPlayer().getInventory().getItemInMainHand().getType().isAir()) return;
        if (ctx.justice().isCuffed(event.getPlayer().getUniqueId())) return;
        if (!ctx.characters().data(event.getPlayer().getUniqueId()).hasCharacter()) return;

        GarageService.Garage garage = ctx.garage().findGarageNearTerminal(event.getPlayer());
        if (garage == null) return;

        event.setCancelled(true);
        ctx.garage().openGarageMenu(event.getPlayer(), garage, ctx.config().prefix());
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
