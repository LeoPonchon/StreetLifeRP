package org.shimakuro.streetLifeRP.chat;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class ProximityChatListener implements Listener {
    private final StreetLifeRPContext ctx;

    public ProximityChatListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) return;
        if (!ctx.chat().proximityEnabled()) return;

        event.setCancelled(true);
        String message = event.getMessage();
        Bukkit.getScheduler().runTask(ctx.plugin(), () -> ctx.chat().sendLocalChat(event.getPlayer(), message));
    }
}

