package org.shimakuro.streetLifeRP.items;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;
import org.shimakuro.streetLifeRP.jobs.JobType;

public final class SpecialItemListener implements Listener {
    private final StreetLifeRPContext ctx;

    public SpecialItemListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!(event.getRightClicked() instanceof Player target)) return;

        Player actor = event.getPlayer();
        ItemStack inHand = actor.getInventory().getItemInMainHand();
        SpecialItemType type = ctx.items().read(inHand);
        if (type == null) return;

        event.setCancelled(true);

        if (actor.getWorld() != target.getWorld() || actor.getLocation().distanceSquared(target.getLocation()) > (4.0 * 4.0)) {
            actor.sendMessage(ctx.config().prefix() + ChatColor.RED + "Trop loin.");
            return;
        }

        switch (type) {
            case HANDCUFFS -> {
                if (ctx.jobs().get(actor.getUniqueId()) != JobType.POLICE) {
                    actor.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                    return;
                }
                boolean next = !ctx.justice().isCuffed(target.getUniqueId());
                ctx.justice().setCuffed(target, next, ctx.config().prefix(), actor.getName());
            }
            case MEDKIT -> {
                if (ctx.jobs().get(actor.getUniqueId()) != JobType.EMS) {
                    actor.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                    return;
                }
                ctx.ems().heal(target, ctx.config().prefix());
            }
            case DEFIB -> {
                if (ctx.jobs().get(actor.getUniqueId()) != JobType.EMS) {
                    actor.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                    return;
                }
                ctx.ems().revive(target, ctx.config().prefix());
            }
            default -> {
                // no-op
            }
        }
    }
}
