package org.shimakuro.streetLifeRP.input;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class InputListener implements Listener {
    private final StreetLifeRPContext ctx;

    public InputListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (!ctx.input().has(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
        ctx.input().handleChat(event.getPlayer(), event.getMessage());
    }
}
