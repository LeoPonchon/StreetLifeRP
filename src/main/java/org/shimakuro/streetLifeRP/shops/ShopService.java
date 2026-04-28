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
import java.util.Locale;
import java.util.Set;

public final class ShopService {
    private final JavaPlugin plugin;
    private final AntiAbuseService antiAbuse;
    private final EconomyService economy;
    private final NamespacedKey priceKey;
    private final NamespacedKey nameKey;
    private final NamespacedKey backKey;
    private final NamespacedKey cmdKey;
    private final NamespacedKey cmdSenderKey;

    private enum CommandSenderMode {
        AUTO,
        PLAYER,
        CONSOLE
    }

    public ShopService(JavaPlugin plugin, AntiAbuseService antiAbuse, EconomyService economy) {
        this.plugin = plugin;
        this.antiAbuse = antiAbuse;
        this.economy = economy;
        this.priceKey = new NamespacedKey(plugin, "shop_price");
        this.nameKey = new NamespacedKey(plugin, "shop_name");
        this.backKey = new NamespacedKey(plugin, "shop_back");
        this.cmdKey = new NamespacedKey(plugin, "shop_cmd");
        this.cmdSenderKey = new NamespacedKey(plugin, "shop_cmd_sender");
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
                String giveCommand = it.getString("give_command");
                String commandSender = it.getString("command_sender", "auto");
                String qaItem = it.getString("qa_item");

                ItemStack item = null;
                if (qaItem != null && !qaItem.isBlank()) {
                    item = tryCreateQualityArmoryItem(qaItem.trim());
                    if (item != null) {
                        item.setAmount(Math.max(1, amount));
                        if (giveCommand == null || giveCommand.isBlank()) {
                            giveCommand = "qa give " + qaItem.trim() + " %player%";
                            commandSender = "console";
                        }
                    }
                }
                if (item == null) {
                    Material mat;
                    try {
                        mat = Material.valueOf(materialName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        continue;
                    }
                    item = new ItemStack(mat, Math.max(1, amount));
                }

                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    if (qaItem == null || qaItem.isBlank()) {
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                    }
                    List<String> lore = new ArrayList<>();
                    if (meta.getLore() != null && !meta.getLore().isEmpty()) {
                        lore.addAll(meta.getLore());
                        lore.add(" ");
                    }
                    lore.add(ChatColor.GRAY + "Prix: " + ChatColor.GOLD + economy.format(price));
                    lore.add(ChatColor.DARK_GRAY + "Clique pour acheter");
                    meta.setLore(lore);
                    meta.getPersistentDataContainer().set(priceKey, PersistentDataType.DOUBLE, price);
                    meta.getPersistentDataContainer().set(nameKey, PersistentDataType.STRING, key);
                    if (giveCommand != null && !giveCommand.isBlank()) {
                        meta.getPersistentDataContainer().set(cmdKey, PersistentDataType.STRING, giveCommand);
                        meta.getPersistentDataContainer().set(cmdSenderKey, PersistentDataType.STRING, commandSender);
                    }
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
        String cmdTemplate = meta.getPersistentDataContainer().get(cmdKey, PersistentDataType.STRING);
        String cmdSenderRaw = meta.getPersistentDataContainer().get(cmdSenderKey, PersistentDataType.STRING);
        if (price == null || key == null) return false;

        if (!antiAbuse.allowAndMark(player.getUniqueId(), AntiAbuseAction.SHOP_BUY)) {
            player.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
            return true;
        }

        if (!economy.spendCash(player.getUniqueId(), price, "shop:" + key)) {
            player.sendMessage(prefix + ChatColor.RED + "Fonds insuffisants.");
            return true;
        }

        if (cmdTemplate != null && !cmdTemplate.isBlank()) {
            CommandSenderMode mode = parseSenderMode(cmdSenderRaw);
            String rendered = renderCommand(cmdTemplate, player);
            boolean ok = dispatch(mode, cmdTemplate, rendered, player);
            if (!ok) {
                economy.addCash(player.getUniqueId(), price, "shop_refund:" + key);
                player.sendMessage(prefix + ChatColor.RED + "Achat annulé (commande échouée).");
                return true;
            }
            player.sendMessage(prefix + ChatColor.GREEN + "Achat effectué.");
            return true;
        }

        ItemStack bought = clicked.clone();
        ItemMeta boughtMeta = bought.getItemMeta();
        if (boughtMeta != null) {
            boughtMeta.getPersistentDataContainer().remove(priceKey);
            boughtMeta.getPersistentDataContainer().remove(nameKey);
            boughtMeta.getPersistentDataContainer().remove(cmdKey);
            boughtMeta.getPersistentDataContainer().remove(cmdSenderKey);
            bought.setItemMeta(boughtMeta);
        }
        player.getInventory().addItem(bought);
        player.sendMessage(prefix + ChatColor.GREEN + "Achat effectué.");
        return true;
    }

    private CommandSenderMode parseSenderMode(String raw) {
        if (raw == null || raw.isBlank()) return CommandSenderMode.AUTO;
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "player" -> CommandSenderMode.PLAYER;
            case "console" -> CommandSenderMode.CONSOLE;
            default -> CommandSenderMode.AUTO;
        };
    }

    private String renderCommand(String template, Player player) {
        if (template == null) return "";
        return template.replace("%player%", player.getName()).trim();
    }

    private boolean dispatch(CommandSenderMode mode, String template, String rawCmd, Player player) {
        if (rawCmd == null) return false;
        String cmd = rawCmd.trim();
        if (cmd.startsWith("/")) cmd = cmd.substring(1).trim();
        if (cmd.isBlank()) return false;

        CommandSenderMode effective = mode != null ? mode : CommandSenderMode.AUTO;
        if (effective == CommandSenderMode.AUTO) {
            boolean hasPlayerPlaceholder = template != null && template.contains("%player%");
            effective = hasPlayerPlaceholder ? CommandSenderMode.CONSOLE : CommandSenderMode.PLAYER;
        }

        boolean ok = dispatchAs(effective, cmd, player);
        if (!ok) {
            CommandSenderMode fallback = (effective == CommandSenderMode.CONSOLE) ? CommandSenderMode.PLAYER : CommandSenderMode.CONSOLE;
            ok = dispatchAs(fallback, cmd, player);
        }
        return ok;
    }

    private boolean dispatchAs(CommandSenderMode mode, String cmd, Player player) {
        if (mode == CommandSenderMode.PLAYER) {
            return player.performCommand(cmd);
        }
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private ItemStack tryCreateQualityArmoryItem(String itemName) {
        try {
            Class<?> api = Class.forName("me.zombie_striker.qg.api.QualityArmory");
            java.lang.reflect.Method m = api.getMethod("getCustomItemAsItemStack", String.class);
            Object out = m.invoke(null, itemName);
            if (out instanceof ItemStack it) return it;
            return null;
        } catch (Throwable ignored) {
            return null;
        }
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
