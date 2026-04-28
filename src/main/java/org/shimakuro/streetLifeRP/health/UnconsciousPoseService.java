package org.shimakuro.streetLifeRP.health;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataValue;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.reflect.EquivalentConverter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

final class UnconsciousPoseService {
    private final ProtocolManager protocol;
    private final boolean enabled;

    UnconsciousPoseService() {
        Plugin pl = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        this.enabled = pl != null && pl.isEnabled();
        this.protocol = enabled ? ProtocolLibrary.getProtocolManager() : null;
    }

    boolean enabled() {
        return enabled;
    }

    void setCrawlPose(Player target, boolean crawling) {
        if (!enabled || protocol == null) return;

        EnumWrappers.EntityPose pose = crawling ? EnumWrappers.EntityPose.SWIMMING : EnumWrappers.EntityPose.STANDING;

        Object nativePose;
        try {
            EquivalentConverter<EnumWrappers.EntityPose> conv = EnumWrappers.getEntityPoseConverter();
            nativePose = conv != null ? conv.getGeneric(pose) : null;
        } catch (Throwable ignored) {
            nativePose = null;
        }
        if (nativePose == null) return;

        WrappedDataWatcher.Serializer serializer = WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass());
        if (serializer == null) return;

        // Entity pose index is stable for modern versions (1.14+): 6
        WrappedDataValue dv = WrappedDataValue.fromWrappedValue(6, serializer, nativePose);
        List<WrappedDataValue> list = new ArrayList<>(1);
        list.add(dv);

        PacketContainer packet = protocol.createPacket(PacketType.Play.Server.ENTITY_METADATA);
        packet.getIntegers().write(0, target.getEntityId());
        packet.getDataValueCollectionModifier().write(0, list);

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;
            try {
                protocol.sendServerPacket(viewer, packet);
            } catch (Exception ignored) {
                // best effort
            }
        }
    }
}
