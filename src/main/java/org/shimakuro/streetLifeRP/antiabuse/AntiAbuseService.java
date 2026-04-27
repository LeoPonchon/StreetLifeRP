package org.shimakuro.streetLifeRP.antiabuse;

import org.bukkit.configuration.ConfigurationSection;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AntiAbuseService {
    private final Map<UUID, EnumMap<AntiAbuseAction, Long>> lastActionsMillis = new ConcurrentHashMap<>();
    private volatile EnumMap<AntiAbuseAction, Long> cooldownMillis = new EnumMap<>(AntiAbuseAction.class);

    public void reloadFromConfig(ConfigurationSection section) {
        EnumMap<AntiAbuseAction, Long> next = new EnumMap<>(AntiAbuseAction.class);
        for (AntiAbuseAction action : AntiAbuseAction.values()) {
            long seconds = section != null ? section.getLong(action.name().toLowerCase(), 0L) : 0L;
            next.put(action, seconds * 1000L);
        }
        cooldownMillis = next;
    }

    public boolean allowAndMark(UUID uuid, AntiAbuseAction action) {
        long now = System.currentTimeMillis();
        EnumMap<AntiAbuseAction, Long> perPlayer = lastActionsMillis.computeIfAbsent(uuid, unused -> new EnumMap<>(AntiAbuseAction.class));
        long cooldown = cooldownMillis.getOrDefault(action, 0L);
        Long last = perPlayer.get(action);
        if (last != null && cooldown > 0 && now - last < cooldown) {
            return false;
        }
        perPlayer.put(action, now);
        return true;
    }
}

