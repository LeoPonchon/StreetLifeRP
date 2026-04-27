package org.shimakuro.streetLifeRP.economy;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class CashItemService {
    private final NamespacedKey valueKey;

    public CashItemService(JavaPlugin plugin) {
        this.valueKey = new NamespacedKey(plugin, "cash_value");
    }

    public ItemStack create(double amount, String currencySymbol) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Cash");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Valeur: " + ChatColor.YELLOW + String.format("%.2f", amount) + currencySymbol,
                    ChatColor.DARK_GRAY + "Clic droit: encaisser"
            ));
            meta.getPersistentDataContainer().set(valueKey, PersistentDataType.DOUBLE, amount);
            item.setItemMeta(meta);
        }
        return item;
    }

    public Double read(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(valueKey, PersistentDataType.DOUBLE);
    }
}

