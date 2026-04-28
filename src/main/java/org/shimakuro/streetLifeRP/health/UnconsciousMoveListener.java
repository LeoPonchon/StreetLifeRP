package org.shimakuro.streetLifeRP.health;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class UnconsciousMoveListener implements Listener {
    private final StreetLifeRPContext ctx;

    public UnconsciousMoveListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!ctx.unconscious().isUnconscious(player)) return;

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        // Allow view rotation, block any positional movement (including jumping).
        if (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ()) return;
        event.setTo(from);
    }
}

