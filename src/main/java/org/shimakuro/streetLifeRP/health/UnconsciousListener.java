package org.shimakuro.streetLifeRP.health;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class UnconsciousListener implements Listener {
    private final StreetLifeRPContext ctx;

    public UnconsciousListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (ctx.unconscious().isUnconscious(player)) {
            event.setCancelled(true);
            return;
        }

        double finalHealth = player.getHealth() - event.getFinalDamage();
        if (finalHealth > 0.0) return;

        event.setCancelled(true);
        ctx.unconscious().knockOut(player);
    }
}

