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
import org.shimakuro.streetLifeRP.billing.BillingService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private static final String APP_GPS = "gps";
    private static final String APP_GPS_PLACES = "gps_places";
    private static final String APP_GPS_PLAYERS = "gps_players";
    private static final String APP_GPS_PLACE = "gps_place";
    private static final String APP_GPS_SEND = "gps_send";
    private static final String APP_ADS = "ads";
    private static final String APP_CRIME = "crime";
    private static final String APP_SMS_TO = "sms_to";
    private static final String APP_CHARACTER_CREATE = "character_create";
    private static final String APP_CHARACTER_SET_FIRST = "character_set_first";
    private static final String APP_CHARACTER_SET_LAST = "character_set_last";
    private static final String APP_CHARACTER_CONFIRM = "character_confirm";
    private static final String APP_BACK = "back";

    private static final int[] AREA_WALLET = {0, 1, 2, 9, 10, 11};
    private static final int[] AREA_SOCIALS = {3, 4, 5, 12, 13, 14};
    private static final int[] AREA_ID_CARD = {6, 7, 8, 15, 16, 17};
    private static final int[] AREA_911 = {18, 19, 20, 27, 28, 29};
    private static final int[] AREA_JOB_BOOK = {21, 22, 23, 30, 31, 32};
    private static final int[] AREA_JOB_TOOLS = {24, 25, 26, 33, 34, 35};
    private static final int[] AREA_GPS = {36, 37, 38, 45, 46, 47};
    private static final int[] AREA_ADS = {39, 40, 41, 48, 49, 50};
    private static final int[] AREA_CRIME = {42, 43, 44, 51, 52, 53};

    private static final NamespacedKey NEXO_EMPTY_ITEM_MODEL = NamespacedKey.fromString("nexo:empty");
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
    private final BillingService billing;
    private final BankService bank;
    private final NamespacedKey appKey;
    private final NamespacedKey uuidKey;
    private final NamespacedKey gpsPlaceKey;
    private final Map<UUID, PendingCharacterCreate> pendingCharacterCreate = new HashMap<>();

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
            UnconsciousService unconscious,
            BillingService billing
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
        this.billing = billing;
        this.appKey = new NamespacedKey(plugin, "phone_app");
        this.uuidKey = new NamespacedKey(plugin, "phone_uuid");
        this.gpsPlaceKey = new NamespacedKey(plugin, "gps_place");
    }

    public void open(Player player, String prefix) {
        PlayerData data = repo.get(player.getUniqueId());
        PhoneHolder holder = new PhoneHolder();
        Inventory inv = Bukkit.createInventory(holder, 54, config.phoneMenuTitle());

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
        setTopArea(inv, AREA_WALLET, clickItem(ChatColor.GOLD + "Portefeuille", walletLore, APP_WALLET));
        setTopArea(inv, AREA_SOCIALS, clickItem(ChatColor.AQUA + "Reseaux sociaux", List.of(ChatColor.GRAY + "SMS et tweets"), APP_SOCIALS));
        if (data.hasCharacter()) {
            setTopArea(inv, AREA_ID_CARD, clickItem(ChatColor.AQUA + "Carte d'identite", List.of(ChatColor.GRAY + "Recevoir sa carte"), APP_ID_CARD));
        } else {
            setTopArea(inv, AREA_ID_CARD, createCharacterEntryItem());
        }
        setTopArea(inv, AREA_911, clickItem(ChatColor.RED + "911", List.of(ChatColor.GRAY + "Appel d'urgence"), APP_CALL_911));
        setTopArea(inv, AREA_JOB_BOOK, jobBookMenuItem());
        setTopArea(inv, AREA_JOB_TOOLS, clickItem(ChatColor.GREEN + "Outils Metier", List.of(ChatColor.GRAY + "Recuperer les outils du job"), APP_JOB_TOOLS));
        setTopArea(inv, AREA_GPS, clickItem(ChatColor.GREEN + "GPS", List.of(ChatColor.GRAY + "Options GPS"), APP_GPS));
        setTopArea(inv, AREA_ADS, clickItem(ChatColor.YELLOW + "Annonces RP", List.of(ChatColor.GRAY + "Publier une annonce globale"), APP_ADS));
        setTopArea(inv, AREA_CRIME, clickItem(ChatColor.RED + "Criminel", List.of(ChatColor.GRAY + "Infos braquage banque"), APP_CRIME));

        player.openInventory(inv);
    }

    public boolean isPhoneInventory(InventoryHolder holder) {
        return holder instanceof PhoneHolder
                || holder instanceof SocialHolder
                || holder instanceof SmsHolder
                || holder instanceof GpsHolder
                || holder instanceof GpsPlacesHolder
                || holder instanceof GpsPlayersHolder
                || holder instanceof CharacterCreateHolder;
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
            case APP_CHARACTER_CREATE -> {
                openCharacterCreate(player);
                yield true;
            }
            case APP_CHARACTER_SET_FIRST -> {
                player.closeInventory();
                input.request(player, prefix + ChatColor.LIGHT_PURPLE + "Prenom ? (ecris dans le chat)", (p, msg) -> {
                    PendingCharacterCreate pending = pendingCharacterCreate.computeIfAbsent(p.getUniqueId(), k -> new PendingCharacterCreate(null, null));
                    pendingCharacterCreate.put(p.getUniqueId(), new PendingCharacterCreate(msg, pending.lastName()));
                    openCharacterCreate(p);
                });
                yield true;
            }
            case APP_CHARACTER_SET_LAST -> {
                player.closeInventory();
                input.request(player, prefix + ChatColor.LIGHT_PURPLE + "Nom ? (ecris dans le chat)", (p, msg) -> {
                    PendingCharacterCreate pending = pendingCharacterCreate.computeIfAbsent(p.getUniqueId(), k -> new PendingCharacterCreate(null, null));
                    pendingCharacterCreate.put(p.getUniqueId(), new PendingCharacterCreate(pending.firstName(), msg));
                    openCharacterCreate(p);
                });
                yield true;
            }
            case APP_CHARACTER_CONFIRM -> {
                PendingCharacterCreate pending = pendingCharacterCreate.get(player.getUniqueId());
                if (pending == null || pending.firstName() == null || pending.firstName().isBlank() || pending.lastName() == null || pending.lastName().isBlank()) {
                    player.sendMessage(prefix + ChatColor.RED + "Renseigne un prenom et un nom d'abord.");
                    yield true;
                }
                boolean ok = characters.create(player.getUniqueId(), pending.firstName().trim(), pending.lastName().trim());
                if (!ok) {
                    player.sendMessage(prefix + ChatColor.RED + "Personnage deja cree.");
                    yield true;
                }
                pendingCharacterCreate.remove(player.getUniqueId());
                player.sendMessage(prefix + ChatColor.GREEN + "Personnage cree: " + ChatColor.WHITE + pending.firstName().trim() + " " + pending.lastName().trim());
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
                    int maxLen = config.chatRaw().getInt("chat.max_message_length");
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
            case APP_GPS -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                openGpsMenu(player);
                yield true;
            }
            case APP_GPS_PLACES -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                openGpsPlaces(player);
                yield true;
            }
            case APP_GPS_PLAYERS -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                openGpsPlayers(player);
                yield true;
            }
            case APP_GPS_PLACE -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                Place place = placeFromItem(meta);
                if (place == null) {
                    player.sendMessage(prefix + ChatColor.RED + "Lieu invalide.");
                    yield true;
                }
                player.sendMessage(prefix + ChatColor.GREEN + "Lieu: " + ChatColor.YELLOW + place.label());
                player.sendMessage(prefix + ChatColor.GRAY + "Position: " + ChatColor.WHITE
                        + place.world() + " " + place.x() + " " + place.y() + " " + place.z());
                yield true;
            }
            case APP_GPS_SEND -> {
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
                int maxLen = config.chatRaw().getInt("chat.max_message_length");
                String msg = "Ma position: " + player.getWorld().getName() + " "
                        + player.getLocation().getBlockX() + " "
                        + player.getLocation().getBlockY() + " "
                        + player.getLocation().getBlockZ();
                phone.sendSms(player, target, msg, prefix, maxLen);
                yield true;
            }
            case APP_ADS -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                player.closeInventory();
                input.request(player, prefix + ChatColor.YELLOW + "Annonce RP ? (ecris dans le chat)", (p, msg) -> {
                    String rpName = characters.rpNameOrNull(p.getUniqueId());
                    Bukkit.broadcastMessage(ChatColor.GOLD + "[Annonce] " + ChatColor.WHITE
                            + (rpName != null ? rpName : p.getName()) + ChatColor.GRAY + ": " + ChatColor.YELLOW + msg);
                });
                yield true;
            }
            case APP_CRIME -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Cree ton personnage d'abord.");
                    yield true;
                }
                if (bank == null) {
                    player.sendMessage(prefix + ChatColor.RED + "Banque indisponible.");
                    yield true;
                }
                var statuses = bank.robberyStatuses(player);
                if (statuses.isEmpty()) {
                    player.sendMessage(prefix + ChatColor.DARK_GRAY + "Aucune banque configurÃ©e.");
                    yield true;
                }

                player.sendMessage(prefix + ChatColor.RED + "Statut braquage:");
                for (var s : statuses) {
                    String bankName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s.bankName()));
                    boolean cooldown = s.cooldownRemainingMillis() > 0L;
                    boolean empty = s.vaultRemaining() <= 0.0;

                    String state;
                    if (cooldown) {
                        long secs = (s.cooldownRemainingMillis() + 999) / 1000;
                        state = ChatColor.RED + "Cooldown " + ChatColor.WHITE + secs + "s";
                    } else if (empty) {
                        state = ChatColor.DARK_GRAY + "Vide";
                    } else {
                        state = ChatColor.GREEN + "Braquable";
                    }

                    player.sendMessage(prefix + ChatColor.YELLOW + bankName + ChatColor.DARK_GRAY + " Â» "
                            + state
                            + ChatColor.DARK_GRAY + " | Reste: " + ChatColor.GOLD + economy.format(s.vaultRemaining())
                            + ChatColor.DARK_GRAY + " | /clic: " + ChatColor.GOLD + economy.format(s.perClick()));
                }
                yield true;
            }
            default -> false;
        };
    }

    private void openSocials(Player player) {
        Inventory inv = Bukkit.createInventory(new SocialHolder(), 54, socialsTitle());

        setSlots(inv, new int[]{19, 20, 21, 28, 29, 30, 37, 38, 39}, clickItem(
                ChatColor.BLUE + "Tweets",
                List.of(ChatColor.GRAY + "Publier un tweet"),
                APP_TWEET
        ));

        setSlots(inv, new int[]{23, 24, 25, 32, 33, 34, 41, 42, 43}, clickItem(
                ChatColor.AQUA + "SMS",
                List.of(ChatColor.GRAY + "Envoyer un message prive"),
                APP_SMS
        ));

        addBackButton(inv);
        player.openInventory(inv);
    }

    private void openGpsMenu(Player player) {
        Inventory inv = Bukkit.createInventory(new GpsHolder(), 54, gpsMenuTitle());
        inv.setItem(20, clickItem(ChatColor.YELLOW + "Lieux importants", List.of(ChatColor.GRAY + "Afficher les lieux importants"), APP_GPS_PLACES));
        inv.setItem(24, clickItem(ChatColor.AQUA + "Envoyer ma position", List.of(ChatColor.GRAY + "Choisir un joueur"), APP_GPS_PLAYERS));
        addBackButton(inv);
        player.openInventory(inv);
    }

    private void openGpsPlaces(Player player) {
        Inventory inv = Bukkit.createInventory(new GpsPlacesHolder(), 54, gpsPlacesTitle());
        int slot = 0;
        for (Place p : importantPlaces()) {
            if (slot >= inv.getSize() - 1) break;
            ItemStack item = appItemVisible(Material.COMPASS, ChatColor.YELLOW + p.label(), List.of(
                    ChatColor.GRAY + "Monde: " + ChatColor.WHITE + p.world(),
                    ChatColor.GRAY + "Coord: " + ChatColor.WHITE + p.x() + " " + p.y() + " " + p.z(),
                    ChatColor.DARK_GRAY + "Cliquer pour afficher"
            ), APP_GPS_PLACE);

            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(gpsPlaceKey, PersistentDataType.STRING, serializePlace(p));
                item.setItemMeta(meta);
            }
            inv.setItem(slot++, item);
        }
        addBackButton(inv);
        player.openInventory(inv);
    }

    private void openGpsPlayers(Player player) {
        Inventory inv = Bukkit.createInventory(new GpsPlayersHolder(), 54, gpsPlayersTitle());
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (slot >= inv.getSize() - 1) break;
            ItemStack item = appItemVisible(Material.PLAYER_HEAD, ChatColor.AQUA + online.getName(), List.of(
                    ChatColor.GRAY + "Cliquer pour envoyer ta position"
            ), APP_GPS_SEND);

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

    private void openSmsContacts(Player player, String prefix) {
        Inventory inv = Bukkit.createInventory(new SmsHolder(), 54, smsContactsTitle());
        int slot = 0;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (slot >= inv.getSize() - 1) break;
            String rpName = characters.rpNameOrNull(online.getUniqueId());
            if (rpName == null) continue;
            ItemStack item = appItemVisible(Material.PLAYER_HEAD, ChatColor.WHITE + rpName, List.of(ChatColor.GRAY + "Cliquer pour ecrire"), APP_SMS_TO);
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

    private void openCharacterCreate(Player player) {
        Inventory inv = Bukkit.createInventory(new CharacterCreateHolder(), 54, characterCreateTitle());
        PendingCharacterCreate pending = pendingCharacterCreate.get(player.getUniqueId());

        String first = pending != null ? pending.firstName() : null;
        String last = pending != null ? pending.lastName() : null;

        inv.setItem(20, appItemVisible(Material.NAME_TAG, ChatColor.LIGHT_PURPLE + "Prenom", List.of(
                ChatColor.GRAY + "Actuel: " + ChatColor.WHITE + (first != null && !first.isBlank() ? first : "(vide)"),
                ChatColor.DARK_GRAY + "Cliquer pour definir"
        ), APP_CHARACTER_SET_FIRST));
        inv.setItem(24, appItemVisible(Material.NAME_TAG, ChatColor.LIGHT_PURPLE + "Nom", List.of(
                ChatColor.GRAY + "Actuel: " + ChatColor.WHITE + (last != null && !last.isBlank() ? last : "(vide)"),
                ChatColor.DARK_GRAY + "Cliquer pour definir"
        ), APP_CHARACTER_SET_LAST));
        inv.setItem(31, appItemVisible(Material.LIME_CONCRETE, ChatColor.GREEN + "Creer le personnage", List.of(
                ChatColor.GRAY + "Prenom + nom requis",
                ChatColor.DARK_GRAY + "Cliquer pour valider"
        ), APP_CHARACTER_CONFIRM));

        addBackButton(inv);
        player.openInventory(inv);
    }

    private String socialsTitle() {
        return titleFromConfig("phone.titles.socials", null);
    }

    private String smsContactsTitle() {
        return titleFromConfig("phone.titles.sms_contacts", null);
    }

    private String gpsMenuTitle() {
        return titleFromConfig("phone.titles.gps_menu", null);
    }

    private String gpsPlacesTitle() {
        return titleFromConfig("phone.titles.gps_places", null);
    }

    private String gpsPlayersTitle() {
        return titleFromConfig("phone.titles.gps_players", null);
    }

    private String characterCreateTitle() {
        return titleFromConfig("phone.titles.character_create", null);
    }

    private String titleFromConfig(String path, String def) {
        String raw = config.phoneRaw().getString(path);
        return ChatColor.translateAlternateColorCodes('&', raw != null ? raw : "");
    }

    private ItemStack createCharacterEntryItem() {
        String rawName = config.phoneRaw().getString("phone.apps.character_create.item_name");
        String name = ChatColor.translateAlternateColorCodes('&', rawName != null ? rawName : "");
        List<String> lore = config.phoneRaw().getStringList("phone.apps.character_create.item_lore");
        ArrayList<String> translatedLore = new ArrayList<>(lore != null ? lore.size() : 0);
        if (lore != null) {
            for (String line : lore) {
                if (line == null) continue;
                translatedLore.add(ChatColor.translateAlternateColorCodes('&', line));
            }
        }
        return clickItem(name, translatedLore, APP_CHARACTER_CREATE);
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

    private Place placeFromItem(ItemMeta meta) {
        String raw = meta.getPersistentDataContainer().get(gpsPlaceKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) return null;
        return deserializePlace(raw);
    }

    private List<Place> importantPlaces() {
        ArrayList<Place> out = new ArrayList<>();

        ConfigurationSection banks = config.banksRaw().getConfigurationSection("banks.list");
        if (banks != null) {
            for (String id : banks.getKeys(false)) {
                ConfigurationSection b = banks.getConfigurationSection(id);
                if (b == null) continue;
                Place p = placeFromZone(b.getString("name"), b.getConfigurationSection("terminal"));
                if (p != null) out.add(p);
            }
        }

        ConfigurationSection armories = config.armoriesRaw().getConfigurationSection("armories.list");
        if (armories != null) {
            for (String id : armories.getKeys(false)) {
                ConfigurationSection a = armories.getConfigurationSection(id);
                if (a == null) continue;
                Place p = placeFromZone(a.getString("name"), a.getConfigurationSection("terminal"));
                if (p != null) out.add(p);
            }
        }

        ConfigurationSection garages = config.vehiclesRaw().getConfigurationSection("vehicles.garages");
        if (garages != null) {
            for (String id : garages.getKeys(false)) {
                ConfigurationSection g = garages.getConfigurationSection(id);
                if (g == null) continue;
                Place p = placeFromZone(g.getString("name"), g.getConfigurationSection("terminal"));
                if (p != null) out.add(p);
            }
        }

        ConfigurationSection fuels = config.vehiclesRaw().getConfigurationSection("vehicles.fuel_stations.list");
        if (fuels != null) {
            for (String id : fuels.getKeys(false)) {
                ConfigurationSection f = fuels.getConfigurationSection(id);
                if (f == null) continue;
                Place p = placeFromZone(f.getString("name"), f.getConfigurationSection("terminal"));
                if (p != null) out.add(p);
            }
        }

        return out;
    }

    private Place placeFromZone(String label, ConfigurationSection section) {
        if (section == null) return null;
        String world = section.getString("world");
        if (world == null || world.isBlank()) return null;

        double x = readPoint(section, "x");
        double y = readPoint(section, "y");
        double z = readPoint(section, "z");
        String clean = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', label != null ? label : "Lieu"));
        return new Place(clean, world, (int) Math.round(x), (int) Math.round(y), (int) Math.round(z));
    }

    private double readPoint(ConfigurationSection section, String key) {
        if (section.contains(key)) return section.getDouble(key);
        String k1 = key + "1";
        String k2 = key + "2";
        if (section.contains(k1) && section.contains(k2)) {
            return (section.getDouble(k1) + section.getDouble(k2)) / 2.0;
        }
        return 0.0;
    }

    private String serializePlace(Place p) {
        return p.label().replace('|', '/') + "|" + p.world().replace('|', '/') + "|" + p.x() + "|" + p.y() + "|" + p.z();
    }

    private Place deserializePlace(String raw) {
        try {
            String[] parts = raw.split("\\|");
            if (parts.length != 5) return null;
            String label = parts[0];
            String world = parts[1];
            int x = Integer.parseInt(parts[2]);
            int y = Integer.parseInt(parts[3]);
            int z = Integer.parseInt(parts[4]);
            return new Place(label, world, x, y, z);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void giveJobTools(Player player, String prefix) {
        JobType job = jobs.get(player.getUniqueId());
        switch (job) {
            case POLICE -> {
                player.getInventory().addItem(specialItems.create(SpecialItemType.HANDCUFFS));
                player.getInventory().addItem(billing.createTool());
                player.sendMessage(prefix + ChatColor.GREEN + "Kit police donne.");
            }
            case EMS -> {
                player.getInventory().addItem(specialItems.create(SpecialItemType.MEDKIT));
                player.getInventory().addItem(specialItems.create(SpecialItemType.DEFIB));
                player.getInventory().addItem(billing.createTool());
                player.sendMessage(prefix + ChatColor.GREEN + "Kit EMS donne.");
            }
            case TAXI, BAKER, BAR, MECHANIC, STRIP_CLUB, JOURNALIST -> {
                player.getInventory().addItem(billing.createTool());
                player.sendMessage(prefix + ChatColor.GREEN + "Outil facture donne.");
            }
            default -> player.sendMessage(prefix + ChatColor.GRAY + "Aucun outil metier disponible pour ton job.");
        }
    }

    private ItemStack jobBookMenuItem() {
        String name = config.phoneRaw().getString("phone.job_book.item_name");
        List<String> lore = config.phoneRaw().getStringList("phone.job_book.item_lore");
        if (lore == null) lore = List.of();

        ArrayList<String> translatedLore = new ArrayList<>(lore.size());
        for (String line : lore) {
            translatedLore.add(ChatColor.translateAlternateColorCodes('&', line));
        }

        return clickItem(ChatColor.translateAlternateColorCodes('&', name != null ? name : ""), translatedLore, APP_JOB_BOOK);
    }

    private ItemStack createJobInfoBook(Player player) {
        JobType job = jobs.get(player.getUniqueId());

        String author = config.phoneRaw().getString("phone.job_book.author");
        String title = config.phoneRaw().getString("phone.job_book.title");
        double salary = jobs.salary(job);
        long cooldownSeconds = jobs.cooldownSeconds(job);
        String currency = config.currencySymbol();

        List<String> pages = new ArrayList<>();
        pages.add(ChatColor.DARK_BLUE + "" + ChatColor.BOLD + "Metier\n\n"
                + ChatColor.DARK_GRAY + "Job: " + ChatColor.GRAY + jobs.displayName(job) + "\n"
                + ChatColor.DARK_GRAY + "Salaire: " + ChatColor.GRAY + salary + currency + "\n"
                + ChatColor.DARK_GRAY + "Cooldown: " + ChatColor.GRAY + cooldownSeconds + "s\n\n"
                + ChatColor.DARK_GRAY + "Salaire automatique toutes les 30min de jeu.");

        String jobKey = job.name().toLowerCase();
        List<String> extraPages = config.phoneRaw().getStringList("phone.job_book.jobs." + jobKey + ".pages");
        if (extraPages != null) {
            for (String raw : extraPages) {
                if (raw == null || raw.isBlank()) continue;
                pages.add(ChatColor.translateAlternateColorCodes('&', raw));
            }
        }

        if (config.billingRaw().getString("billing.kind_by_job." + jobKey) != null) {
            List<String> billingPages = config.phoneRaw().getStringList("phone.job_book.billing.pages");
            if (billingPages != null) {
                for (String raw : billingPages) {
                    if (raw == null || raw.isBlank()) continue;
                    pages.add(ChatColor.translateAlternateColorCodes('&', raw));
                }
            }
        }

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setAuthor(ChatColor.translateAlternateColorCodes('&', author != null ? author : ""));
            meta.setTitle(ChatColor.translateAlternateColorCodes('&', title != null ? title : ""));
            meta.setPages(pages);
            book.setItemMeta(meta);
        }
        return book;
    }

    private String formatDuration(long millis) {
        long totalSeconds = Math.max(0L, millis / 1000L);
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        if (minutes <= 0) return seconds + "s";
        return minutes + "m" + (seconds < 10 ? "0" : "") + seconds + "s";
    }

    private void setTopArea(Inventory inv, int[] slots, ItemStack item) {
        for (int slot : slots) {
            inv.setItem(slot, item.clone());
        }
    }

    private void setSlots(Inventory inv, int[] slots, ItemStack item) {
        for (int slot : slots) {
            inv.setItem(slot, item.clone());
        }
    }

    private ItemStack appItem(Material material, String name, List<String> lore, String appId) {
        return appItem(material, name, lore, appId, false);
    }

    private ItemStack appItemVisible(Material material, String name, List<String> lore, String appId) {
        return appItem(material, name, lore, appId, false);
    }

    private ItemStack appItem(Material material, String name, List<String> lore, String appId, boolean hiddenModel) {
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

    private ItemStack clickItem(String name, List<String> lore, String appId) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (NEXO_EMPTY_ITEM_MODEL != null) {
                meta.setItemModel(NEXO_EMPTY_ITEM_MODEL);
            }
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
        return config.phoneMenuTitle();
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

    private static final class GpsHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class GpsPlacesHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class GpsPlayersHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private static final class CharacterCreateHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private record PendingCharacterCreate(String firstName, String lastName) {}

    private record Place(String label, String world, int x, int y, int z) {}
}
