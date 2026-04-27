package org.shimakuro.streetLifeRP;

import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseService;
import org.shimakuro.streetLifeRP.characters.CharacterService;
import org.shimakuro.streetLifeRP.chat.ChatService;
import org.shimakuro.streetLifeRP.chat.ProximityChatListener;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;
import org.shimakuro.streetLifeRP.core.config.ConfigService;
import org.shimakuro.streetLifeRP.core.log.AuditLogService;
import org.shimakuro.streetLifeRP.core.module.Module;
import org.shimakuro.streetLifeRP.core.module.ModuleManager;
import org.shimakuro.streetLifeRP.data.PlayerDataListener;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.economy.EconomyService;
import org.shimakuro.streetLifeRP.ems.EmsService;
import org.shimakuro.streetLifeRP.items.SpecialItemListener;
import org.shimakuro.streetLifeRP.items.SpecialItemService;
import org.shimakuro.streetLifeRP.identity.IdentityService;
import org.shimakuro.streetLifeRP.jobs.JobService;
import org.shimakuro.streetLifeRP.justice.JusticeListener;
import org.shimakuro.streetLifeRP.justice.CuffedRestrictionListener;
import org.shimakuro.streetLifeRP.justice.JusticeService;
import org.shimakuro.streetLifeRP.phone.PhoneService;
import org.shimakuro.streetLifeRP.phone.PhoneMenuListener;
import org.shimakuro.streetLifeRP.phone.PhoneMenuService;
import org.shimakuro.streetLifeRP.phone.PhoneItemService;
import org.shimakuro.streetLifeRP.phone.PhoneItemListener;
import org.shimakuro.streetLifeRP.phone.PhoneSlotGuardListener;
import org.shimakuro.streetLifeRP.shops.ShopListener;
import org.shimakuro.streetLifeRP.shops.ShopService;
import org.shimakuro.streetLifeRP.trade.TradeListener;
import org.shimakuro.streetLifeRP.trade.TradeService;
import org.shimakuro.streetLifeRP.input.InputListener;
import org.shimakuro.streetLifeRP.input.InputService;
import org.shimakuro.streetLifeRP.economy.CashItemListener;
import org.shimakuro.streetLifeRP.economy.CashItemService;
import org.shimakuro.streetLifeRP.justice.PoliceInteractListener;
import org.bukkit.plugin.java.JavaPlugin;

public final class StreetLifeRP extends JavaPlugin {

    private ModuleManager moduleManager;
    private StreetLifeRPContext context;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        ConfigService configService = new ConfigService(this);
        AuditLogService auditLogService = new AuditLogService(this);
        PlayerDataRepository playerDataRepository = new PlayerDataRepository(this);
        AntiAbuseService antiAbuseService = new AntiAbuseService();
        IdentityService identityService = new IdentityService();
        EconomyService economyService = new EconomyService(playerDataRepository, antiAbuseService, auditLogService, configService.currencySymbol());
        CharacterService characterService = new CharacterService(playerDataRepository, identityService, configService.startingCash(), configService.startingBank());
        ShopService shopService = new ShopService(this, antiAbuseService, economyService);
        JobService jobService = new JobService(playerDataRepository, antiAbuseService, economyService);
        JusticeService justiceService = new JusticeService(this, playerDataRepository, antiAbuseService, economyService, auditLogService);
        EmsService emsService = new EmsService();
        ChatService chatService = new ChatService(playerDataRepository, jobService, antiAbuseService, auditLogService);
        SpecialItemService itemService = new SpecialItemService(this);
        PhoneService phoneService = new PhoneService(playerDataRepository, jobService, antiAbuseService, auditLogService);
        InputService inputService = new InputService(this);
        CashItemService cashItemService = new CashItemService(this);
        PhoneItemService phoneItemService = new PhoneItemService(this);
        TradeService tradeService = new TradeService(this, auditLogService);
        PhoneMenuService phoneMenuService = new PhoneMenuService(
                this,
                configService,
                playerDataRepository,
                economyService,
                jobService,
                justiceService,
                characterService,
                identityService,
                shopService,
                chatService,
                phoneService,
                inputService,
                cashItemService,
                itemService
        );

