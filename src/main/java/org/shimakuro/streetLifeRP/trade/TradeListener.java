package org.shimakuro.streetLifeRP.trade;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class TradeListener implements Listener {
    private final StreetLifeRPContext ctx;

    public TradeListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractPlayer(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player actor = event.getPlayer();
        if (!actor.isSneaking()) return;

        ItemStack inHand = actor.getInventory().getItemInMainHand();
        if (inHand != null && !inHand.getType().isAir()) return;

        event.setCancelled(true);

        if (actor.getWorld() != target.getWorld() || actor.getLocation().distanceSquared(target.getLocation()) > (4.0 * 4.0)) {
            actor.sendMessage(ctx.config().prefix() + ChatColor.RED + "Trop loin.");
            return;
        }

        ctx.trade().openTrade(actor, target, ctx.config().prefix());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof TradeService.TradeHolder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;

        TradeService.TradeSession session = ctx.trade().session(player.getUniqueId());
        if (session == null || session.inv() != top) return;

        int raw = event.getRawSlot();
        if (raw < 0 || raw >= top.getSize()) return;

        if (raw == TradeService.modeSlot()) {
            ctx.trade().cycleMode(player);
            ctx.trade().updateUi(session);
            return;
        }
        if (raw == TradeService.confirmSlotA() || raw == TradeService.confirmSlotB()) {
            ctx.trade().toggleConfirm(player, ctx.config().prefix());
            ctx.trade().updateUi(session);
            return;
        }

        boolean isA = player.getUniqueId().equals(session.a());
        boolean isOfferA = raw == TradeService.offerSlotA();
        boolean isOfferB = raw == TradeService.offerSlotB();
        if (!(isOfferA || isOfferB)) return;

        if ((isOfferA && !isA) || (isOfferB && isA)) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Impossible.");
            return;
        }

        boolean canEdit = ctx.trade().canEditOffer(session, player.getUniqueId(), isOfferA);
        if (!canEdit) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Offre verrouillée.");
            return;
        }

        ItemStack cursor = event.getCursor();
        ItemStack current = top.getItem(raw);

        // Allow trading only Cash items
        if (cursor != null && !cursor.getType().isAir()) {
            if (ctx.cashItems().read(cursor) == null) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Trade: uniquement des items Cash.");
                return;
            }
        }
        if (current != null && !current.getType().isAir()) {
            if (ctx.cashItems().read(current) == null) {
                top.setItem(raw, null);
                ctx.trade().onOfferChanged(session);
                return;
            }
        }

        top.setItem(raw, cursor == null || cursor.getType().isAir() ? null : cursor.clone());
        event.setCursor(current == null ? null : current.clone());
        ctx.trade().onOfferChanged(session);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        TradeService.TradeSession session = ctx.trade().session(player.getUniqueId());
        if (session == null) return;
        if (!event.getInventory().equals(session.inv())) return;
        ctx.trade().cancel(player, ctx.config().prefix(), "close");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ctx.trade().cancel(event.getPlayer(), ctx.config().prefix(), "quit");
    }
}

