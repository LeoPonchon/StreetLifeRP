package org.shimakuro.streetLifeRP.economy;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseAction;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class CashItemListener implements Listener {
    private final StreetLifeRPContext ctx;

    public CashItemListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Double value = ctx.cashItems().read(item);
        if (value == null || value <= 0) return;

        event.setCancelled(true);
        if (!ctx.antiAbuse().allowAndMark(player.getUniqueId(), AntiAbuseAction.CASH_REDEEM)) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Action trop rapide.");
            return;
        }

        item.setAmount(item.getAmount() - 1);
        ctx.economy().addCash(player.getUniqueId(), value, "cash_redeem");
        player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Encaissement: +" + ctx.economy().format(value));
    }
}

