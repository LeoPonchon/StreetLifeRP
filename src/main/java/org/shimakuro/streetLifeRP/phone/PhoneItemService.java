package org.shimakuro.streetLifeRP.phone;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class PhoneItemService {
    public static final int HOTBAR_SLOT = 8; // slot 9

    private final NamespacedKey key;

    public PhoneItemService(JavaPlugin plugin) {
        this.key = new NamespacedKey(plugin, "slrp_phone");
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Téléphone");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Slot 9 (obligatoire)",
                    ChatColor.DARK_GRAY + "Clic droit: ouvrir"
            ));
            meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isPhone(ItemStack item) {
        if (item == null || item.getType().isAir()) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        Byte b = meta.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return b != null && b == (byte) 1;
    }
}

