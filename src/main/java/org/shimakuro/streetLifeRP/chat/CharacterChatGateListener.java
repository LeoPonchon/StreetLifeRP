package org.shimakuro.streetLifeRP.chat;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

public final class CharacterChatGateListener implements Listener {
    private final StreetLifeRPContext ctx;

    public CharacterChatGateListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        if (ctx.input().has(event.getPlayer().getUniqueId())) return;
        if (ctx.characters().data(event.getPlayer().getUniqueId()).hasCharacter()) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage d'abord.");
    }
}
