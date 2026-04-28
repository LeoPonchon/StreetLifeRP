package org.shimakuro.streetLifeRP.economy;

import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseAction;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseService;
import org.shimakuro.streetLifeRP.core.log.AuditLogService;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;

import java.text.DecimalFormat;
import java.util.UUID;

public final class EconomyService {
    private final PlayerDataRepository repo;
    private final AntiAbuseService antiAbuse;
    private final AuditLogService audit;
    private final String currencySymbol;
    private final DecimalFormat df = new DecimalFormat("#,##0.00");

    public EconomyService(PlayerDataRepository repo, AntiAbuseService antiAbuse, AuditLogService audit, String currencySymbol) {
        this.repo = repo;
        this.antiAbuse = antiAbuse;
        this.audit = audit;
        this.currencySymbol = currencySymbol;
    }

    public String format(double amount) {
        return df.format(amount) + currencySymbol;
    }

    public double cash(UUID uuid) {
        return repo.get(uuid).cash();
    }

    public double bank(UUID uuid) {
        return repo.get(uuid).bank();
    }

    public synchronized boolean deposit(UUID uuid, double amount) {
        if (amount <= 0) return false;
        if (!antiAbuse.allowAndMark(uuid, AntiAbuseAction.BANK_DEPOSIT)) return false;
        PlayerData data = repo.get(uuid);
        if (data.cash() < amount) return false;
        data.setCash(round2(data.cash() - amount));
        data.setBank(round2(data.bank() + amount));
        repo.save(data);
        audit.logSensitive("BANK_DEPOSIT uuid=" + uuid + " amount=" + amount);
        return true;
    }

    public synchronized boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return false;
        if (!antiAbuse.allowAndMark(uuid, AntiAbuseAction.BANK_WITHDRAW)) return false;
        PlayerData data = repo.get(uuid);
        if (data.bank() < amount) return false;
        data.setBank(round2(data.bank() - amount));
        data.setCash(round2(data.cash() + amount));
        repo.save(data);
        audit.logSensitive("BANK_WITHDRAW uuid=" + uuid + " amount=" + amount);
        return true;
    }

    public synchronized boolean transferCash(UUID from, UUID to, double amount) {
        if (amount <= 0) return false;
        if (!antiAbuse.allowAndMark(from, AntiAbuseAction.MONEY_TRANSFER)) return false;
        if (from.equals(to)) return false;

        PlayerData sender = repo.get(from);
        PlayerData receiver = repo.get(to);
        if (sender.cash() < amount) return false;

        sender.setCash(round2(sender.cash() - amount));
        receiver.setCash(round2(receiver.cash() + amount));
        repo.save(sender);
        repo.save(receiver);
        audit.logSensitive("MONEY_TRANSFER from=" + from + " to=" + to + " amount=" + amount);
        return true;
    }

    public synchronized boolean spendCash(UUID uuid, double amount, String reason) {
        if (amount <= 0) return false;
        PlayerData data = repo.get(uuid);
        if (data.cash() < amount) return false;
        data.setCash(round2(data.cash() - amount));
        repo.save(data);
        audit.logSensitive("SPEND_CASH uuid=" + uuid + " amount=" + amount + " reason=" + reason);
        return true;
    }

    public synchronized void addCash(UUID uuid, double amount, String reason) {
        if (amount <= 0) return;
        PlayerData data = repo.get(uuid);
        data.setCash(round2(data.cash() + amount));
        repo.save(data);
        audit.logSensitive("ADD_CASH uuid=" + uuid + " amount=" + amount + " reason=" + reason);
    }

    /**
     * Apply a cash delta (positive or negative).
     * Returns the effective delta applied.
     */
    public synchronized double addCashSigned(UUID uuid, double delta, String reason) {
        if (delta == 0.0) return 0.0;
        PlayerData data = repo.get(uuid);
        double before = data.cash();
        double after = round2(before + delta);
        data.setCash(after);
        repo.save(data);
        double applied = round2(after - before);
        audit.logSensitive("ADD_CASH_SIGNED uuid=" + uuid + " delta=" + delta + " applied=" + applied + " reason=" + reason);
        return applied;
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
