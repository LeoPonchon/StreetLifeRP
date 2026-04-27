package org.shimakuro.streetLifeRP.data;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class PlayerDataListener implements Listener {
    private final StreetLifeRPContext ctx;

    public PlayerDataListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ctx.playerData().get(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        ctx.playerData().unload(event.getPlayer().getUniqueId());
    }
}

