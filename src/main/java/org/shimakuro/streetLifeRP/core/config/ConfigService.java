package org.shimakuro.streetLifeRP.core.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class ConfigService {
    private final JavaPlugin plugin;
    private FileConfiguration phoneConfig;
    private FileConfiguration messagesConfig;
    private FileConfiguration economyConfig;
    private FileConfiguration antiAbuseConfig;
    private FileConfiguration jobsConfig;
    private FileConfiguration chatConfig;
    private FileConfiguration banksConfig;
    private FileConfiguration vehiclesConfig;
    private FileConfiguration armoriesConfig;
    private FileConfiguration shopsConfig;
    private FileConfiguration justiceConfig;
    private FileConfiguration billingConfig;

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
        ensureDefaults();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        ensureDefaultResources();

        phoneConfig = loadYamlOrNull("phone.yml");
        messagesConfig = loadYamlOrNull("messages.yml");
        economyConfig = loadYamlOrNull("economy.yml");
        antiAbuseConfig = loadYamlOrNull("antiabuse.yml");
        jobsConfig = loadYamlOrNull("jobs.yml");
        chatConfig = loadYamlOrNull("chat.yml");
        banksConfig = loadYamlOrNull("banks.yml");
        vehiclesConfig = loadYamlOrNull("vehicles.yml");
        armoriesConfig = loadYamlOrNull("armories.yml");
        shopsConfig = loadYamlOrNull("shops.yml");
        justiceConfig = loadYamlOrNull("justice.yml");
        billingConfig = loadYamlOrNull("billing.yml");
    }

    public FileConfiguration raw() {
        return plugin.getConfig();
    }

    public FileConfiguration phoneRaw() {
        // Single source of truth: phone.yml
        return phoneConfig != null ? phoneConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "phone.yml"));
    }

    public FileConfiguration messagesRaw() {
        return messagesConfig != null ? messagesConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    public FileConfiguration economyRaw() {
        return economyConfig != null ? economyConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "economy.yml"));
    }

    public FileConfiguration antiAbuseRaw() {
        return antiAbuseConfig != null ? antiAbuseConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "antiabuse.yml"));
    }

    public FileConfiguration jobsRaw() {
        return jobsConfig != null ? jobsConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "jobs.yml"));
    }

    public FileConfiguration chatRaw() {
        return chatConfig != null ? chatConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "chat.yml"));
    }

    public FileConfiguration banksRaw() {
        return banksConfig != null ? banksConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "banks.yml"));
    }

    public FileConfiguration vehiclesRaw() {
        return vehiclesConfig != null ? vehiclesConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "vehicles.yml"));
    }

    public FileConfiguration armoriesRaw() {
        return armoriesConfig != null ? armoriesConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "armories.yml"));
    }

    public FileConfiguration shopsRaw() {
        return shopsConfig != null ? shopsConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "shops.yml"));
    }

    public FileConfiguration justiceRaw() {
        return justiceConfig != null ? justiceConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "justice.yml"));
    }

    public FileConfiguration billingRaw() {
        return billingConfig != null ? billingConfig : YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "billing.yml"));
    }

    public String phoneMenuTitle() {
        String title = phoneRaw().getString("phone.menu.title");
        return ChatColor.translateAlternateColorCodes('&', title != null ? title : "");
    }

    public String prefix() {
        String prefix = messagesRaw().getString("messages.prefix");
        return ChatColor.translateAlternateColorCodes('&', prefix != null ? prefix : "");
    }

    public double startingCash() {
        return economyRaw().getDouble("economy.starting_cash");
    }

    public double startingBank() {
        return economyRaw().getDouble("economy.starting_bank");
    }

    public String currencySymbol() {
        String sym = economyRaw().getString("economy.currency_symbol");
        return sym != null ? sym : "";
    }

    private void ensureDefaults() {
        // Ensure data folder exists and default extra config files are present.
        //noinspection ResultOfMethodCallIgnored
        plugin.getDataFolder().mkdirs();
        ensureDefaultResources();
    }

    private void ensureDefaultResources() {
        ensureDefaultResource("phone.yml");
        ensureDefaultResource("messages.yml");
        ensureDefaultResource("economy.yml");
        ensureDefaultResource("antiabuse.yml");
        ensureDefaultResource("jobs.yml");
        ensureDefaultResource("chat.yml");
        ensureDefaultResource("banks.yml");
        ensureDefaultResource("vehicles.yml");
        ensureDefaultResource("armories.yml");
        ensureDefaultResource("shops.yml");
        ensureDefaultResource("justice.yml");
        ensureDefaultResource("billing.yml");
    }

    private void ensureDefaultResource(String filename) {
        File target = new File(plugin.getDataFolder(), filename);
        if (target.exists()) return;
        try {
            plugin.saveResource(filename, false);
        } catch (IllegalArgumentException ignored) {
            // resource not bundled; ignore
        }
    }

    private FileConfiguration loadYamlOrNull(String filename) {
        File file = new File(plugin.getDataFolder(), filename);
        if (!file.exists()) return null;
        return YamlConfiguration.loadConfiguration(file);
    }
}
