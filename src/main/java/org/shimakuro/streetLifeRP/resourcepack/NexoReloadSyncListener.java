package org.shimakuro.streetLifeRP.resourcepack;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.Locale;

public final class NexoReloadSyncListener implements Listener {
    private final ExternalResourcePackSyncService sync;

    public NexoReloadSyncListener(ExternalResourcePackSyncService sync) {
        this.sync = sync;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (isNexoReload(event.getMessage())) {
            sync.syncNow("nexo-player-command");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onServerCommand(ServerCommandEvent event) {
        if (isNexoReload(event.getCommand())) {
            sync.syncNow("nexo-server-command");
        }
    }

    private boolean isNexoReload(String raw) {
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
        if (!root.equals("nexo") && !root.equals("n")) return false;
        return parts[1].equals("reload");
    }
}
