package org.shimakuro.streetLifeRP.items;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class SpecialItemService {
    private final NamespacedKey typeKey;

    public SpecialItemService(JavaPlugin plugin) {
        this.typeKey = new NamespacedKey(plugin, "slrp_item_type");
    }

    public ItemStack create(SpecialItemType type) {
        ItemStack item = new ItemStack(material(type));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name(type));
            meta.setLore(lore(type));
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type.name());
            item.setItemMeta(meta);
        }
        return item;
    }

    public SpecialItemType read(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String raw = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return null;
        try {
            return SpecialItemType.valueOf(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Material material(SpecialItemType type) {
        return switch (type) {
            case HANDCUFFS -> Material.TRIPWIRE_HOOK;
            case MEDKIT -> Material.PAPER;
            case DEFIB -> Material.HEART_OF_THE_SEA;
        };
    }

    private String name(SpecialItemType type) {
        return switch (type) {
            case HANDCUFFS -> ChatColor.AQUA + "Menottes";
            case MEDKIT -> ChatColor.GREEN + "Kit de soins";
            case DEFIB -> ChatColor.LIGHT_PURPLE + "Défibrillateur";
        };
    }

    private List<String> lore(SpecialItemType type) {
        return switch (type) {
            case HANDCUFFS -> List.of(
                    ChatColor.GRAY + "Clic droit sur un joueur",
                    ChatColor.DARK_GRAY + "Toggle menottes"
            );
            case MEDKIT -> List.of(
                    ChatColor.GRAY + "Clic droit sur un joueur",
                    ChatColor.DARK_GRAY + "Soigne (EMS)"
            );
            case DEFIB -> List.of(
                    ChatColor.GRAY + "Clic droit sur un joueur",
                    ChatColor.DARK_GRAY + "Réanime (EMS)"
            );
        };
    }
}
