package org.shimakuro.streetLifeRP.bank;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.shimakuro.streetLifeRP.core.config.ConfigService;
import org.shimakuro.streetLifeRP.economy.CashItemService;
import org.shimakuro.streetLifeRP.economy.EconomyService;
import org.shimakuro.streetLifeRP.input.InputService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public final class BankService {
    private static final String ACTION_DEPOSIT = "deposit";
    private static final String ACTION_WITHDRAW = "withdraw";
    private static final String ACTION_ROB = "rob";

    private final JavaPlugin plugin;
    private final ConfigService config;
    private final EconomyService economy;
    private final InputService input;
    private final CashItemService cashItems;
    private final BankRobberyService robbery;

    private final NamespacedKey actionKey;
    private final NamespacedKey bankIdKey;

    private volatile Map<String, BankDef> banks = Map.of();

    public BankService(JavaPlugin plugin, ConfigService config, EconomyService economy, InputService input, CashItemService cashItems, BankRobberyService robbery) {
        this.plugin = plugin;
        this.config = config;
        this.economy = economy;
        this.input = input;
        this.cashItems = cashItems;
        this.robbery = robbery;
        this.actionKey = new NamespacedKey(plugin, "bank_action");
        this.bankIdKey = new NamespacedKey(plugin, "bank_id");
    }

    public void reloadFromConfig(ConfigurationSection section) {
        Map<String, BankDef> next = new HashMap<>();
        ConfigurationSection list = section != null ? section.getConfigurationSection("list") : null;
        if (list != null) {
            for (String id : list.getKeys(false)) {
                ConfigurationSection b = list.getConfigurationSection(id);
                if (b == null) continue;
                String name = b.getString("name", "&eBanque");
                Zone terminal = Zone.read(b.getConfigurationSection("terminal"));
                double initial = b.getDouble("vault_initial", 0.0);
                if (terminal == null) continue;
                next.put(id.toLowerCase(Locale.ROOT), new BankDef(id.toLowerCase(Locale.ROOT), name, terminal, initial));
            }
        }
        banks = Map.copyOf(next);
        robbery.ensureBanksKnown(banks.values());
    }

    public BankDef findBankNear(Player player) {
        if (player == null) return null;
        for (BankDef b : banks.values()) {
            if (b.terminal().isNearSquare(player.getLocation())) return b;
        }
        return null;
    }

    public void open(Player player, BankDef bank, String prefix) {
        if (bank == null) return;
        String title = ChatColor.translateAlternateColorCodes('&', bank.name());
        Inventory inv = Bukkit.createInventory(new BankHolder(bank.id()), 27, title);

        inv.setItem(11, actionItem(Material.EMERALD, ChatColor.GREEN + "Déposer à la banque", List.of(
                ChatColor.GRAY + "Transférer cash → banque"
        ), ACTION_DEPOSIT, bank.id()));

        inv.setItem(13, actionItem(Material.PAPER, ChatColor.YELLOW + "Retirer en cash", List.of(
                ChatColor.GRAY + "Transférer banque → cash",
                ChatColor.DARK_GRAY + "Option billet via téléphone"
        ), ACTION_WITHDRAW, bank.id()));

        double vault = robbery.vaultAmount(bank.id());
        inv.setItem(15, actionItem(Material.REDSTONE, ChatColor.RED + "Braquer la banque", List.of(
                ChatColor.GRAY + "Vol: 10% du coffre / clic",
                ChatColor.DARK_GRAY + "Coffre: " + economy.format(vault)
        ), ACTION_ROB, bank.id()));

        inv.setItem(26, backItem());
        player.openInventory(inv);
        player.sendMessage(prefix + ChatColor.GRAY + "Banque: " + ChatColor.WHITE + ChatColor.stripColor(title));
    }

    public boolean isBankInventory(InventoryHolder holder) {
        return holder instanceof BankHolder;
    }

    public void handleClick(Player player, ItemStack clicked, String prefix) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return;
        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        String bankId = meta.getPersistentDataContainer().get(bankIdKey, PersistentDataType.STRING);
        if (action == null) return;

        if ("back".equals(action)) {
            player.closeInventory();
            return;
        }
        if (bankId == null || bankId.isBlank()) return;

        BankDef bank = banks.get(bankId.toLowerCase(Locale.ROOT));
        if (bank == null) return;

        switch (action) {
            case ACTION_DEPOSIT -> {
                player.closeInventory();
                input.request(player, prefix + ChatColor.GREEN + "Montant à déposer ? (cash → banque)", (p, raw) -> {
                    Double amount = parseAmount(raw);
                    if (amount == null || amount <= 0) {
                        p.sendMessage(prefix + ChatColor.RED + "Montant invalide.");
                        return;
                    }
                    boolean ok = economy.deposit(p.getUniqueId(), amount);
                    p.sendMessage(prefix + (ok ? ChatColor.GREEN + "Dépôt OK: " + economy.format(amount) : ChatColor.RED + "Dépôt refusé."));
                });
            }
            case ACTION_WITHDRAW -> {
                player.closeInventory();
                input.request(player, prefix + ChatColor.YELLOW + "Montant à retirer ? (banque → cash)", (p, raw) -> {
                    Double amount = parseAmount(raw);
                    if (amount == null || amount <= 0) {
                        p.sendMessage(prefix + ChatColor.RED + "Montant invalide.");
                        return;
                    }
                    boolean ok = economy.withdraw(p.getUniqueId(), amount);
                    p.sendMessage(prefix + (ok ? ChatColor.GREEN + "Retrait OK: " + economy.format(amount) : ChatColor.RED + "Retrait refusé."));
                });
            }
            case ACTION_ROB -> {
                player.closeInventory();
                robbery.tryRob(player, bank, prefix);
            }
            default -> {
                // no-op
            }
        }
    }

    private ItemStack actionItem(Material material, String name, List<String> lore, String action, String bankId) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(new ArrayList<>(lore));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
            meta.getPersistentDataContainer().set(bankIdKey, PersistentDataType.STRING, bankId);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack backItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Fermer");
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "back");
            item.setItemMeta(meta);
        }
        return item;
    }

    private Double parseAmount(String raw) {
        if (raw == null) return null;
        String normalized = raw.trim().replace(',', '.');
        try {
            return Double.parseDouble(normalized);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public record BankDef(String id, String name, Zone terminal, double vaultInitial) {}

    public record Zone(String world, double x, double y, double z, double radius) {
        static Zone read(ConfigurationSection section) {
            if (section == null) return null;
            String world = section.getString("world");
            if (world == null || world.isBlank()) return null;
            double x = section.getDouble("x", 0.0);
            double y = section.getDouble("y", 64.0);
            double z = section.getDouble("z", 0.0);
            double radius = Math.max(0.5, section.getDouble("radius", 4.0));
            return new Zone(world, x, y, z, radius);
        }

        boolean isNearSquare(org.bukkit.Location loc) {
            if (loc == null || loc.getWorld() == null) return false;
            if (!loc.getWorld().getName().equalsIgnoreCase(world)) return false;
            double dx = Math.abs(loc.getX() - x);
            double dy = Math.abs(loc.getY() - y);
            double dz = Math.abs(loc.getZ() - z);
            return dx <= radius && dy <= radius && dz <= radius;
        }
    }

    private record BankHolder(String bankId) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}

