package org.shimakuro.streetLifeRP.phone;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

import java.util.Locale;
import java.util.Map;

public final class PhoneSlotGuardListener implements Listener {
    private final StreetLifeRPContext ctx;

    public PhoneSlotGuardListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> ensurePhone(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent event) {
        ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> ensurePhone(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onClearCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.isBlank() || raw.charAt(0) != '/') return;

        String cmd = raw.substring(1).trim();
        if (cmd.isEmpty()) return;

        int space = cmd.indexOf(' ');
        String label = (space == -1 ? cmd : cmd.substring(0, space)).toLowerCase(Locale.ROOT);
        if (!label.equals("clear") && !label.equals("minecraft:clear") && !label.equals("essentials:clear")) return;

        // Let the clear command run, then restore the phone if it was wiped.
        ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> ensurePhone(event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        if (!ctx.phoneItems().isPhone(event.getItemDrop().getItemStack())) return;
        event.setCancelled(true);
        ensurePhone(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        if (ctx.phoneItems().isPhone(event.getMainHandItem()) || ctx.phoneItems().isPhone(event.getOffHandItem())) {
            event.setCancelled(true);
            ensurePhone(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        // Any click involving the phone item should be blocked.
        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();
        if (ctx.phoneItems().isPhone(current) || ctx.phoneItems().isPhone(cursor)) {
            event.setCancelled(true);
            ensurePhone(player);
            return;
        }

        // Block putting items into slot 9 (hotbar slot 8).
        if (event.getClickedInventory() instanceof PlayerInventory && event.getSlot() == PhoneItemService.HOTBAR_SLOT) {
            event.setCancelled(true);
            ensurePhone(player);
            return;
        }

        // Block number-key swap into slot 9.
        if (event.getHotbarButton() == PhoneItemService.HOTBAR_SLOT) {
            event.setCancelled(true);
            ensurePhone(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        int topSize = event.getView().getTopInventory().getSize();
        int phoneRaw = topSize + PhoneItemService.HOTBAR_SLOT;
        if (event.getRawSlots().contains(phoneRaw) || ctx.phoneItems().isPhone(event.getOldCursor())) {
            event.setCancelled(true);
            ensurePhone(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        ctx.plugin().getServer().getScheduler().runTask(ctx.plugin(), () -> ensurePhone(player));
    }

    private void ensurePhone(Player player) {
        PlayerInventory inv = player.getInventory();

        // Remove duplicates elsewhere (keep only slot 9).
        for (int i = 0; i < inv.getSize(); i++) {
            if (i == PhoneItemService.HOTBAR_SLOT) continue;
            ItemStack it = inv.getItem(i);
            if (ctx.phoneItems().isPhone(it)) {
                inv.setItem(i, null);
            }
        }

        ItemStack inSlot = inv.getItem(PhoneItemService.HOTBAR_SLOT);
        if (ctx.phoneItems().isPhone(inSlot)) return;

        // Move existing item away to avoid deletion.
        if (inSlot != null && !inSlot.getType().isAir()) {
            inv.setItem(PhoneItemService.HOTBAR_SLOT, null);
            Map<Integer, ItemStack> leftover = inv.addItem(inSlot);
            for (ItemStack it : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), it);
            }
        }

        inv.setItem(PhoneItemService.HOTBAR_SLOT, ctx.phoneItems().create());
    }
}
