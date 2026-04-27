package org.shimakuro.streetLifeRP.input;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class InputService {
    private final JavaPlugin plugin;
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

    public InputService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void request(Player player, String prompt, BiConsumer<Player, String> onInput) {
        player.sendMessage(prompt);
        pending.put(player.getUniqueId(), new PendingInput(onInput));
    }

    public boolean has(UUID uuid) {
        return pending.containsKey(uuid);
    }

    public void cancel(UUID uuid) {
        pending.remove(uuid);
    }

    public boolean handleChat(Player player, String message) {
        PendingInput pi = pending.remove(player.getUniqueId());
        if (pi == null) return false;
        plugin.getServer().getScheduler().runTask(plugin, () -> pi.onInput().accept(player, message));
        return true;
    }

    private record PendingInput(BiConsumer<Player, String> onInput) {}
}

