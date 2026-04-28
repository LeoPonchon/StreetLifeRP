package org.shimakuro.streetLifeRP.bank;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.shimakuro.streetLifeRP.core.config.ConfigService;
import org.shimakuro.streetLifeRP.core.log.AuditLogService;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.economy.EconomyService;
import org.shimakuro.streetLifeRP.jobs.JobService;
import org.shimakuro.streetLifeRP.jobs.JobType;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class BankRobberyService implements Runnable {
    private final JavaPlugin plugin;
    private final ConfigService config;
    private final PlayerDataRepository players;
    private final EconomyService economy;
    private final JobService jobs;
    private final AuditLogService audit;

    private final File file;
    private final Map<String, Double> vaults = new HashMap<>();
    private final Map<UUID, PendingLoot> pending = new HashMap<>();
    private final Map<String, BankService.BankDef> bankDefs = new HashMap<>();
    private final Map<String, Map<UUID, Long>> lastRobAtMillis = new HashMap<>();

    private int taskId = -1;

    public BankRobberyService(JavaPlugin plugin, ConfigService config, PlayerDataRepository players, EconomyService economy, JobService jobs, AuditLogService audit) {
        this.plugin = plugin;
        this.config = config;
        this.players = players;
        this.economy = economy;
        this.jobs = jobs;
        this.audit = audit;
        this.file = new File(plugin.getDataFolder(), "banks.yml");
    }

    public void enable() {
        load();
        disable();
        taskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 20L, 20L);
    }

    public void disable() {
        if (taskId != -1) {
            plugin.getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        save();
    }

    public void ensureBanksKnown(Collection<BankService.BankDef> defs) {
        bankDefs.clear();
        for (BankService.BankDef d : defs) {
            bankDefs.put(d.id().toLowerCase(Locale.ROOT), d);
            vaults.putIfAbsent(d.id().toLowerCase(Locale.ROOT), Math.max(0.0, d.vaultInitial()));
        }
        save();
    }

    public double vaultAmount(String bankId) {
        if (bankId == null) return 0.0;
        return vaults.getOrDefault(bankId.toLowerCase(Locale.ROOT), 0.0);
    }

    public void tryRob(Player player, BankService.BankDef bank, String prefix) {
        if (player == null || bank == null) return;
        if (!players.get(player.getUniqueId()).hasCharacter()) {
            player.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
            return;
        }

        String bankId = bank.id().toLowerCase(Locale.ROOT);
        long now = System.currentTimeMillis();
        long cd = clickCooldownMillis();
        long last = lastRobAtMillis.computeIfAbsent(bankId, k -> new HashMap<>()).getOrDefault(player.getUniqueId(), 0L);
        if (last > 0 && now - last < cd) {
            long remaining = (cd - (now - last) + 999) / 1000;
            player.sendMessage(prefix + ChatColor.RED + "Cooldown braquage: " + remaining + "s.");
            return;
        }

        double vault = vaults.getOrDefault(bankId, 0.0);
        if (vault <= 0.0) {
            player.sendMessage(prefix + ChatColor.DARK_GRAY + "Le coffre est vide.");
            return;
        }

        double ratio = takeRatio();
        double initial = Math.max(0.0, bank.vaultInitial());
        double perClick = round2(initial * ratio);
        double take = round2(Math.min(vault, perClick));
        if (take <= 0.0) {
            player.sendMessage(prefix + ChatColor.DARK_GRAY + "Rien à voler.");
            return;
        }

        vault = round2(Math.max(0.0, vault - take));
        vaults.put(bankId, vault);
        lastRobAtMillis.get(bankId).put(player.getUniqueId(), now);

        PendingLoot loot = pending.getOrDefault(player.getUniqueId(), new PendingLoot(0.0, 0L, bankId, bank.name()));
        double nextStolen = round2(loot.amountStolen() + take);
        long expiresAt = now + escapeWindowMillis(nextStolen);
        pending.put(player.getUniqueId(), new PendingLoot(nextStolen, expiresAt, bankId, bank.name()));
        save();

        player.sendMessage(prefix + ChatColor.RED + "Braquage: +" + economy.format(take) + ChatColor.DARK_GRAY + " (en attente)");
        player.sendMessage(prefix + ChatColor.DARK_GRAY + "Coffre restant: " + economy.format(vault));
        alertPolice(bank, player, prefix, take);
        audit.logSensitive("BANK_ROB bank=" + bankId + " robber=" + player.getUniqueId() + " take=" + take + " vault_after=" + vault + " pending=" + nextStolen);
    }

    public void onCuffed(Player target, boolean cuffed, String prefix) {
        if (!cuffed) return;
        PendingLoot loot = pending.remove(target.getUniqueId());
        if (loot == null) return;

        long now = System.currentTimeMillis();
        if (now > loot.expiresAtMillis()) {
            // Too late to restitute.
            pending.put(target.getUniqueId(), loot);
            return;
        }

        String bankId = loot.bankId().toLowerCase(Locale.ROOT);
        double vault = vaults.getOrDefault(bankId, 0.0);
        vaults.put(bankId, round2(vault + loot.amountStolen()));
        save();

        target.sendMessage(prefix + ChatColor.RED + "Arrêté: argent du braquage confisqué et restitué à la banque.");
        audit.logSensitive("BANK_ROB_RESTITUTED bank=" + bankId + " robber=" + target.getUniqueId() + " amount=" + loot.amountStolen());
    }

    public PendingLootView pendingLoot(UUID uuid) {
        if (uuid == null) return null;
        PendingLoot loot = pending.get(uuid);
        if (loot == null) return null;
        long now = System.currentTimeMillis();
        long remaining = Math.max(0L, loot.expiresAtMillis() - now);
        return new PendingLootView(loot.amountStolen(), remaining, loot.bankName());
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();
        if (pending.isEmpty()) return;

        for (Map.Entry<UUID, PendingLoot> e : new HashMap<>(pending).entrySet()) {
            UUID uuid = e.getKey();
            PendingLoot loot = e.getValue();
            if (now < loot.expiresAtMillis()) continue;

            if (players.get(uuid).cuffed()) continue;

            pending.remove(uuid);
            double applied = economy.addCashSigned(uuid, loot.amountStolen(), "bank_robbery_payout:" + loot.bankId());
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(config.prefix() + ChatColor.GREEN + "Braquage réussi: +" + economy.format(applied));
            }
            audit.logSensitive("BANK_ROB_PAYOUT bank=" + loot.bankId() + " robber=" + uuid + " amount=" + loot.amountStolen());
        }

        save();
    }

    private void alertPolice(BankService.BankDef bank, Player robber, String prefix, double take) {
        String bankName = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', bank.name()));
        String msg = prefix + ChatColor.RED + "Braquage en cours: " + ChatColor.YELLOW + bankName
                + ChatColor.GRAY + " @ " + ChatColor.WHITE
                + robber.getWorld().getName() + " " + robber.getLocation().getBlockX() + " " + robber.getLocation().getBlockY() + " " + robber.getLocation().getBlockZ()
                + ChatColor.DARK_GRAY + " (+" + economy.format(take) + ")";

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (jobs.get(p.getUniqueId()) == JobType.POLICE) {
                p.sendMessage(msg);
            }
        }
    }

    private long clickCooldownMillis() {
        long seconds = config.raw().getLong("banks.robbery.click_cooldown_seconds", 20L);
        return Math.max(1L, seconds) * 1000L;
    }

    private double takeRatio() {
        double ratio = config.raw().getDouble("banks.robbery.take_ratio", 0.10);
        if (ratio <= 0.0) return 0.10;
        return Math.min(1.0, ratio);
    }

    private long escapeWindowMillis(double stolenTotal) {
        double per1000 = config.raw().getDouble("banks.robbery.escape_minutes_per_1000_stolen", 1.0);
        int min = config.raw().getInt("banks.robbery.min_escape_minutes", 1);
        int max = config.raw().getInt("banks.robbery.max_escape_minutes", 30);
        int minutes = (int) Math.ceil((stolenTotal / 1000.0) * per1000);
        if (minutes < min) minutes = min;
        if (minutes > max) minutes = max;
        return minutes * 60L * 1000L;
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection v = cfg.getConfigurationSection("vaults");
        if (v != null) {
            for (String key : v.getKeys(false)) {
                vaults.put(key.toLowerCase(Locale.ROOT), Math.max(0.0, v.getDouble(key, 0.0)));
            }
        }
    }

    private void save() {
        //noinspection ResultOfMethodCallIgnored
        plugin.getDataFolder().mkdirs();
        YamlConfiguration cfg = new YamlConfiguration();
        for (Map.Entry<String, Double> e : vaults.entrySet()) {
            cfg.set("vaults." + e.getKey(), e.getValue());
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save banks.yml", e);
        }
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private record PendingLoot(double amountStolen, long expiresAtMillis, String bankId, String bankName) {}

    public record PendingLootView(double amountStolen, long remainingMillis, String bankName) {}
}
