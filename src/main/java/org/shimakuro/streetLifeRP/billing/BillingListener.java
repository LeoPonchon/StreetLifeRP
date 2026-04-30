package org.shimakuro.streetLifeRP.billing;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

import java.util.Locale;
import java.util.UUID;

public final class BillingListener implements Listener {
    private final StreetLifeRPContext ctx;

    public BillingListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        BillingService.BillingData data = ctx.billing().read(stack);
        if (data == null || !"invoice".equalsIgnoreCase(data.type())) return;
        event.setCancelled(true);
        event.getPlayer().sendMessage(ctx.config().prefix() + ChatColor.RED + msg("billing.messages.cant_drop"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player actor = event.getPlayer();
        ItemStack inHand = actor.getInventory().getItemInMainHand();
        BillingService.BillingData tool = ctx.billing().read(inHand);
        if (tool == null || !"tool".equalsIgnoreCase(tool.type())) return;

        event.setCancelled(true);

        if (!ctx.characters().data(actor.getUniqueId()).hasCharacter()) {
            actor.sendMessage(ctx.config().prefix() + ChatColor.RED + msg("billing.messages.need_character"));
            return;
        }
        if (!ctx.characters().data(target.getUniqueId()).hasCharacter()) {
            actor.sendMessage(ctx.config().prefix() + ChatColor.RED + msg("billing.messages.target_no_character"));
            return;
        }
        if (actor.getWorld() != target.getWorld() || actor.getLocation().distanceSquared(target.getLocation()) > (4.0 * 4.0)) {
            actor.sendMessage(ctx.config().prefix() + ChatColor.RED + msg("billing.messages.too_far"));
            return;
        }

        String prompt = ctx.config().prefix() + ChatColor.YELLOW + msg("billing.messages.prompt_amount");
        ctx.input().request(actor, prompt, (p, raw) -> {
            double amount = parseAmount(raw);
            if (amount <= 0.0) {
                p.sendMessage(ctx.config().prefix() + ChatColor.RED + msg("billing.messages.invalid_amount"));
                return;
            }

            double max = ctx.config().billingRaw().getDouble("billing.invoice.max_amount", 0.0);
            if (max > 0.0 && amount > max) {
                p.sendMessage(ctx.config().prefix() + ChatColor.RED + msg("billing.messages.amount_too_high", ctx.economy().format(max)));
                return;
            }

            UUID issuer = p.getUniqueId();
            var job = ctx.jobs().get(issuer);
            ItemStack invoice = ctx.billing().createInvoice(issuer, job, amount);
            target.getInventory().addItem(invoice);
            p.sendMessage(ctx.config().prefix() + ChatColor.GREEN + msg("billing.messages.sent"));
            target.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + msg("billing.messages.received"));
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Player player = event.getPlayer();
        ItemStack inHand = player.getInventory().getItemInMainHand();

        BillingService.BillingData data = ctx.billing().read(inHand);
        if (data == null || !"invoice".equalsIgnoreCase(data.type())) return;

        event.setCancelled(true);

        if (!ctx.characters().data(player.getUniqueId()).hasCharacter()) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + msg("billing.messages.need_character"));
            return;
        }
        if (data.issuer() == null) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + msg("billing.messages.invalid_invoice"));
            return;
        }
        double amount = data.amount();
        if (amount <= 0.0) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + msg("billing.messages.invalid_invoice"));
            return;
        }

        if (!ctx.economy().spendCash(player.getUniqueId(), amount, "invoice_pay")) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + msg("billing.messages.insufficient_funds"));
            return;
        }

        ctx.economy().addCash(data.issuer(), amount, "invoice_receive");
        consumeOne(player);

        player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + msg("billing.messages.paid"));
        Player issuerOnline = ctx.plugin().getServer().getPlayer(data.issuer());
        if (issuerOnline != null) {
            issuerOnline.sendMessage(ctx.config().prefix() + ChatColor.GREEN + msg("billing.messages.got_paid"));
        }
    }

    private void consumeOne(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null) return;
        int amt = item.getAmount();
        if (amt <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            item.setAmount(amt - 1);
            player.getInventory().setItemInMainHand(item);
        }
    }

    private String msg(String path) {
        String raw = ctx.config().billingRaw().getString(path);
        if (raw == null) return "";
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    private String msg(String path, String maxFormatted) {
        String raw = ctx.config().billingRaw().getString(path);
        if (raw == null) return "";
        raw = raw.replace("%max%", maxFormatted != null ? maxFormatted : "");
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    private double parseAmount(String raw) {
        if (raw == null) return 0.0;
        String s = raw.trim().toLowerCase(Locale.ROOT).replace(',', '.');
        if (s.isBlank()) return 0.0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
