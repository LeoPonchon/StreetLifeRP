package org.shimakuro.streetLifeRP.chat;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

import java.util.Set;

public final class ChatCommandBlockListener implements Listener {
    private static final Set<String> BLOCKED = Set.of(
            // Vanilla
            "me",
            "msg", "tell", "w", "whisper",
            "r", "reply",
            "say",
            "teammsg", "tm",
            "tellraw",
            "title",
            // Vehicles (QAV2) - block all player commands
            "qav",
            "qav2",
            "qualityarmoryvehicles",
            "qualityarmoryvehicles2",
            // Common aliases (plugins)
            "m", "t",
            "pm", "dm",
            "bc", "broadcast"
    );

    private final StreetLifeRPContext ctx;

    public ChatCommandBlockListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        String raw = event.getMessage();
        if (raw == null || raw.isBlank() || raw.charAt(0) != '/') return;

        String cmd = raw.substring(1).trim();
        if (cmd.isEmpty()) return;

        int space = cmd.indexOf(' ');
        String label = (space == -1 ? cmd : cmd.substring(0, space)).toLowerCase();

        // Allow SMS commands explicitly (only comms allowed besides local chat).
        if (label.equals("sms") || label.endsWith(":sms")) return;

        String base = label;
        String namespace = null;
        int colon = base.indexOf(':');
        if (colon != -1) {
            namespace = colon > 0 ? base.substring(0, colon) : null;
            if (colon + 1 < base.length()) {
                base = base.substring(colon + 1);
            }
        }

        if (!BLOCKED.contains(base) && (namespace == null || !BLOCKED.contains(namespace))) return;

        event.setCancelled(true);
        event.getPlayer().sendMessage(ctx.config().prefix() + ChatColor.RED + "Commande désactivée: utilisez les SMS ou le chat local.");
    }
}
