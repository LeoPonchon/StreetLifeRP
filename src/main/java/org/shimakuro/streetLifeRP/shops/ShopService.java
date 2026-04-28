package org.shimakuro.streetLifeRP.shops;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseAction;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseService;
import org.shimakuro.streetLifeRP.economy.EconomyService;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ShopService {
    private final JavaPlugin plugin;
    private final AntiAbuseService antiAbuse;
    private final EconomyService economy;
    private final NamespacedKey priceKey;
    private final NamespacedKey nameKey;
    private final NamespacedKey backKey;

    public ShopService(JavaPlugin plugin, AntiAbuseService antiAbuse, EconomyService economy) {
        this.plugin = plugin;
        this.antiAbuse = antiAbuse;
        this.economy = economy;
        this.priceKey = new NamespacedKey(plugin, "shop_price");
        this.nameKey = new NamespacedKey(plugin, "shop_name");
        this.backKey = new NamespacedKey(plugin, "shop_back");
    }

    public void open(Player player, ConfigurationSection shopSection, String prefix) {
        String title = ChatColor.translateAlternateColorCodes('&', shopSection.getString("title", "&aBoutique"));
        Inventory inv = Bukkit.createInventory(new ShopHolder(), 27, title);

        ConfigurationSection items = shopSection.getConfigurationSection("items");
        if (items != null) {
            Set<String> keys = items.getKeys(false);
            for (String key : keys) {
                ConfigurationSection it = items.getConfigurationSection(key);
                if (it == null) continue;
                int slot = it.getInt("slot", -1);
                String materialName = it.getString("material", "BREAD");
                int amount = it.getInt("amount", 1);
                double price = it.getDouble("price", 0.0);
                String displayName = it.getString("name", key);

                Material mat;
                try {
                    mat = Material.valueOf(materialName.toUpperCase());
                } catch (IllegalArgumentException e) {
                    continue;
                }

                ItemStack item = new ItemStack(mat, Math.max(1, amount));
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                    List<String> lore = new ArrayList<>();
                    lore.add(ChatColor.GRAY + "Prix: " + ChatColor.GOLD + economy.format(price));
                    lore.add(ChatColor.DARK_GRAY + "Clique pour acheter");
                    meta.setLore(lore);
                    meta.getPersistentDataContainer().set(priceKey, PersistentDataType.DOUBLE, price);
                    meta.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, key);
                    item.setItemMeta(meta);
                }
                if (slot >= 0 && slot < inv.getSize()) {
                    inv.setItem(slot, item);
                }
            }
        }

        addBackButton(inv);
        player.openInventory(inv);
        player.sendMessage(prefix + ChatColor.GREEN + "Boutique ouverte.");
    }

    private void addBackButton(Inventory inv) {
        int slot = firstEmptyBackSlot(inv);
        inv.setItem(slot, backItem());
    }

    private int firstEmptyBackSlot(Inventory inv) {
        int[] preferred = {26, 18, 8, 17, 0};
        for (int slot : preferred) {
            ItemStack item = inv.getItem(slot);
            if (item == null || item.getType().isAir()) return slot;
        }
        return inv.getSize() - 1;
    }

    private ItemStack backItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Retour");
            meta.setLore(List.of(ChatColor.GRAY + "Revenir au téléphone"));
            meta.getPersistentDataContainer().set(backKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean tryBuy(Player player, ItemStack clicked, String prefix) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return false;
        Double price = meta.getPersistentDataContainer().get(priceKey, PersistentDataType.DOUBLE);
        String key = meta.getPersistentDataContainer().get(nameKey, PersistentDataType.STRING);
        if (price == null || key == null) return false;

        if (!antiAbuse.allowAndMark(player.getUniqueId(), AntiAbuseAction.SHOP_BUY)) {
            player.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
            return true;
        }

        if (!economy.spendCash(player.getUniqueId(), price, "shop:" + key)) {
            player.sendMessage(prefix + ChatColor.RED + "Fonds insuffisants.");
            return true;
        }

        ItemStack bought = clicked.clone();
        ItemMeta boughtMeta = bought.getItemMeta();
        if (boughtMeta != null) {
            boughtMeta.getPersistentDataContainer().remove(priceKey);
            boughtMeta.getPersistentDataContainer().remove(nameKey);
            bought.setItemMeta(boughtMeta);
        }
        player.getInventory().addItem(bought);
        player.sendMessage(prefix + ChatColor.GREEN + "Achat effectué.");
        return true;
    }

    public boolean isShopInventory(InventoryHolder holder) {
        return holder instanceof ShopHolder;
    }

    public boolean isBackButton(ItemStack clicked) {
        if (clicked == null || clicked.getType().isAir()) return false;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return false;
        Byte value = meta.getPersistentDataContainer().get(backKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    private static final class ShopHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
