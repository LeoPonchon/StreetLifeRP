package org.shimakuro.streetLifeRP.justice;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseAction;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseService;
import org.shimakuro.streetLifeRP.core.log.AuditLogService;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.economy.EconomyService;

import java.util.UUID;

public final class JusticeService {
    private final PlayerDataRepository repo;
    private final AntiAbuseService antiAbuse;
    private final EconomyService economy;
    private final AuditLogService audit;
    private final java.util.function.BiConsumer<Player, Boolean> cuffHook;

    public JusticeService(JavaPlugin plugin, PlayerDataRepository repo, AntiAbuseService antiAbuse, EconomyService economy, AuditLogService audit) {
        this(plugin, repo, antiAbuse, economy, audit, (p, c) -> {});
    }

    public JusticeService(JavaPlugin plugin, PlayerDataRepository repo, AntiAbuseService antiAbuse, EconomyService economy, AuditLogService audit, java.util.function.BiConsumer<Player, Boolean> cuffHook) {
        this.repo = repo;
        this.antiAbuse = antiAbuse;
        this.economy = economy;
        this.audit = audit;
        this.cuffHook = cuffHook != null ? cuffHook : (p, c) -> {};
    }

    public void reloadFromConfig(ConfigurationSection section) {
        // no-op (jail system removed; kept for compatibility with reload calls)
    }

    public void applyStateOnJoin(Player player, String prefix) {
        PlayerData data = repo.get(player.getUniqueId());
        if (data.cuffed()) {
            applyCuffed(player, true, prefix);
        }
    }

    public void setCuffed(Player target, boolean cuffed, String prefix, String actorName) {
        PlayerData data = repo.get(target.getUniqueId());
        data.setCuffed(cuffed);
        repo.save(data);
        applyCuffed(target, cuffed, prefix);
        try {
            cuffHook.accept(target, cuffed);
        } catch (Throwable ignored) {
            // best effort
        }
        audit.logSensitive("CUFF actor=" + actorName + " target=" + target.getUniqueId() + " cuffed=" + cuffed);
    }

    public boolean isCuffed(UUID uuid) {
        return repo.get(uuid).cuffed();
    }

    private void applyCuffed(Player target, boolean cuffed, String prefix) {
        if (cuffed) {
            target.sendMessage(prefix + ChatColor.RED + "Vous êtes menotté.");
        } else {
            target.sendMessage(prefix + ChatColor.GREEN + "Vous n'êtes plus menotté.");
        }
    }

    public void issueFine(UUID target, String issuerName, double amount, String reason) {
        PlayerData data = repo.get(target);
        data.setFineAmount(amount);
        data.setFineIssuer(issuerName);
        data.setFineReason(reason);
        data.setFineIssuedAtMillis(System.currentTimeMillis());
        repo.save(data);
        audit.logSensitive("FINE_ISSUED issuer=" + issuerName + " target=" + target + " amount=" + amount + " reason=" + reason);
    }

    public boolean payFine(UUID uuid) {
        if (!antiAbuse.allowAndMark(uuid, AntiAbuseAction.FINE_PAY)) return false;
        PlayerData data = repo.get(uuid);
        if (!data.hasFine()) return false;

        double amount = data.fineAmount();
        if (!tryTakeMoney(uuid, amount)) return false;

        data.setFineAmount(0.0);
        data.setFineIssuer(null);
        data.setFineReason(null);
        data.setFineIssuedAtMillis(0L);
        repo.save(data);
        audit.logSensitive("FINE_PAID uuid=" + uuid + " amount=" + amount);
        return true;
    }

    private boolean tryTakeMoney(UUID uuid, double amount) {
        PlayerData data = repo.get(uuid);
        if (data.bank() >= amount) {
            data.setBank(Math.round((data.bank() - amount) * 100.0) / 100.0);
            repo.save(data);
            audit.logSensitive("FINE_TAKE bank uuid=" + uuid + " amount=" + amount);
            return true;
        }
        if (data.cash() >= amount) {
            return economy.spendCash(uuid, amount, "fine_pay");
        }
        return false;
    }
}