        this.context = new StreetLifeRPContext(
                this,
                configService,
                auditLogService,
                playerDataRepository,
                antiAbuseService,
                characterService,
                identityService,
                economyService,
                shopService,
                jobService,
                justiceService,
                emsService,
                chatService,
                itemService,
                phoneService,
                phoneMenuService,
                phoneItemService,
                inputService,
                cashItemService,
                tradeService
        );

        this.moduleManager = new ModuleManager(getLogger());
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "Logs";
            }

            @Override
            public void enable() {
                auditLogService.open();
                auditLogService.logInfo("StreetLifeRP enabled.");
            }

            @Override
            public void disable() {
                auditLogService.logInfo("StreetLifeRP disabled.");
                auditLogService.close();
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "Configuration";
            }

            @Override
            public void enable() {
                context.reloadAll();
            }

            @Override
            public void disable() {
                // no-op
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "PlayerData";
            }

            @Override
            public void enable() {
                playerDataRepository.init();
                getServer().getPluginManager().registerEvents(new PlayerDataListener(context), StreetLifeRP.this);
            }

            @Override
            public void disable() {
                playerDataRepository.saveAll();
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "Justice";
            }

            @Override
            public void enable() {
                getServer().getPluginManager().registerEvents(new JusticeListener(context), StreetLifeRP.this);
                getServer().getPluginManager().registerEvents(new CuffedRestrictionListener(context), StreetLifeRP.this);
                getServer().getPluginManager().registerEvents(new PoliceInteractListener(context), StreetLifeRP.this);
            }

            @Override
            public void disable() {
                // no-op
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "Shop";
            }

            @Override
            public void enable() {
                getServer().getPluginManager().registerEvents(new ShopListener(context), StreetLifeRP.this);
            }

            @Override
            public void disable() {
                // no-op
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "Chat";
            }

            @Override
            public void enable() {
                getServer().getPluginManager().registerEvents(new ProximityChatListener(context), StreetLifeRP.this);
            }

            @Override
            public void disable() {
                // no-op
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "Items";
            }

            @Override
            public void enable() {
                getServer().getPluginManager().registerEvents(new SpecialItemListener(context), StreetLifeRP.this);
            }

            @Override
            public void disable() {
                // no-op
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "PhoneMenu";
            }

            @Override
            public void enable() {
                getServer().getPluginManager().registerEvents(new PhoneMenuListener(context), StreetLifeRP.this);
                getServer().getPluginManager().registerEvents(new PhoneItemListener(context), StreetLifeRP.this);
                getServer().getPluginManager().registerEvents(new PhoneSlotGuardListener(context), StreetLifeRP.this);
            }

            @Override
            public void disable() {
                // no-op
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "Input";
            }

            @Override
            public void enable() {
                getServer().getPluginManager().registerEvents(new InputListener(context), StreetLifeRP.this);
            }

            @Override
            public void disable() {
                // no-op
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "EconomyItems";
            }

            @Override
            public void enable() {
                getServer().getPluginManager().registerEvents(new CashItemListener(context), StreetLifeRP.this);
            }

            @Override
            public void disable() {
                // no-op
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "Trade";
            }

            @Override
            public void enable() {
                getServer().getPluginManager().registerEvents(new TradeListener(context), StreetLifeRP.this);
            }

            @Override
            public void disable() {
                context.trade().cancelAll();
            }
        });
        moduleManager.register(new Module() {
            @Override
            public String name() {
                return "Commands";
            }

            @Override
            public void enable() {
                // no-op: commandes désactivées (full interaction mode)
            }

            @Override
            public void disable() {
                // no-op
            }
        });

        moduleManager.enableAll();

    }

    @Override
    public void onDisable() {
        if (moduleManager != null) {
            moduleManager.disableAll();
        }
    }
}
