package org.shimakuro.streetLifeRP.core.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigService {
    private final JavaPlugin plugin;

    public ConfigService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public FileConfiguration raw() {
        return plugin.getConfig();
    }

    public String prefix() {
        String prefix = raw().getString("messages.prefix", "&7[&bStreetLifeRP&7] &r");
        return ChatColor.translateAlternateColorCodes('&', prefix);
    }

    public double startingCash() {
        return raw().getDouble("economy.starting_cash", 500.0);
    }

    public double startingBank() {
        return raw().getDouble("economy.starting_bank", 0.0);
    }

    public String currencySymbol() {
        return raw().getString("economy.currency_symbol", "$");
    }
}
