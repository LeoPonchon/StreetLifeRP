package org.shimakuro.streetLifeRP.items;

import org.bukkit.ChatColor;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.event.inventory.InventoryType;
import org.shimakuro.streetLifeRP.billing.BillingService;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class KeyItemGuardListener implements Listener {
    private final StreetLifeRPContext ctx;

    public KeyItemGuardListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!isKeyItem(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(ctx.config().prefix() + ChatColor.RED + "Impossible de jeter un objet clé.");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        boolean currentKey = isKeyItem(current);
        boolean cursorKey = isKeyItem(cursor);
        if (!currentKey && !cursorKey) return;

        Inventory top = event.getView().getTopInventory();
        Inventory bottom = event.getView().getBottomInventory();
        int topSize = top.getSize();
        int raw = event.getRawSlot();

        // Block shift-click moving key items to other inventory.
        if (event.isShiftClick() && currentKey && event.getClickedInventory() instanceof PlayerInventory) {
            // Only relevant when a container is open.
            if (top != null && top.getType() != InventoryType.CRAFTING && bottom instanceof PlayerInventory) {
                event.setCancelled(true);
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Impossible de ranger un objet clé.");
            }
            return;
        }

        // Block placing key items into top inventory (containers, crafting table, etc).
        if (cursorKey && raw >= 0 && raw < topSize) {
            event.setCancelled(true);
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Impossible de ranger un objet clé.");
            return;
        }

        // Block taking key items from player inventory into top inventory via hotbar swap.
        if (event.getHotbarButton() != -1 && raw >= 0 && raw < topSize) {
            ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
            if (isKeyItem(hotbar)) {
                event.setCancelled(true);
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Impossible de ranger un objet clé.");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInvDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isKeyItem(event.getOldCursor())) return;

        int topSize = event.getView().getTopInventory().getSize();
        for (Integer raw : event.getRawSlots()) {
            if (raw != null && raw >= 0 && raw < topSize) {
                event.setCancelled(true);
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Impossible de ranger un objet clé.");
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlaceInFrame(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof ItemFrame)) return;
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();
        if (!isKeyItem(inHand)) return;
        event.setCancelled(true);
        player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Impossible de poser un objet clé.");
    }

    private boolean isKeyItem(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        if (ctx.phoneItems().isPhone(item)) return true;
        if (ctx.items().read(item) != null) return true;
        BillingService.BillingData billing = ctx.billing().read(item);
        return billing != null && ("tool".equalsIgnoreCase(billing.type()) || "invoice".equalsIgnoreCase(billing.type()));
    }
}
