package org.shimakuro.streetLifeRP.justice;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;
import org.shimakuro.streetLifeRP.jobs.JobType;

import java.util.List;
import java.util.UUID;

public final class PoliceInteractListener implements Listener {
    private final StreetLifeRPContext ctx;
    private final NamespacedKey fineAmountKey;

    public PoliceInteractListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
        this.fineAmountKey = new NamespacedKey(ctx.plugin(), "fine_amount");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClickPlayer(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player actor = event.getPlayer();
        if (actor.isSneaking()) return; // reserved for trade
        if (ctx.jobs().get(actor.getUniqueId()) != JobType.POLICE) return;

        var inHand = actor.getInventory().getItemInMainHand();
        if (inHand != null && !inHand.getType().isAir()) return;

        if (!ctx.characters().data(actor.getUniqueId()).hasCharacter()) return;
        if (!ctx.characters().data(target.getUniqueId()).hasCharacter()) {
            actor.sendMessage(ctx.config().prefix() + ChatColor.RED + "Ce joueur n'a pas de personnage.");
            return;
        }

        event.setCancelled(true);
        openFineMenu(actor, target);
    }

    private void openFineMenu(Player actor, Player target) {
        String rpName = ctx.characters().rpNameOrNull(target.getUniqueId());
        if (rpName == null) {
            actor.sendMessage(ctx.config().prefix() + ChatColor.RED + "Ce joueur n'a pas de personnage.");
            return;
        }
        Inventory inv = ctx.plugin().getServer().createInventory(new FineHolder(target.getUniqueId()), 27,
                ChatColor.RED + "Amende: " + ChatColor.WHITE + rpName);

        inv.setItem(11, amountItem(50));
        inv.setItem(12, amountItem(100));
        inv.setItem(13, amountItem(200));
        inv.setItem(14, amountItem(500));
        inv.setItem(15, amountItem(1000));
        inv.setItem(22, customItem());

        actor.openInventory(inv);
    }

    private ItemStack amountItem(double amount) {
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Amende: " + ChatColor.GOLD + ctx.economy().format(amount));
            meta.setLore(List.of(ChatColor.GRAY + "Cliquer pour appliquer"));
            meta.getPersistentDataContainer().set(fineAmountKey, PersistentDataType.DOUBLE, amount);
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack customItem() {
        ItemStack it = new ItemStack(Material.WRITABLE_BOOK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Montant perso");
            meta.setLore(List.of(ChatColor.GRAY + "Saisie via chat"));
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFineMenuClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof FineHolder fineHolder)) return;

        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player actor)) return;

        Player target = ctx.plugin().getServer().getPlayer(fineHolder.target());
        if (target == null) {
            actor.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur hors ligne.");
            actor.closeInventory();
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType().isAir()) return;

        if (clicked.getType() == Material.WRITABLE_BOOK) {
            actor.closeInventory();
            ctx.input().request(actor, ctx.config().prefix() + ChatColor.AQUA + "Montant ? (chat)", (p, raw) -> {
                Double amount;
                try {
                    amount = Double.parseDouble(raw.replace(',', '.'));
                } catch (NumberFormatException e) {
                    p.sendMessage(ctx.config().prefix() + ChatColor.RED + "Montant invalide.");
                    return;
                }
                if (amount <= 0) {
                    p.sendMessage(ctx.config().prefix() + ChatColor.RED + "Montant invalide.");
                    return;
                }
                applyFine(p, target, amount, "Amende");
            });
            return;
        }

        if (clicked.getType() != Material.PAPER) return;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        Double amount = meta.getPersistentDataContainer().get(fineAmountKey, PersistentDataType.DOUBLE);
        if (amount == null || amount <= 0) return;

        actor.closeInventory();
        applyFine(actor, target, amount, "Amende");
    }

    private void applyFine(Player actor, Player target, double amount, String reason) {
        String actorRp = ctx.characters().rpNameOrNull(actor.getUniqueId());
        if (actorRp == null) {
            actor.sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage d'abord.");
            return;
        }
        ctx.justice().issueFine(target.getUniqueId(), actorRp, amount, reason);
        actor.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Amende mise.");
        target.sendMessage(ctx.config().prefix() + ChatColor.RED + "Vous avez reçu une amende: " + ctx.economy().format(amount) + " (" + reason + ").");
    }

    private record FineHolder(UUID target) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
