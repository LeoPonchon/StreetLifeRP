package org.shimakuro.streetLifeRP.resourcepack;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;

public final class OraxenReloadSyncListener implements Listener {
    private final ExternalResourcePackSyncService sync;

    public OraxenReloadSyncListener(ExternalResourcePackSyncService sync) {
        this.sync = sync;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isOraxenReload(event.getMessage())) {
            sync.syncNow("oraxen-player-command");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        if (isOraxenReload(event.getCommand())) {
            sync.syncNow("oraxen-server-command");
        }
    }

    private boolean isOraxenReload(String raw) {
        if (raw == null) return false;
        String command = raw.trim();
        if (command.startsWith("/")) command = command.substring(1).trim();
        if (command.isBlank()) return false;

        String[] parts = command.toLowerCase(Locale.ROOT).split("\\s+");
        if (parts.length < 2) return false;
        String root = parts[0];
        int namespaceIndex = root.indexOf(':');
        if (namespaceIndex >= 0 && namespaceIndex + 1 < root.length()) {
            root = root.substring(namespaceIndex + 1);
        }
        if (!root.equals("oraxen") && !root.equals("o")) return false;
        return parts[1].equals("reload");
    }
}
