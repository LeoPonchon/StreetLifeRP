package org.shimakuro.streetLifeRP.gui;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Client-side mask of the player's inventory while a StreetLifeRP GUI is open.
 * Implemented via ProtocolLib packet rewriting (no server-side inventory edits).
 */
public final class GuiInventoryMaskService {
    private final Plugin plugin;
    private final boolean enabled;
    private final ProtocolManager protocol;

    private final Map<UUID, MaskState> states = new HashMap<>();
    private PacketListener listener;

    public GuiInventoryMaskService(Plugin plugin) {
        this.plugin = plugin;
        Plugin pl = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        this.enabled = pl != null && pl.isEnabled();
        this.protocol = enabled ? ProtocolLibrary.getProtocolManager() : null;
    }

    public boolean enabled() {
        return enabled;
    }

    public void enable() {
        if (!enabled || protocol == null) return;
        if (listener != null) return;

        listener = new PacketAdapter(plugin, ListenerPriority.HIGHEST,
                PacketType.Play.Server.WINDOW_ITEMS,
                PacketType.Play.Server.SET_SLOT
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                Player player = event.getPlayer();
                if (player == null) return;
                MaskState state = states.get(player.getUniqueId());
                if (state == null) return;

                try {
                    if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                        maskWindowItems(event, state);
                    } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                        maskSetSlot(event, state);
                    }
                } catch (Throwable ignored) {
                    // best effort: never break networking
                }
            }
        };

        protocol.addPacketListener(listener);
    }

    public void disable() {
        if (!enabled || protocol == null) return;
        if (listener != null) {
            protocol.removePacketListener(listener);
            listener = null;
        }
        states.clear();
    }

    public void startMask(Player player, int topSize) {
        if (!enabled || player == null) return;
        states.put(player.getUniqueId(), new MaskState(Math.max(0, topSize)));
    }

    public void stopMask(Player player) {
        if (player == null) return;
        states.remove(player.getUniqueId());
    }

    private void maskWindowItems(PacketEvent event, MaskState state) {
        List<ItemStack> items = event.getPacket().getItemListModifier().read(0);
        if (items == null || items.isEmpty()) return;
        int topSize = state.topSize();
        int start = topSize;
        int endExclusive = Math.min(items.size(), topSize + 36);
        if (start < 0 || start >= endExclusive) return;

        ItemStack air = new ItemStack(Material.AIR);
        for (int i = start; i < endExclusive; i++) {
            items.set(i, air);
        }
        event.getPacket().getItemListModifier().write(0, items);
    }

    private void maskSetSlot(PacketEvent event, MaskState state) {
        int topSize = state.topSize();
        int slot = readSlotIndex(event);
        if (slot < topSize || slot >= topSize + 36) return;
        event.getPacket().getItemModifier().write(0, new ItemStack(Material.AIR));
    }

    private int readSlotIndex(PacketEvent event) {
        // ProtocolLib abstracts across versions; slot is generally exposed as an integer.
        try {
            return event.getPacket().getIntegers().read(1);
        } catch (Throwable ignored) {
            // fallback (older mappings)
        }
        try {
            return event.getPacket().getIntegers().read(0);
        } catch (Throwable ignored) {
            // ignore
        }
        return -1;
    }

    private record MaskState(int topSize) {}
}

