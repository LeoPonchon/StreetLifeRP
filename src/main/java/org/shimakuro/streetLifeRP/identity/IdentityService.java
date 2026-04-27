package org.shimakuro.streetLifeRP.identity;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

public final class IdentityService {
    private final SecureRandom random = new SecureRandom();

    public String generateIdNumber(UUID uuid) {
        int n = random.nextInt(90000000) + 10000000;
        return "SL-" + n;
    }

    public ItemStack createIdCard(String firstName, String lastName, String idNumber) {
        ItemStack card = new ItemStack(Material.PAPER);
        ItemMeta meta = card.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Carte d'identité");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Nom: " + ChatColor.WHITE + lastName,
                    ChatColor.GRAY + "Prénom: " + ChatColor.WHITE + firstName,
                    ChatColor.GRAY + "ID: " + ChatColor.WHITE + idNumber
            ));
            card.setItemMeta(meta);
        }
        return card;
    }
}

