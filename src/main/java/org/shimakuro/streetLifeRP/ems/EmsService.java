package org.shimakuro.streetLifeRP.ems;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.shimakuro.streetLifeRP.health.UnconsciousService;

public final class EmsService {
    private final UnconsciousService unconscious;

    public EmsService(UnconsciousService unconscious) {
        this.unconscious = unconscious;
    }

    public void heal(Player target, String prefix) {
        double max = target.getAttribute(Attribute.MAX_HEALTH) != null
                ? target.getAttribute(Attribute.MAX_HEALTH).getValue()
                : 20.0;
        target.setFireTicks(0);
        target.setHealth(Math.min(max, max));
        target.setFoodLevel(20);
        target.setSaturation(20f);
        target.sendMessage(prefix + ChatColor.GREEN + "Vous avez été soigné.");
    }

    public void revive(Player target, String prefix) {
        // Defib: only bring unconscious -> conscious (no full heal, nothing else).
        if (!unconscious.isUnconscious(target)) return;
        unconscious.reviveToConscious(target);
        target.sendMessage(prefix + ChatColor.GREEN + "Vous avez été réanimé.");
    }
}

