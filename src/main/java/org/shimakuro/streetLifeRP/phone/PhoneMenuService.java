package org.shimakuro.streetLifeRP.phone;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.shimakuro.streetLifeRP.bank.BankService;
import org.shimakuro.streetLifeRP.characters.CharacterService;
import org.shimakuro.streetLifeRP.chat.ChatService;
import org.shimakuro.streetLifeRP.core.config.ConfigService;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.economy.CashItemService;
import org.shimakuro.streetLifeRP.economy.EconomyService;
import org.shimakuro.streetLifeRP.health.UnconsciousService;
import org.shimakuro.streetLifeRP.identity.IdentityService;
import org.shimakuro.streetLifeRP.input.InputService;
import org.shimakuro.streetLifeRP.items.SpecialItemService;
import org.shimakuro.streetLifeRP.items.SpecialItemType;
import org.shimakuro.streetLifeRP.jobs.JobService;
import org.shimakuro.streetLifeRP.jobs.JobType;
import org.shimakuro.streetLifeRP.justice.JusticeService;
import org.shimakuro.streetLifeRP.shops.ShopService;
import org.shimakuro.streetLifeRP.vehicles.GarageService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PhoneMenuService {
    private static final String APP_SOCIALS = "socials";
    private static final String APP_SMS = "sms";
    private static final String APP_TWEET = "tweet";
    private static final String APP_WALLET = "wallet";
    private static final String APP_CALL_911 = "call_911";
    private static final String APP_ID_CARD = "id_card";
    private static final String APP_JOB_BOOK = "job_book";
    private static final String APP_JOB_TOOLS = "job_tools";
    private static final String APP_SMS_TO = "sms_to";
    private static final String APP_BACK = "back";

    private final JavaPlugin plugin;
    private final ConfigService config;
    private final PlayerDataRepository repo;
    private final EconomyService economy;
    private final JobService jobs;
    private final CharacterService characters;
    private final IdentityService identity;
    private final ChatService chat;
    private final PhoneService phone;
    private final InputService input;
    private final SpecialItemService specialItems;
    private final BankService bank;
    private final NamespacedKey appKey;
    private final NamespacedKey uuidKey;

    public PhoneMenuService(
            JavaPlugin plugin,
            ConfigService config,
            PlayerDataRepository repo,
            EconomyService economy,
            JobService jobs,
            JusticeService justice,
            CharacterService characters,
            IdentityService identity,
            ShopService shop,
            ChatService chat,
            PhoneService phone,
            InputService input,
            CashItemService cashItems,
            SpecialItemService specialItems,
            GarageService garage,
            BankService bank,
            UnconsciousService unconscious
    ) {
        this.plugin = plugin;
        this.config = config;
        this.repo = repo;
        this.economy = economy;
        this.jobs = jobs;
        this.characters = characters;
        this.identity = identity;
        this.chat = chat;
        this.phone = phone;
        this.input = input;
        this.specialItems = specialItems;
        this.bank = bank;
        this.appKey = new NamespacedKey(plugin, "phone_app");
        this.uuidKey = new NamespacedKey(plugin, "phone_uuid");
    }

    public void open(Player player, String prefix) {
        PlayerData data = repo.get(player.getUniqueId());
        Inventory inv = Bukkit.createInventory(new PhoneHolder(), 54, title());

        ArrayList<String> walletLore = new ArrayList<>();
        walletLore.add(ChatColor.GRAY + "Voir cash + banque");
        if (bank != null) {
            BankService.PendingRobberyView pending = bank.pendingRobbery(player.getUniqueId());
            if (pending != null && pending.amountDirty() > 0.0) {
                walletLore.add(ChatColor.DARK_RED + "Argent sale: " + ChatColor.RED + economy.format(pending.amountDirty()));
                walletLore.add(ChatColor.GRAY + "Attente: " + ChatColor.WHITE + formatDuration(pending.remainingMillis()));
            }
        }
        if (data.hasFine()) {
            walletLore.add(ChatColor.RED + "Amende: " + ChatColor.GOLD + economy.format(data.fineAmount()));
        }

        inv.setItem(10, appItem(Material.WRITABLE_BOOK, ChatColor.AQUA + "Réseaux sociaux", List.of(ChatColor.GRAY + "SMS et tweets"), APP_SOCIALS));
        inv.setItem(12, appItem(Material.LEATHER, ChatColor.GOLD + "Portefeuille", walletLore, APP_WALLET));
        inv.setItem(14, appItem(Material.REDSTONE, ChatColor.RED + "911", List.of(ChatColor.GRAY + "Appel d'urgence"), APP_CALL_911));
        inv.setItem(16, appItem(Material.NAME_TAG, ChatColor.AQUA + "Carte d'identité", List.of(ChatColor.GRAY + "Recevoir sa carte"), APP_ID_CARD));
        inv.setItem(28, jobBookMenuItem());
        inv.setItem(30, appItem(Material.CHEST, ChatColor.GREEN + "Outils Métier", List.of(ChatColor.GRAY + "Récupérer les outils du job"), APP_JOB_TOOLS));

        player.openInventory(inv);
    }

    public boolean isPhoneInventory(InventoryHolder holder) {
        return holder instanceof PhoneHolder || holder instanceof SocialHolder || holder instanceof SmsHolder;
    }

    public boolean handleClick(Player player, ItemStack clicked, String prefix) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return false;
        String app = meta.getPersistentDataContainer().get(appKey, PersistentDataType.STRING);
        if (app == null || app.isBlank()) return false;

        PlayerData data = repo.get(player.getUniqueId());
        return switch (app) {
            case APP_BACK -> {
                open(player, prefix);
                yield true;
            }
            case APP_SOCIALS -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                openSocials(player);
                yield true;
            }
            case APP_SMS -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                openSmsContacts(player, prefix);
                yield true;
            }
            case APP_TWEET -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                player.closeInventory();
                input.request(player, prefix + ChatColor.BLUE + "Tweet ? (ecris dans le chat)", (p, msg) -> chat.sendTweet(p, msg, prefix));
                yield true;
            }
            case APP_SMS_TO -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                Player target = targetFromItem(meta);
                if (target == null) {
                    player.sendMessage(prefix + ChatColor.RED + "Joueur hors ligne.");
                    yield true;
                }
                player.closeInventory();
                input.request(player, prefix + ChatColor.AQUA + "Message SMS ? (ecris dans le chat)", (p, msg) -> {
                    int maxLen = config.raw().getInt("chat.max_message_length", 200);
                    phone.sendSms(p, target, msg, prefix, maxLen);
                });
                yield true;
            }
            case APP_WALLET -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                player.sendMessage(prefix + ChatColor.YELLOW + "Cash: " + ChatColor.GOLD + economy.format(data.cash()));
                player.sendMessage(prefix + ChatColor.YELLOW + "Banque: " + ChatColor.GOLD + economy.format(data.bank()));
                if (bank != null) {
                    BankService.PendingRobberyView pending = bank.pendingRobbery(player.getUniqueId());
                    if (pending != null && pending.amountDirty() > 0.0) {
                        player.sendMessage(prefix + ChatColor.RED + "Argent sale: " + ChatColor.GOLD + economy.format(pending.amountDirty())
                                + ChatColor.GRAY + " (attente " + ChatColor.WHITE + formatDuration(pending.remainingMillis()) + ChatColor.GRAY + ")");
                    }
                }
                if (data.hasFine()) {
                    player.sendMessage(prefix + ChatColor.RED + "Amende: " + ChatColor.GOLD + economy.format(data.fineAmount())
                            + ChatColor.GRAY + " (" + (data.fineReason() != null ? data.fineReason() : "Amende") + ")");
                }
                yield true;
            }
            case APP_CALL_911 -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                player.closeInventory();
                input.request(player, prefix + ChatColor.RED + "Message 911 ? (ecris dans le chat)", (p, msg) -> chat.sendEmergencyCall(p, msg, prefix));
                yield true;
            }
            case APP_ID_CARD -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                player.getInventory().addItem(identity.createIdCard(data.firstName(), data.lastName(), data.idNumber()));
                player.sendMessage(prefix + ChatColor.GREEN + "Carte d'identite ajoutee a l'inventaire.");
                yield true;
            }
            case APP_JOB_BOOK -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                player.closeInventory();
                plugin.getServer().getScheduler().runTask(plugin, () -> player.openBook(createJobInfoBook(player)));
                yield true;
            }
            case APP_JOB_TOOLS -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                giveJobTools(player, prefix);
                yield true;
            }
            default -> false;
        };
    }

    private void openSocials(Player player) {
        Inventory inv = Bukkit.createInventory(new SocialHolder(), 54, title());
        inv.setItem(20, appItem(Material.WRITABLE_BOOK, ChatColor.AQUA + "SMS", List.of(ChatColor.GRAY + "Envoyer un message prive"), APP_SMS));
        inv.setItem(24, appItem(Material.PAPER, ChatColor.BLUE + "Tweets", List.of(ChatColor.GRAY + "Publier un tweet"), APP_TWEET));
        addBackButton(inv);
        player.openInventory(inv);
    }

    private void openSmsContacts(Player player, String prefix) {
        Inventory inv = Bukkit.createInventory(new SmsHolder(), 54, ChatColor.AQUA + "SMS - Contacts");
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (slot >= inv.getSize() - 1) break;
            String rpName = characters.rpNameOrNull(online.getUniqueId());
            if (rpName == null) continue;
            ItemStack item = appItem(Material.PLAYER_HEAD, ChatColor.WHITE + rpName, List.of(ChatColor.GRAY + "Cliquer pour ecrire"), APP_SMS_TO);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, online.getUniqueId().toString());
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }
        addBackButton(inv);
        player.openInventory(inv);
    }

    private Player targetFromItem(ItemMeta meta) {
        String rawUuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
        if (rawUuid == null) return null;
        try {
            return Bukkit.getPlayer(UUID.fromString(rawUuid));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void giveJobTools(Player player, String prefix) {
        JobType job = jobs.get(player.getUniqueId());
        switch (job) {
            case POLICE -> {
                player.getInventory().addItem(specialItems.create(SpecialItemType.HANDCUFFS));
                player.sendMessage(prefix + ChatColor.GREEN + "Kit police donne.");
            }
            case EMS -> {
                player.getInventory().addItem(specialItems.create(SpecialItemType.MEDKIT));
                player.getInventory().addItem(specialItems.create(SpecialItemType.DEFIB));
                player.sendMessage(prefix + ChatColor.GREEN + "Kit EMS donne.");
            }
            default -> player.sendMessage(prefix + ChatColor.GRAY + "Aucun outil metier disponible pour ton job.");
        }
    }

    private ItemStack jobBookMenuItem() {
        String name = config.raw().getString("phone.job_book.item_name", "&bInfos metier");
        List<String> lore = config.raw().getStringList("phone.job_book.item_lore");
        if (lore == null || lore.isEmpty()) lore = List.of("&7Clique pour ouvrir");

        List<String> translatedLore = new ArrayList<>(lore.size());
        for (String line : lore) {
            translatedLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        return appItem(Material.WRITTEN_BOOK, ChatColor.translateAlternateColorCodes('&', name), translatedLore, APP_JOB_BOOK);
    }

    private ItemStack createJobInfoBook(Player player) {
        JobType job = jobs.get(player.getUniqueId());

        String author = config.raw().getString("phone.job_book.author", "StreetLifeRP");
        String title = config.raw().getString("phone.job_book.title", "Infos metier");
        double salary = jobs.salary(job);
        long cooldownSeconds = jobs.cooldownSeconds(job);
        String currency = config.currencySymbol();

        List<String> pages = new ArrayList<>();
        pages.add(ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "Metier\n\n"
                + ChatColor.DARK_GRAY + "Job: " + ChatColor.GRAY + jobDisplayName(job) + "\n"
                + ChatColor.DARK_GRAY + "Salaire: " + ChatColor.GRAY + salary + currency + "\n"
                + ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.GRAY + cooldownSeconds + "s\n\n"
                + ChatColor.DARK_GRAY + "Salaire automatique toutes les 30min de jeu.");

        String jobKey = job.name().toLowerCase();
        List<String> extraPages = config.raw().getStringList("phone.job_book.jobs." + jobKey + ".pages");
        if (extraPages != null) {
            for (String raw : extraPages) {
                if (raw == null || raw.isBlank()) continue;
                pages.add(ChatColor.translateAlternateColorCodes('&', raw));
            }
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setAuthor(ChatColor.translateAlternateColorCodes('&', author));
            meta.setTitle(ChatColor.translateAlternateColorCodes('&', title));
            meta.setPages(pages);
            book.setItemMeta(meta);
        }
        return book;
    }

    private String jobDisplayName(JobType job) {
        return switch (job) {
            case UNEMPLOYED -> "Sans emploi";
            case TAXI -> "Taxi";
            case BAKER -> "Boulanger";
            case BAR -> "Bar";
            case GROCERY -> "Superette";
            case JOURNALIST -> "Journaliste";
            case MECHANIC -> "Mecanicien";
            case DEALER -> "Dealer";
            case STRIP_CLUB -> "Strip club";
            case POLICE -> "Police";
            case EMS -> "Medic";
        };
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes <= 0) return seconds + "s";
        return minutes + "m" + (seconds < 10 ? "0" : "") + seconds + "s";
    }

    private ItemStack appItem(Material material, String name, List<String> lore, String appId) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(new ArrayList<>(lore));
            meta.getPersistentDataContainer().set(appKey, PersistentDataType.STRING, appId);
            item.setItemMeta(meta);
        }
        return item;
    }

    private void addBackButton(Inventory inv) {
        inv.setItem(inv.getSize() - 1, appItem(Material.ARROW, ChatColor.YELLOW + "Retour", List.of(
                ChatColor.GRAY + "Revenir au telephone"
        ), APP_BACK));
    }

    private String title() {
        ConfigurationSection section = config.raw().getConfigurationSection("phone");
        String raw = section != null ? section.getString("menu.title") : null;
        if (raw == null || raw.isBlank()) raw = "ꐟ";
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    private static final class PhoneHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class SocialHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class SmsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
