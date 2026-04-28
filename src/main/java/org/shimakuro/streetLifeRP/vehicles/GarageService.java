package org.shimakuro.streetLifeRP.vehicles;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
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
import org.shimakuro.streetLifeRP.core.log.AuditLogService;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.economy.EconomyService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GarageService {
    private static final int SLOT_INFO = 4;
    private static final int SLOT_DEALERSHIP = 22;

    private final JavaPlugin plugin;
    private final PlayerDataRepository playerData;
    private final AntiAbuseService antiAbuse;
    private final AuditLogService audit;
    private final EconomyService economy;

    private final NamespacedKey vehicleKey;
    private final NamespacedKey garageKey;
    private final NamespacedKey actionKey;
    private final NamespacedKey backKey;

    private volatile Settings settings = Settings.defaults();

    private enum CommandSenderMode {
        AUTO,
        CONSOLE,
        PLAYER
    }

    public GarageService(JavaPlugin plugin, PlayerDataRepository playerData, AntiAbuseService antiAbuse, EconomyService economy, AuditLogService audit) {
        this.plugin = plugin;
        this.playerData = playerData;
        this.antiAbuse = antiAbuse;
        this.economy = economy;
        this.audit = audit;
        this.vehicleKey = new NamespacedKey(plugin, "garage_vehicle");
        this.garageKey = new NamespacedKey(plugin, "garage_id");
        this.actionKey = new NamespacedKey(plugin, "garage_action");
        this.backKey = new NamespacedKey(plugin, "garage_back");
    }

    public void reloadFromConfig(ConfigurationSection section) {
        settings = Settings.fromConfig(section);
    }

    public Garage findGarageNearTerminal(Player player) {
        Location here = player.getLocation();
        Settings s = settings;
        for (Garage g : s.garages().values()) {
            if (g.terminal().isNear(here)) return g;
        }
        return null;
    }

    public boolean isNearSpawnPoint(Player player, Garage garage) {
        if (garage == null) return false;
        return garage.spawn().isNear(player.getLocation());
    }

    public boolean isGarageInventory(InventoryHolder holder) {
        return holder instanceof GarageHolder || holder instanceof DealerHolder;
    }

    public void openGarageMenu(Player player, Garage garage, String prefix) {
        if (garage == null) {
            player.sendMessage(prefix + ChatColor.RED + "Aucun garage à proximité.");
            return;
        }

        Inventory inv = Bukkit.createInventory(new GarageHolder(garage.id()), 27, garage.title());
        inv.setItem(SLOT_INFO, infoItem(garage));
        inv.setItem(SLOT_DEALERSHIP, dealershipButton(garage));
        inv.setItem(8, backItem("phone", garage));

        List<VehicleDef> vehicles = listPlayerVehicles(player);
        if (vehicles.isEmpty()) {
            inv.setItem(13, emptyItem());
            player.openInventory(inv);
            return;
        }

        int slot = 9;
        for (VehicleDef def : vehicles) {
            if (slot >= inv.getSize()) break;
            inv.setItem(slot++, vehicleItem(def, garage));
        }
        player.openInventory(inv);
    }

    private List<VehicleDef> listPlayerVehicles(Player player) {
        Settings s = settings;
        PlayerData data = playerData.get(player.getUniqueId());
        List<VehicleDef> out = new ArrayList<>();

        if (s.requireOwnership() && data.ownedVehicles().isEmpty() && !s.defaultOwned().isEmpty()) {
            data.setOwnedVehicles(s.defaultOwned());
            playerData.save(data);
        }

        Set<String> owned = Set.copyOf(data.ownedVehicles());
        for (Map.Entry<String, VehicleDef> e : s.catalog().entrySet()) {
            String key = e.getKey();
            VehicleDef def = e.getValue();
            if (s.requireOwnership() && !owned.contains(key)) continue;
            out.add(def);
        }
        return out;
    }

    public boolean handleGarageMenuClick(Player player, ItemStack clicked, String prefix) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return false;

        String action = meta.getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
        if ("open_dealership".equals(action)) {
            String garageId = meta.getPersistentDataContainer().get(garageKey, PersistentDataType.STRING);
            if (garageId == null) return true;
            Garage garage = settings.garages().get(garageId);
            if (garage == null) return true;
            openDealershipMenu(player, garage, prefix);
            return true;
        }

        return trySpawnFromMenu(player, clicked, prefix);
    }

    public void openDealershipMenu(Player player, Garage garage, String prefix) {
        Inventory inv = Bukkit.createInventory(new DealerHolder(garage.id()), 54, ChatColor.GOLD + "Concession - " + garage.plainName());
        inv.setItem(SLOT_INFO, infoItem(garage));
        inv.setItem(8, backItem("garage", garage));

        Settings s = settings;
        PlayerData data = playerData.get(player.getUniqueId());
        Set<String> owned = Set.copyOf(data.ownedVehicles());

        int slot = 9;
        for (Map.Entry<String, VehicleDef> e : s.catalog().entrySet()) {
            if (slot >= inv.getSize()) break;
            String key = e.getKey();
            VehicleDef def = e.getValue();
            boolean has = owned.contains(key);
            inv.setItem(slot++, dealershipItem(def, garage, has));
        }

        player.openInventory(inv);
        player.sendMessage(prefix + ChatColor.GRAY + "Concession ouverte.");
    }

    public boolean tryBuyFromMenu(Player player, ItemStack clicked, String prefix) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return false;

        String vehicleKeyRaw = meta.getPersistentDataContainer().get(vehicleKey, PersistentDataType.STRING);
        if (vehicleKeyRaw == null || vehicleKeyRaw.isBlank()) return false;

        String key = vehicleKeyRaw.toLowerCase(Locale.ROOT);
        VehicleDef def = settings.catalog().get(key);
        if (def == null) {
            player.sendMessage(prefix + ChatColor.RED + "Véhicule invalide.");
            return true;
        }

        if (!antiAbuse.allowAndMark(player.getUniqueId(), AntiAbuseAction.VEHICLE_BUY)) {
            player.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
            return true;
        }

        PlayerData data = playerData.get(player.getUniqueId());
        if (data.ownedVehicles().contains(key)) {
            player.sendMessage(prefix + ChatColor.YELLOW + "Tu possèdes déjà ce véhicule.");
            return true;
        }

        double price = def.price();
        if (price <= 0) {
            player.sendMessage(prefix + ChatColor.RED + "Véhicule non achetable (prix).");
            return true;
        }

        if (!economy.spendCash(player.getUniqueId(), price, "vehicle_buy:" + key)) {
            player.sendMessage(prefix + ChatColor.RED + "Fonds insuffisants.");
            return true;
        }

        List<String> next = new ArrayList<>(data.ownedVehicles());
        next.add(key);
        data.setOwnedVehicles(next);
        playerData.save(data);

        player.sendMessage(prefix + ChatColor.GREEN + "Achat effectué: " + ChatColor.WHITE + ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', def.displayName())));
        audit.logSensitive("VEHICLE_BUY uuid=" + player.getUniqueId() + " vehicle=" + key + " price=" + price);
        return true;
    }

    public boolean isBackButton(ItemStack clicked) {
        if (clicked == null || clicked.getType().isAir()) return false;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return false;
        String back = meta.getPersistentDataContainer().get(backKey, PersistentDataType.STRING);
        return back != null && !back.isBlank();
    }

    public String backTarget(ItemStack clicked) {
        if (clicked == null || clicked.getType().isAir()) return null;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(backKey, PersistentDataType.STRING);
    }

    public Garage garageFromItem(ItemStack clicked) {
        if (clicked == null || clicked.getType().isAir()) return null;
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return null;
        String garageId = meta.getPersistentDataContainer().get(garageKey, PersistentDataType.STRING);
        if (garageId == null) return null;
        return settings.garages().get(garageId);
    }

    private boolean trySpawnFromMenu(Player player, ItemStack clicked, String prefix) {
        ItemMeta meta = clicked.getItemMeta();
        if (meta == null) return false;

        String vehicleKeyRaw = meta.getPersistentDataContainer().get(vehicleKey, PersistentDataType.STRING);
        String garageId = meta.getPersistentDataContainer().get(garageKey, PersistentDataType.STRING);
        if (vehicleKeyRaw == null || garageId == null) return false;

        Settings s = settings;
        Garage garage = s.garages().get(garageId);
        if (garage == null) {
            player.sendMessage(prefix + ChatColor.RED + "Garage invalide.");
            return true;
        }
        String key = vehicleKeyRaw.toLowerCase(Locale.ROOT);
        VehicleDef def = s.catalog().get(key);
        if (def == null) {
            player.sendMessage(prefix + ChatColor.RED + "Véhicule invalide.");
            return true;
        }

        if (!antiAbuse.allowAndMark(player.getUniqueId(), AntiAbuseAction.VEHICLE_SPAWN)) {
            player.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
            return true;
        }

        if (s.requireOwnership()) {
            PlayerData data = playerData.get(player.getUniqueId());
            if (!data.ownedVehicles().contains(key)) {
                player.sendMessage(prefix + ChatColor.RED + "Tu ne possèdes pas ce véhicule.");
                return true;
            }
        }

        Location spawn = garage.spawn().toLocation();
        if (spawn == null) {
            player.sendMessage(prefix + ChatColor.RED + "Spawn garage non configuré.");
            return true;
        }

        despawnExistingQavVehicles(player);

        String template = s.spawnCommandTemplate();
        String cmd = renderCommand(template, def.providerId(), player, spawn);
        if (cmd == null || cmd.isBlank()) {
            player.sendMessage(prefix + ChatColor.RED + "Commande de spawn non configurée.");
            return true;
        }

        boolean ok = trySpawnQavDirect(template, def.providerId(), player, spawn);
        if (!ok) {
            ok = dispatchSpawnCommandAt(s.spawnCommandSender(), template, cmd, player, spawn);
            if (ok) rememberNewestOwnedVehicle(player);
        }
        player.sendMessage(prefix + (ok ? ChatColor.GREEN + "Véhicule sorti." : ChatColor.RED + "Échec du spawn véhicule (commande)."));
        audit.logSensitive("VEHICLE_SPAWN uuid=" + player.getUniqueId() + " vehicle=" + key + " provider_id=" + def.providerId() + " garage=" + garage.id());
        return true;
    }

    private boolean trySpawnQavDirect(String template, String vehicleProviderId, Player player, Location spawn) {
        if (template == null) return false;
        String normalized = template.trim().toLowerCase(Locale.ROOT);
        if (!normalized.equals("qav spawnvehicle %vehicle%")
                && !normalized.equals("qualityarmoryvehicles spawnvehicle %vehicle%")
                && !normalized.equals("qualityarmoryvehicles2 spawnvehicle %vehicle%")) {
            return false;
        }

        try {
            Class<?> api = Class.forName("me.zombie_striker.qav.api.QualityArmoryVehicles");
            Class<?> abstractVehicle = Class.forName("me.zombie_striker.qav.vehicles.AbstractVehicle");
            java.lang.reflect.Method getVehicle = api.getMethod("getVehicle", String.class);
            Object vehicleType = getVehicle.invoke(null, vehicleProviderId);
            if (vehicleType == null) return false;

            java.lang.reflect.Method spawnVehicle = api.getMethod("spawnVehicle", abstractVehicle, Location.class, Player.class);
            Object vehicle = spawnVehicle.invoke(null, vehicleType, spawn, player);
            if (vehicle == null) return false;
            setVehicleOwner(vehicle, player.getUniqueId());
            fillFuel(vehicle);
            rememberActiveVehicle(player, vehicle);
            return true;
        } catch (Throwable e) {
            plugin.getLogger().fine("QAV direct spawn unavailable: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            return false;
        }
    }

    private boolean dispatchSpawnCommand(CommandSenderMode mode, String template, String rawCmd, Player player) {
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

        if (!ok) {
            plugin.getLogger().warning("Vehicle spawn command failed: sender=" + effective + " cmd=\"" + cmd + "\"");
        }
        return ok;
    }

    private boolean dispatchSpawnCommandAt(CommandSenderMode mode, String template, String rawCmd, Player player, Location spawn) {
        if (usesLocationPlaceholders(template)) {
            return dispatchSpawnCommand(mode, template, rawCmd, player);
        }

        player.teleport(spawn);
        return dispatchSpawnCommand(mode, template, rawCmd, player);
    }

    private boolean usesLocationPlaceholders(String template) {
        if (template == null) return false;
        return template.contains("%world%")
                || template.contains("%x%")
                || template.contains("%y%")
                || template.contains("%z%");
    }

    private void despawnExistingQavVehicles(Player player) {
        PlayerData data = playerData.get(player.getUniqueId());
        String activeVehicleUuid = data.activeVehicleUuid();
        boolean removedAny = false;

        try {
            Class<?> api = Class.forName("me.zombie_striker.qav.api.QualityArmoryVehicles");
            for (Object v : findQavVehiclesForPlayer(api, player.getUniqueId(), activeVehicleUuid)) {
                if (v == null) continue;
                try {
                    java.lang.reflect.Method isInvalid = v.getClass().getMethod("isInvalid");
                    Object inv = isInvalid.invoke(v);
                    if (inv instanceof Boolean b && b) continue;
                } catch (Throwable ignored) {
                    // best effort
                }

                try {
                    java.lang.reflect.Method deconstruct = v.getClass().getMethod("deconstruct", Player.class, String.class);
                    deconstruct.invoke(v, player, "StreetLifeRP:spawn_replace");
                    removedAny = true;
                } catch (NoSuchMethodException e) {
                    try {
                        java.lang.reflect.Method deconstruct = v.getClass().getMethod("deconstruct", Player.class, String.class, boolean.class);
                        deconstruct.invoke(v, player, "StreetLifeRP:spawn_replace", true);
                        removedAny = true;
                    } catch (Throwable ignored2) {
                        // ignore
                    }
                } catch (Throwable ignored) {
                    // ignore
                }
            }
        } catch (Throwable ignored) {
            // QAV not installed or API changed; ignore
        }

        if (removedAny || activeVehicleUuid != null) {
            data.setActiveVehicleUuid(null);
            playerData.save(data);
        }
    }

    private List<Object> findQavVehiclesForPlayer(Class<?> api, java.util.UUID playerUuid, String activeVehicleUuid) {
        ArrayList<Object> out = new ArrayList<>();
        try {
            java.lang.reflect.Method getOwned = api.getMethod("getOwnedVehicles", java.util.UUID.class);
            Object list = getOwned.invoke(null, playerUuid);
            if (list instanceof java.util.List<?> vehicles) {
                out.addAll(vehicles);
            }
        } catch (Throwable ignored) {
            // best effort
        }

        try {
            Class<?> main = Class.forName("me.zombie_striker.qav.Main");
            java.lang.reflect.Field vehiclesField = main.getField("vehicles");
            Object list = vehiclesField.get(null);
            if (list instanceof java.util.List<?> vehicles) {
                for (Object vehicle : vehicles) {
                    if (belongsToPlayer(vehicle, playerUuid, activeVehicleUuid) && !out.contains(vehicle)) {
                        out.add(vehicle);
                    }
                }
            }
        } catch (Throwable ignored) {
            // best effort
        }
        return out;
    }

    private boolean belongsToPlayer(Object vehicle, java.util.UUID playerUuid, String activeVehicleUuid) {
        if (vehicle == null) return false;
        java.util.UUID vehicleUuid = vehicleUuid(vehicle);
        if (activeVehicleUuid != null && vehicleUuid != null && activeVehicleUuid.equals(vehicleUuid.toString())) return true;

        try {
            java.lang.reflect.Method getOwner = vehicle.getClass().getMethod("getOwner");
            Object owner = getOwner.invoke(vehicle);
            if (playerUuid.equals(owner)) return true;
        } catch (Throwable ignored) {
            // best effort
        }

        try {
            java.lang.reflect.Method getWhiteList = vehicle.getClass().getMethod("getWhiteList");
            Object whitelist = getWhiteList.invoke(vehicle);
            if (whitelist instanceof java.util.Collection<?> uuids && uuids.contains(playerUuid)) return true;
        } catch (Throwable ignored) {
            // best effort
        }

        return false;
    }

    private void setVehicleOwner(Object vehicle, java.util.UUID owner) {
        try {
            java.lang.reflect.Method setOwner = vehicle.getClass().getMethod("setOwner", java.util.UUID.class);
            setOwner.invoke(vehicle, owner);
        } catch (Throwable ignored) {
            // best effort
        }
    }

    private java.util.UUID vehicleUuid(Object vehicle) {
        try {
            java.lang.reflect.Method getVehicleUUID = vehicle.getClass().getMethod("getVehicleUUID");
            Object uuid = getVehicleUUID.invoke(vehicle);
            if (uuid instanceof java.util.UUID u) return u;
        } catch (Throwable ignored) {
            // best effort
        }
        return null;
    }

    private void rememberActiveVehicle(Player player, Object vehicle) {
        java.util.UUID vehicleUuid = vehicleUuid(vehicle);
        if (vehicleUuid == null) return;
        PlayerData data = playerData.get(player.getUniqueId());
        data.setActiveVehicleUuid(vehicleUuid.toString());
        playerData.save(data);
    }

    private void rememberNewestOwnedVehicle(Player player) {
        try {
            Class<?> api = Class.forName("me.zombie_striker.qav.api.QualityArmoryVehicles");
            java.lang.reflect.Method getOwned = api.getMethod("getOwnedVehicles", java.util.UUID.class);
            Object list = getOwned.invoke(null, player.getUniqueId());
            if (!(list instanceof java.util.List<?> vehicles) || vehicles.isEmpty()) return;
            Object vehicle = vehicles.get(vehicles.size() - 1);
            fillFuel(vehicle);
            rememberActiveVehicle(player, vehicle);
        } catch (Throwable ignored) {
            // best effort
        }
    }

    private void fillFuel(Object vehicleEntity) {
        if (vehicleEntity == null) return;
        int max = settings.fuelMax();
        if (max <= 0) return;
        try {
            java.lang.reflect.Method setFuel = vehicleEntity.getClass().getMethod("setFuel", int.class);
            setFuel.invoke(vehicleEntity, max);
        } catch (Throwable ignored) {
            // best effort
        }
    }

    private boolean dispatchAs(CommandSenderMode mode, String cmd, Player player) {
        if (mode == CommandSenderMode.PLAYER) {
            return player.performCommand(cmd);
        }
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
    }

    private String renderCommand(String template, String vehicleProviderId, Player player, Location loc) {
        if (template == null) return null;
        String world = (loc.getWorld() != null) ? loc.getWorld().getName() : "world";
        String cmd = template;
        cmd = cmd.replace("%vehicle%", Objects.toString(vehicleProviderId, ""));
        cmd = cmd.replace("%player%", player.getName());
        cmd = cmd.replace("%world%", world);
        cmd = cmd.replace("%x%", Integer.toString(loc.getBlockX()));
        cmd = cmd.replace("%y%", Integer.toString(loc.getBlockY()));
        cmd = cmd.replace("%z%", Integer.toString(loc.getBlockZ()));
        return cmd.trim();
    }

    private ItemStack vehicleItem(VehicleDef def, Garage garage) {
        ItemStack it = qavVehicleIcon(def.providerId(), def.icon());
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', def.displayName()));
            meta.setLore(List.of(
                    ChatColor.GRAY + "Garage: " + ChatColor.WHITE + garage.plainName(),
                    ChatColor.DARK_GRAY + "Clique pour sortir le véhicule."
            ));
            meta.getPersistentDataContainer().set(vehicleKey, PersistentDataType.STRING, def.key());
            meta.getPersistentDataContainer().set(garageKey, PersistentDataType.STRING, garage.id());
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack dealershipButton(Garage garage) {
        ItemStack it = new ItemStack(Material.EMERALD);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + "Concession");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Acheter un véhicule",
                    ChatColor.DARK_GRAY + "Clique pour ouvrir"
            ));
            meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "open_dealership");
            meta.getPersistentDataContainer().set(garageKey, PersistentDataType.STRING, garage.id());
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack dealershipItem(VehicleDef def, Garage garage, boolean owned) {
        ItemStack it = qavVehicleIcon(def.providerId(), def.icon());
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', def.displayName()));
            ArrayList<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Garage: " + ChatColor.WHITE + garage.plainName());
            if (owned) {
                lore.add(ChatColor.GREEN + "Déjà possédé");
            } else {
                lore.add(ChatColor.GRAY + "Prix: " + ChatColor.GOLD + economy.format(def.price()));
                lore.add(ChatColor.DARK_GRAY + "Clique pour acheter");
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(vehicleKey, PersistentDataType.STRING, def.key());
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack backItem(String target, Garage garage) {
        ItemStack it = new ItemStack(Material.ARROW);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Retour");
            meta.setLore(List.of(ChatColor.GRAY + ("garage".equals(target) ? "Revenir au garage" : "Revenir au téléphone")));
            meta.getPersistentDataContainer().set(backKey, PersistentDataType.STRING, target);
            meta.getPersistentDataContainer().set(garageKey, PersistentDataType.STRING, garage.id());
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack qavVehicleIcon(String providerId, Material fallback) {
        try {
            Class<?> api = Class.forName("me.zombie_striker.qav.api.QualityArmoryVehicles");
            java.lang.reflect.Method getVehicle = api.getMethod("getVehicle", String.class);
            Object vehicle = getVehicle.invoke(null, providerId);
            if (vehicle == null) return new ItemStack(fallback);

            Class<?> abstractVehicle = Class.forName("me.zombie_striker.qav.vehicles.AbstractVehicle");
            java.lang.reflect.Method getItem = api.getMethod("getVehicleItemStack", abstractVehicle);
            Object out = getItem.invoke(null, vehicle);
            if (out instanceof ItemStack stack && !stack.getType().isAir()) {
                return stack;
            }
        } catch (Throwable ignored) {
            // ignore
        }
        return new ItemStack(fallback);
    }

    private ItemStack emptyItem() {
        ItemStack it = new ItemStack(Material.BARRIER);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.RED + "Aucun véhicule");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Ajoute des véhicules au joueur",
                    ChatColor.GRAY + "ou désactive require_ownership."
            ));
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack infoItem(Garage garage) {
        ItemStack it = new ItemStack(Material.MAP);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Garage");
            meta.setLore(List.of(
                    ChatColor.GRAY + "Point terminal: " + ChatColor.WHITE + garage.terminal().pretty(),
                    ChatColor.GRAY + "Point sortie: " + ChatColor.WHITE + garage.spawn().pretty()
            ));
            it.setItemMeta(meta);
        }
        return it;
    }

    public record Garage(String id, String name, Zone terminal, Zone spawn) {
        public String plainName() {
            return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', name));
        }

        public String title() {
            String raw = name;
            if (raw == null || raw.isBlank()) raw = "&eGarage";
            return ChatColor.translateAlternateColorCodes('&', raw);
        }
    }

    public record Zone(String world, double x, double y, double z, double radius, float yaw, float pitch) {
        public boolean isNear(Location loc) {
            if (loc == null || loc.getWorld() == null || world == null) return false;
            if (!loc.getWorld().getName().equals(world)) return false;
            if (radius <= 0) return false;
            double dx = loc.getX() - x;
            double dy = loc.getY() - y;
            double dz = loc.getZ() - z;
            return (dx * dx + dy * dy + dz * dz) <= (radius * radius);
        }

        public Location toLocation() {
            if (world == null || world.isBlank()) return null;
            World w = Bukkit.getWorld(world);
            if (w == null) return null;
            return new Location(w, x, y, z, yaw, pitch);
        }

        public String pretty() {
            return world + " " + Math.round(x) + " " + Math.round(y) + " " + Math.round(z);
        }
    }

    public record VehicleDef(String key, String displayName, String providerId, double price, Material icon) {}

    public static final class GarageHolder implements InventoryHolder {
        private final String garageId;

        public GarageHolder(String garageId) {
            this.garageId = garageId;
        }

        public String garageId() {
            return garageId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static final class DealerHolder implements InventoryHolder {
        private final String garageId;

        public DealerHolder(String garageId) {
            this.garageId = garageId;
        }

        public String garageId() {
            return garageId;
        }

        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    private record Settings(
            boolean requireOwnership,
            String spawnCommandTemplate,
            CommandSenderMode spawnCommandSender,
            int fuelMax,
            List<String> defaultOwned,
            Map<String, Garage> garages,
            Map<String, VehicleDef> catalog
    ) {
        static Settings defaults() {
            return new Settings(
                    true,
                    "qav spawnVehicle %vehicle%",
                    CommandSenderMode.AUTO,
                    10000,
                    List.of(),
                    Map.of(),
                    Map.of()
            );
        }

        static Settings fromConfig(ConfigurationSection section) {
            Settings d = defaults();
            if (section == null) return d;

            boolean requireOwnership = section.getBoolean("require_ownership", d.requireOwnership());
            String spawnCommand = section.getString("provider.spawn_command", d.spawnCommandTemplate());
            CommandSenderMode sender = parseSenderMode(section.getString("provider.sender", "auto"));
            int fuelMax = section.getInt("fuel.max", d.fuelMax());
            List<String> defaultOwned = normalizeList(section.getStringList("default_owned"));

            Map<String, Garage> garages = new HashMap<>();
            ConfigurationSection garagesSection = section.getConfigurationSection("garages");
            if (garagesSection != null) {
                for (String id : garagesSection.getKeys(false)) {
                    ConfigurationSection g = garagesSection.getConfigurationSection(id);
                    if (g == null) continue;
                    String name = g.getString("name", "&eGarage");
                    Zone terminal = readZone(g.getConfigurationSection("terminal"));
                    Zone spawn = readZone(g.getConfigurationSection("spawn"));
                    if (terminal == null || spawn == null) continue;
                    garages.put(id, new Garage(id, name, terminal, spawn));
                }
            }

            Map<String, VehicleDef> catalog = new HashMap<>();
            ConfigurationSection catalogSection = section.getConfigurationSection("catalog");
            if (catalogSection != null) {
                for (String key : catalogSection.getKeys(false)) {
                    ConfigurationSection v = catalogSection.getConfigurationSection(key);
                    if (v == null) continue;
                    String displayName = v.getString("name", key);
                    String providerId = v.getString("vehicle_id", key);
                    double price = v.getDouble("price", 0.0);
                    String iconName = v.getString("icon", "MINECART");
                    Material icon;
                    try {
                        icon = Material.valueOf(iconName.toUpperCase(Locale.ROOT));
                    } catch (IllegalArgumentException e) {
                        icon = Material.MINECART;
                    }
                    catalog.put(key.toLowerCase(Locale.ROOT), new VehicleDef(key.toLowerCase(Locale.ROOT), displayName, providerId, price, icon));
                }
            }

            return new Settings(requireOwnership, spawnCommand, sender, fuelMax, defaultOwned, Map.copyOf(garages), Map.copyOf(catalog));
        }

        private static CommandSenderMode parseSenderMode(String raw) {
            if (raw == null || raw.isBlank()) return CommandSenderMode.AUTO;
            String v = raw.trim().toLowerCase(Locale.ROOT);
            return switch (v) {
                case "console", "server" -> CommandSenderMode.CONSOLE;
                case "player" -> CommandSenderMode.PLAYER;
                default -> CommandSenderMode.AUTO;
            };
        }

        private static List<String> normalizeList(List<String> raw) {
            if (raw == null || raw.isEmpty()) return List.of();
            ArrayList<String> out = new ArrayList<>();
            for (String s : raw) {
                if (s == null) continue;
                String v = s.trim().toLowerCase(Locale.ROOT);
                if (v.isBlank()) continue;
                out.add(v);
            }
            return List.copyOf(out);
        }

        private static Zone readZone(ConfigurationSection section) {
            if (section == null) return null;
            String world = section.getString("world");
            double x = section.getDouble("x", 0.0);
            double y = section.getDouble("y", 0.0);
            double z = section.getDouble("z", 0.0);
            double radius = section.getDouble("radius", 3.5);
            float yaw = (float) section.getDouble("yaw", 0.0);
            float pitch = (float) section.getDouble("pitch", 0.0);
            if (world == null || world.isBlank()) return null;
            return new Zone(world, x, y, z, Math.max(0.5, radius), yaw, pitch);
        }
    }
}
