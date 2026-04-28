package org.shimakuro.streetLifeRP.core;

import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseService;
import org.shimakuro.streetLifeRP.characters.CharacterService;
import org.shimakuro.streetLifeRP.chat.ChatService;
import org.shimakuro.streetLifeRP.core.config.ConfigService;
import org.shimakuro.streetLifeRP.core.log.AuditLogService;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.economy.EconomyService;
import org.shimakuro.streetLifeRP.bank.BankService;
import org.shimakuro.streetLifeRP.health.UnconsciousService;
import org.shimakuro.streetLifeRP.items.SpecialItemService;
import org.shimakuro.streetLifeRP.identity.IdentityService;
import org.shimakuro.streetLifeRP.jobs.JobService;
import org.shimakuro.streetLifeRP.justice.JusticeService;
import org.shimakuro.streetLifeRP.ems.EmsService;
import org.shimakuro.streetLifeRP.shops.ShopService;
import org.shimakuro.streetLifeRP.phone.PhoneService;
import org.shimakuro.streetLifeRP.phone.PhoneMenuService;
import org.shimakuro.streetLifeRP.phone.PhoneItemService;
import org.shimakuro.streetLifeRP.input.InputService;
import org.shimakuro.streetLifeRP.economy.CashItemService;
import org.shimakuro.streetLifeRP.trade.TradeService;
import org.shimakuro.streetLifeRP.vehicles.GarageService;
import org.bukkit.plugin.java.JavaPlugin;

public final class StreetLifeRPContext {
    private final JavaPlugin plugin;
    private final ConfigService config;
    private final AuditLogService auditLog;
    private final PlayerDataRepository playerData;
    private final AntiAbuseService antiAbuse;
    private final CharacterService characters;
    private final IdentityService identity;
    private final EconomyService economy;
    private final BankService bank;
    private final ShopService shop;
    private final JobService jobs;
    private final JusticeService justice;
    private final EmsService ems;
    private final ChatService chat;
    private final SpecialItemService items;
    private final UnconsciousService unconscious;
    private final PhoneService phone;
    private final PhoneMenuService phoneMenu;
    private final PhoneItemService phoneItems;
    private final InputService input;
    private final CashItemService cashItems;
    private final TradeService trade;
    private final GarageService garage;

    public StreetLifeRPContext(
            JavaPlugin plugin,
            ConfigService config,
            AuditLogService auditLog,
            PlayerDataRepository playerData,
            AntiAbuseService antiAbuse,
            CharacterService characters,
            IdentityService identity,
            EconomyService economy,
            BankService bank,
            ShopService shop,
            JobService jobs,
            JusticeService justice,
            EmsService ems,
            ChatService chat,
            SpecialItemService items,
            UnconsciousService unconscious,
            PhoneService phone,
            PhoneMenuService phoneMenu,
            PhoneItemService phoneItems,
            InputService input,
            CashItemService cashItems,
            TradeService trade,
            GarageService garage
    ) {
        this.plugin = plugin;
        this.config = config;
        this.auditLog = auditLog;
        this.playerData = playerData;
        this.antiAbuse = antiAbuse;
        this.characters = characters;
        this.identity = identity;
        this.economy = economy;
        this.bank = bank;
        this.shop = shop;
        this.jobs = jobs;
        this.justice = justice;
        this.ems = ems;
        this.chat = chat;
        this.items = items;
        this.unconscious = unconscious;
        this.phone = phone;
        this.phoneMenu = phoneMenu;
        this.phoneItems = phoneItems;
        this.input = input;
        this.cashItems = cashItems;
        this.trade = trade;
        this.garage = garage;
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public ConfigService config() {
        return config;
    }

    public AuditLogService auditLog() {
        return auditLog;
    }

    public PlayerDataRepository playerData() {
        return playerData;
    }

    public AntiAbuseService antiAbuse() {
        return antiAbuse;
    }

    public CharacterService characters() {
        return characters;
    }

    public IdentityService identity() {
        return identity;
    }

    public EconomyService economy() {
        return economy;
    }

    public BankService bank() {
        return bank;
    }

    public ShopService shop() {
        return shop;
    }

    public JobService jobs() {
        return jobs;
    }

    public JusticeService justice() {
        return justice;
    }

    public EmsService ems() {
        return ems;
    }

    public ChatService chat() {
        return chat;
    }

    public SpecialItemService items() {
        return items;
    }

    public UnconsciousService unconscious() {
        return unconscious;
    }

    public PhoneService phone() {
        return phone;
    }

    public PhoneMenuService phoneMenu() {
        return phoneMenu;
    }

    public PhoneItemService phoneItems() {
        return phoneItems;
    }

    public InputService input() {
        return input;
    }

    public CashItemService cashItems() {
        return cashItems;
    }

    public TradeService trade() {
        return trade;
    }

    public GarageService garage() {
        return garage;
    }

    public void reloadAll() {
        config.reload();
        antiAbuse.reloadFromConfig(config.raw().getConfigurationSection("antiabuse.cooldowns_seconds"));
        jobs.reloadFromConfig(config.raw().getConfigurationSection("jobs"));
        justice.reloadFromConfig(config.raw().getConfigurationSection("justice"));
        chat.reloadFromConfig(config.raw().getConfigurationSection("chat"));
        bank.reloadFromConfig(config.raw().getConfigurationSection("banks"));
        garage.reloadFromConfig(config.raw().getConfigurationSection("vehicles"));
    }
}
