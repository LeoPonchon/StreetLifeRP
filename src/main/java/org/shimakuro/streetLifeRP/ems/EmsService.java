package org.shimakuro.streetLifeRP.ems;

import org.bukkit.ChatColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

public final class EmsService {
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
        double max = target.getAttribute(Attribute.MAX_HEALTH) != null
                ? target.getAttribute(Attribute.MAX_HEALTH).getValue()
                : 20.0;
        if (target.getHealth() <= 0.0) {
            return;
        }
        target.setHealth(Math.min(max, 1.0));
        target.setFoodLevel(20);
        target.sendMessage(prefix + ChatColor.GREEN + "Vous avez été réanimé.");
    }
}
