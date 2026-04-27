package org.shimakuro.streetLifeRP.justice;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class JusticeListener implements Listener {
    private final StreetLifeRPContext ctx;

    public JusticeListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ctx.justice().applyStateOnJoin(event.getPlayer(), ctx.config().prefix());
    }
}

