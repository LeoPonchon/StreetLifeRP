package org.shimakuro.streetLifeRP.phone;

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
import org.shimakuro.streetLifeRP.characters.CharacterService;
import org.shimakuro.streetLifeRP.chat.ChatService;
import org.shimakuro.streetLifeRP.core.config.ConfigService;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.economy.CashItemService;
import org.shimakuro.streetLifeRP.identity.IdentityService;
import org.shimakuro.streetLifeRP.jobs.JobService;
import org.shimakuro.streetLifeRP.shops.ShopService;
import org.shimakuro.streetLifeRP.economy.EconomyService;
import org.shimakuro.streetLifeRP.justice.JusticeService;
import org.shimakuro.streetLifeRP.input.InputService;
import org.shimakuro.streetLifeRP.items.SpecialItemService;
import org.shimakuro.streetLifeRP.items.SpecialItemType;
import org.shimakuro.streetLifeRP.jobs.JobType;
import org.shimakuro.streetLifeRP.vehicles.GarageService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PhoneMenuService {
    private static final String APP_NUMBER = "number";
    private static final String APP_GARAGE = "garage";
    private static final String APP_SMS = "sms";
    private static final String APP_SHOP = "shop";
    private static final String APP_WORK = "work";
    private static final String APP_WALLET = "wallet";
    private static final String APP_ID_CARD = "id_card";
    private static final String APP_PAY_FINE = "pay_fine";
    private static final String APP_CREATE_CHARACTER = "create_character";
    private static final String APP_CALL_911 = "call_911";
    private static final String APP_WITHDRAW_CASH = "withdraw_cash";
    private static final String APP_SMS_TO = "sms_to";
    private static final String APP_POLICE_KIT = "police_kit";
    private static final String APP_EMS_KIT = "ems_kit";
    private static final String APP_ADMIN_DELETE_CHAR = "admin_delete_char";
    private static final String APP_ADMIN_SET_TEAM = "admin_set_team";
    private static final String APP_ADMIN_PICK_DELETE_TARGET = "admin_pick_delete_target";
    private static final String APP_ADMIN_PICK_TEAM_TARGET = "admin_pick_team_target";
    private static final String APP_ADMIN_SET_JOB = "admin_set_job";
    private static final String APP_ADMIN_TOGGLE_CUFF = "admin_toggle_cuff";
    private static final String APP_ADMIN_PICK_CUFF_TARGET = "admin_pick_cuff_target";
    private static final String APP_BACK = "back";

    private final JavaPlugin plugin;
    private final ConfigService config;
    private final PlayerDataRepository repo;
    private final EconomyService economy;
    private final JobService jobs;
    private final JusticeService justice;
    private final CharacterService characters;
    private final IdentityService identity;
    private final ShopService shop;
    private final ChatService chat;
    private final PhoneService phone;
    private final InputService input;
    private final CashItemService cashItems;
    private final SpecialItemService specialItems;
    private final GarageService garage;
    private final NamespacedKey appKey;
    private final NamespacedKey uuidKey;
    private final NamespacedKey jobKey;

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
            GarageService garage
    ) {
        this.plugin = plugin;
        this.config = config;
        this.repo = repo;
        this.economy = economy;
        this.jobs = jobs;
        this.justice = justice;
        this.characters = characters;
        this.identity = identity;
        this.shop = shop;
        this.chat = chat;
        this.phone = phone;
        this.input = input;
        this.cashItems = cashItems;
        this.specialItems = specialItems;
        this.garage = garage;
        this.appKey = new NamespacedKey(plugin, "phone_app");
        this.uuidKey = new NamespacedKey(plugin, "phone_uuid");
        this.jobKey = new NamespacedKey(plugin, "phone_job");
    }

    public void open(Player player, String prefix) {
        PlayerData data = repo.get(player.getUniqueId());
        String title = title();
        Inventory inv = Bukkit.createInventory(new PhoneHolder(), 27, title);

        if (player.hasPermission("streetliferp.admin.character.delete")) {
            inv.setItem(0, appItem(Material.BARRIER, ChatColor.RED + "Admin: supprimer perso", List.of(
                    ChatColor.GRAY + "Choisir un joueur"
            ), APP_ADMIN_DELETE_CHAR));
        }
        if (player.hasPermission("streetliferp.admin.cuff.toggle")) {
            inv.setItem(1, appItem(Material.TRIPWIRE_HOOK, ChatColor.AQUA + "Admin: toggle menottes", List.of(
                    ChatColor.GRAY + "Choisir un joueur"
            ), APP_ADMIN_TOGGLE_CUFF));
        }
        if (player.hasPermission("streetliferp.admin.job.set")) {
            inv.setItem(2, appItem(Material.WRITABLE_BOOK, ChatColor.AQUA + "Admin: set team", List.of(
                    ChatColor.GRAY + "Choisir un joueur"
            ), APP_ADMIN_SET_TEAM));
        }

        if (!data.hasCharacter()) {
            inv.setItem(13, appItem(Material.NAME_TAG, ChatColor.GREEN + "Créer personnage", List.of(
                    ChatColor.GRAY + "Choisir prénom/nom"
            ), APP_CREATE_CHARACTER));
            player.openInventory(inv);
            return;
        }

        String num = phone.ensureNumber(player.getUniqueId());
        inv.setItem(4, appItem(Material.PAPER, ChatColor.YELLOW + "Numéro", List.of(ChatColor.GRAY + num), APP_NUMBER));
        inv.setItem(6, appItem(Material.MINECART, ChatColor.YELLOW + "Garage", List.of(ChatColor.GRAY + "Sortir un véhicule (près d'un garage)"), APP_GARAGE));
        inv.setItem(10, appItem(Material.WRITABLE_BOOK, ChatColor.AQUA + "SMS", List.of(ChatColor.GRAY + "Envoyer un message"), APP_SMS));
        inv.setItem(12, appItem(Material.EMERALD, ChatColor.GREEN + "Boutique", List.of(ChatColor.GRAY + "Ouvrir la boutique"), APP_SHOP));
        inv.setItem(14, appItem(Material.GOLD_INGOT, ChatColor.GOLD + "Job", List.of(ChatColor.GRAY + "Travailler / salaire"), APP_WORK));
        inv.setItem(16, appItem(Material.LEATHER, ChatColor.GOLD + "Portefeuille", List.of(ChatColor.GRAY + "Voir cash + banque"), APP_WALLET));
        inv.setItem(20, appItem(Material.PAPER, ChatColor.GOLD + "Retirer cash", List.of(ChatColor.GRAY + "Créer un billet (item)"), APP_WITHDRAW_CASH));
        inv.setItem(22, appItem(Material.NAME_TAG, ChatColor.AQUA + "Identité", List.of(ChatColor.GRAY + "Donner une carte d'identité"), APP_ID_CARD));
        inv.setItem(24, appItem(Material.REDSTONE, ChatColor.RED + "911", List.of(ChatColor.GRAY + "Appel d'urgence"), APP_CALL_911));

        if (data.hasFine()) {
            inv.setItem(26, appItem(Material.SUNFLOWER, ChatColor.RED + "Payer l'amende", List.of(ChatColor.GRAY + "Débite banque puis cash"), APP_PAY_FINE));
        }

        JobType job = jobs.get(player.getUniqueId());
        if (job == JobType.POLICE) {
            inv.setItem(18, appItem(Material.TRIPWIRE_HOOK, ChatColor.AQUA + "Kit Police", List.of(ChatColor.GRAY + "Donne des menottes"), APP_POLICE_KIT));
        }
        if (job == JobType.EMS) {
            inv.setItem(8, appItem(Material.GHAST_TEAR, ChatColor.GREEN + "Kit EMS", List.of(ChatColor.GRAY + "Donne medkit + défib"), APP_EMS_KIT));
        }

        player.openInventory(inv);
    }

    public void openSmsContacts(Player player, String prefix) {
        PlayerData data = repo.get(player.getUniqueId());
        if (!data.hasCharacter()) {
            player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
            return;
        }

        Inventory inv = Bukkit.createInventory(new SmsHolder(), 54, ChatColor.AQUA + "SMS - Contacts");
        int slot = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(player)) continue;
            if (slot >= inv.getSize() - 1) break;
            String rpName = characters.rpNameOrNull(p.getUniqueId());
            if (rpName == null) continue;
            ItemStack it = appItem(Material.PLAYER_HEAD, ChatColor.WHITE + rpName, List.of(ChatColor.GRAY + "Cliquer pour écrire"), APP_SMS_TO);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, p.getUniqueId().toString());
                it.setItemMeta(meta);
            }
            inv.setItem(slot++, it);
        }
        addBackButton(inv);
        player.openInventory(inv);
    }

    public boolean isPhoneInventory(InventoryHolder holder) {
        return holder instanceof PhoneHolder || holder instanceof SmsHolder || holder instanceof AdminPickHolder || holder instanceof AdminJobHolder;
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
            case APP_ADMIN_DELETE_CHAR -> {
                if (!player.hasPermission("streetliferp.admin.character.delete")) yield true;
                player.closeInventory();
                openAdminPickPlayer(player, prefix, APP_ADMIN_PICK_DELETE_TARGET);
                yield true;
            }
            case APP_ADMIN_SET_TEAM -> {
                if (!player.hasPermission("streetliferp.admin.job.set")) yield true;
                player.closeInventory();
                openAdminPickPlayer(player, prefix, APP_ADMIN_PICK_TEAM_TARGET);
                yield true;
            }
            case APP_ADMIN_TOGGLE_CUFF -> {
                if (!player.hasPermission("streetliferp.admin.cuff.toggle")) yield true;
                player.closeInventory();
                openAdminPickPlayer(player, prefix, APP_ADMIN_PICK_CUFF_TARGET);
                yield true;
            }
            case APP_ADMIN_PICK_DELETE_TARGET -> {
                if (!player.hasPermission("streetliferp.admin.character.delete")) yield true;
                String rawUuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
                if (rawUuid == null) yield true;
                UUID targetUuid;
                try {
                    targetUuid = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException e) {
                    yield true;
                }

                Player target = Bukkit.getPlayer(targetUuid);
                if (target == null) {
                    player.sendMessage(prefix + ChatColor.RED + "Joueur hors ligne.");
                    yield true;
                }
                boolean deleted = characters.delete(targetUuid);
                player.sendMessage(prefix + (deleted ? ChatColor.GREEN + "Personnage supprimé." : ChatColor.YELLOW + "Aucun personnage à supprimer."));
                if (deleted) {
                    target.sendMessage(prefix + ChatColor.RED + "Votre personnage a été supprimé par un admin.");
                }
                yield true;
            }
            case APP_ADMIN_PICK_CUFF_TARGET -> {
                if (!player.hasPermission("streetliferp.admin.cuff.toggle")) yield true;
                String rawUuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
                if (rawUuid == null) yield true;
                UUID targetUuid;
                try {
                    targetUuid = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException e) {
                    yield true;
                }
                Player target = Bukkit.getPlayer(targetUuid);
                if (target == null) {
                    player.sendMessage(prefix + ChatColor.RED + "Joueur hors ligne.");
                    yield true;
                }
                boolean next = !justice.isCuffed(targetUuid);
                String actorRp = characters.rpNameOrNull(player.getUniqueId());
                justice.setCuffed(target, next, prefix, actorRp != null ? actorRp : player.getUniqueId().toString());
                String targetRp = characters.rpNameOrNull(targetUuid);
                player.sendMessage(prefix + ChatColor.GREEN + "Menottes " + (next ? "activées" : "retirées") + " pour " + (targetRp != null ? targetRp : target.getName()) + ".");
                player.closeInventory();
                yield true;
            }
            case APP_ADMIN_PICK_TEAM_TARGET -> {
                if (!player.hasPermission("streetliferp.admin.job.set")) yield true;
                String rawUuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
                if (rawUuid == null) yield true;
                UUID targetUuid;
                try {
                    targetUuid = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException e) {
                    yield true;
                }
                Player target = Bukkit.getPlayer(targetUuid);
                if (target == null) {
                    player.sendMessage(prefix + ChatColor.RED + "Joueur hors ligne.");
                    yield true;
                }
                player.closeInventory();
                openAdminPickJob(player, targetUuid);
                yield true;
            }
            case APP_ADMIN_SET_JOB -> {
                if (!player.hasPermission("streetliferp.admin.job.set")) yield true;
                String rawUuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
                String rawJob = meta.getPersistentDataContainer().get(jobKey, PersistentDataType.STRING);
                if (rawUuid == null || rawJob == null) yield true;
                UUID targetUuid;
                try {
                    targetUuid = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException e) {
                    yield true;
                }
                JobType type;
                try {
                    type = JobType.valueOf(rawJob);
                } catch (IllegalArgumentException e) {
                    yield true;
                }
                jobs.set(targetUuid, type);
                Player target = Bukkit.getPlayer(targetUuid);
                player.sendMessage(prefix + ChatColor.GREEN + "Team mise à jour: " + type.name());
                if (target != null) {
                    target.sendMessage(prefix + ChatColor.YELLOW + "Vous êtes maintenant: " + type.name());
                }
                yield true;
            }
            case APP_CREATE_CHARACTER -> {
                player.closeInventory();
                input.request(player, prefix + ChatColor.YELLOW + "Prénom ? (écris dans le chat)", (p, first) -> {
                    String firstName = first.trim();
                    if (firstName.isBlank()) {
                        p.sendMessage(prefix + ChatColor.RED + "Prénom invalide.");
                        return;
                    }
                    input.request(p, prefix + ChatColor.YELLOW + "Nom ? (écris dans le chat)", (p2, last) -> {
                        String lastName = last.trim();
                        if (lastName.isBlank()) {
                            p2.sendMessage(prefix + ChatColor.RED + "Nom invalide.");
                            return;
                        }
                        boolean ok = characters.create(p2.getUniqueId(), firstName, lastName);
                        p2.sendMessage(prefix + (ok ? ChatColor.GREEN + "Personnage créé." : ChatColor.RED + "Personnage déjà créé."));
                    });
                });
                yield true;
            }
            case APP_NUMBER -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                String num = phone.ensureNumber(player.getUniqueId());
                player.sendMessage(prefix + ChatColor.YELLOW + "Numéro: " + ChatColor.WHITE + num);
                yield true;
            }
            case APP_SMS -> {
                player.closeInventory();
                openSmsContacts(player, prefix);
                yield true;
            }
            case APP_SMS_TO -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                String rawUuid = meta.getPersistentDataContainer().get(uuidKey, PersistentDataType.STRING);
                if (rawUuid == null) yield true;
                UUID targetUuid;
                try {
                    targetUuid = UUID.fromString(rawUuid);
                } catch (IllegalArgumentException e) {
                    yield true;
                }
                Player target = Bukkit.getPlayer(targetUuid);
                if (target == null) {
                    player.sendMessage(prefix + ChatColor.RED + "Joueur hors ligne.");
                    yield true;
                }

                player.closeInventory();
                input.request(player, prefix + ChatColor.AQUA + "Message SMS ? (écris dans le chat)", (p, msg) -> {
                    int maxLen = config.raw().getInt("chat.max_message_length", 200);
                    phone.sendSms(p, target, msg, prefix, maxLen);
                });
                yield true;
            }
            case APP_SHOP -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                ConfigurationSection shopSection = config.raw().getConfigurationSection("shop");
                if (shopSection == null) {
                    player.sendMessage(prefix + ChatColor.RED + "Shop non configuré.");
                    yield true;
                }
                player.closeInventory();
                shop.open(player, shopSection, prefix);
                yield true;
            }
            case APP_GARAGE -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                GarageService.Garage g = garage.findGarageNearTerminal(player);
                if (g == null) {
                    player.sendMessage(prefix + ChatColor.RED + "Va à un garage pour utiliser cette app.");
                    yield true;
                }
                player.closeInventory();
                garage.openGarageMenu(player, g, prefix);
                yield true;
            }
            case APP_WORK -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                player.closeInventory();
                JobService.WorkResult res = jobs.work(player.getUniqueId());
                if (res instanceof JobService.WorkResultPaid paid) {
                    double amount = paid.amount();
                    String sign = amount < 0 ? "-" : "+";
                    player.sendMessage(prefix + ChatColor.GREEN + "Travail effectué: " + sign + economy.format(Math.abs(amount)));
                    yield true;
                }
                if (res instanceof JobService.WorkResultCooldown cd) {
                    player.sendMessage(prefix + ChatColor.RED + "Cooldown: " + cd.secondsRemaining() + "s.");
                    yield true;
                }
                player.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
                yield true;
            }
            case APP_WALLET -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                player.sendMessage(prefix + ChatColor.YELLOW + "Cash: " + ChatColor.GOLD + economy.format(data.cash()));
                player.sendMessage(prefix + ChatColor.YELLOW + "Banque: " + ChatColor.GOLD + economy.format(data.bank()));
                if (data.hasFine()) {
                    player.sendMessage(prefix + ChatColor.RED + "Amende: " + ChatColor.GOLD + economy.format(data.fineAmount())
                            + ChatColor.GRAY + " (" + (data.fineReason() != null ? data.fineReason() : "Amende") + ")");
                }
                yield true;
            }
            case APP_WITHDRAW_CASH -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                player.closeInventory();
                input.request(player, prefix + ChatColor.GOLD + "Montant à retirer en billet ? (ex: 50)", (p, raw) -> {
                    Double amount;
                    try {
                        amount = Double.parseDouble(raw.replace(',', '.'));
                    } catch (NumberFormatException e) {
                        p.sendMessage(prefix + ChatColor.RED + "Montant invalide.");
                        return;
                    }
                    if (amount == null || amount <= 0) {
                        p.sendMessage(prefix + ChatColor.RED + "Montant invalide.");
                        return;
                    }
                    if (!economy.spendCash(p.getUniqueId(), amount, "cash_withdraw_item")) {
                        p.sendMessage(prefix + ChatColor.RED + "Fonds insuffisants.");
                        return;
                    }
                    p.getInventory().addItem(cashItems.create(amount, config.currencySymbol()));
                    p.sendMessage(prefix + ChatColor.GREEN + "Billet créé: " + economy.format(amount));
                });
                yield true;
            }
            case APP_ID_CARD -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                player.getInventory().addItem(identity.createIdCard(data.firstName(), data.lastName(), data.idNumber()));
                player.sendMessage(prefix + ChatColor.GREEN + "Carte d'identité ajoutée à l'inventaire.");
                yield true;
            }
            case APP_PAY_FINE -> {
                if (!data.hasFine()) {
                    player.sendMessage(prefix + ChatColor.GRAY + "Aucune amende.");
                    yield true;
                }
                player.closeInventory();
                boolean ok = justice.payFine(player.getUniqueId());
                player.sendMessage(prefix + (ok ? ChatColor.GREEN + "Amende payée." : ChatColor.RED + "Paiement refusé (fonds ou anti-abuse)."));
                yield true;
            }
            case APP_CALL_911 -> {
                if (!data.hasCharacter()) {
                    player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                player.closeInventory();
                input.request(player, prefix + ChatColor.RED + "Message 911 ? (écris dans le chat)", (p, msg) -> chat.sendEmergencyCall(p, msg, prefix));
                yield true;
            }
            case APP_POLICE_KIT -> {
                if (jobs.get(player.getUniqueId()) != JobType.POLICE) yield true;
                player.getInventory().addItem(specialItems.create(SpecialItemType.HANDCUFFS));
                player.sendMessage(prefix + ChatColor.GREEN + "Kit police donné.");
                yield true;
            }
            case APP_EMS_KIT -> {
                if (jobs.get(player.getUniqueId()) != JobType.EMS) yield true;
                player.getInventory().addItem(specialItems.create(SpecialItemType.MEDKIT));
                player.getInventory().addItem(specialItems.create(SpecialItemType.DEFIB));
                player.sendMessage(prefix + ChatColor.GREEN + "Kit EMS donné.");
                yield true;
            }
            default -> false;
        };
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
                ChatColor.GRAY + "Revenir au téléphone"
        ), APP_BACK));
    }

    private String title() {
        ConfigurationSection section = config.raw().getConfigurationSection("phone");
        String raw = section != null ? section.getString("menu.title") : null;
        if (raw == null || raw.isBlank()) raw = "&eTéléphone";
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    private void openAdminPickPlayer(Player player, String prefix, String appId) {
        Inventory inv = Bukkit.createInventory(new AdminPickHolder(appId), 54, ChatColor.DARK_RED + "Admin - Joueurs");
        int slot = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (slot >= inv.getSize() - 1) break;
            String rpName = characters.rpNameOrNull(p.getUniqueId());
            if (rpName == null) continue;
            ItemStack it = appItem(Material.PLAYER_HEAD, ChatColor.WHITE + rpName, List.of(ChatColor.GRAY + "Cliquer"), appId);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, p.getUniqueId().toString());
                it.setItemMeta(meta);
            }
            inv.setItem(slot++, it);
        }
        addBackButton(inv);
        player.openInventory(inv);
        player.sendMessage(prefix + ChatColor.GRAY + "Choisis un joueur.");
    }

    private void openAdminPickJob(Player admin, UUID targetUuid) {
        Player target = Bukkit.getPlayer(targetUuid);
        String rpName = characters.rpNameOrNull(targetUuid);
        String title = ChatColor.AQUA + "Team: " + (rpName != null ? rpName : targetUuid.toString());
        Inventory inv = Bukkit.createInventory(new AdminJobHolder(targetUuid), 27, title);
        int slot = 10;
        for (JobType type : JobType.values()) {
            ItemStack it = appItem(Material.NAME_TAG, ChatColor.YELLOW + type.name(), List.of(ChatColor.GRAY + "Cliquer"), APP_ADMIN_SET_JOB);
            ItemMeta meta = it.getItemMeta();
            if (meta != null) {
                meta.getPersistentDataContainer().set(uuidKey, PersistentDataType.STRING, targetUuid.toString());
                meta.getPersistentDataContainer().set(jobKey, PersistentDataType.STRING, type.name());
                it.setItemMeta(meta);
            }
            inv.setItem(slot++, it);
            if (slot == 17) break;
        }
        addBackButton(inv);
        admin.openInventory(inv);
    }

    private static final class PhoneHolder implements InventoryHolder {
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

    private record AdminPickHolder(String appId) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private record AdminJobHolder(UUID target) implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }
}
