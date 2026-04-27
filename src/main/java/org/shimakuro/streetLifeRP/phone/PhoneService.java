package org.shimakuro.streetLifeRP.phone;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseAction;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseService;
import org.shimakuro.streetLifeRP.core.log.AuditLogService;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.jobs.JobService;
import org.shimakuro.streetLifeRP.jobs.JobType;

import java.util.UUID;

public final class PhoneService {
    private final PlayerDataRepository repo;
    private final JobService jobs;
    private final AntiAbuseService antiAbuse;
    private final AuditLogService auditLog;

    public PhoneService(PlayerDataRepository repo, JobService jobs, AntiAbuseService antiAbuse, AuditLogService auditLog) {
        this.repo = repo;
        this.jobs = jobs;
        this.antiAbuse = antiAbuse;
        this.auditLog = auditLog;
    }

    public String ensureNumber(UUID uuid) {
        PlayerData data = repo.get(uuid);
        if (data.phoneNumber() != null && !data.phoneNumber().isBlank()) {
            return data.phoneNumber();
        }
        String next = generateNumber(uuid);
        data.setPhoneNumber(next);
        repo.save(data);
        return next;
    }

    public String number(UUID uuid) {
        PlayerData data = repo.get(uuid);
        return data.phoneNumber();
    }

    public boolean sendSms(Player sender, Player target, String rawMessage, String prefix, int maxLen) {
        if (!antiAbuse.allowAndMark(sender.getUniqueId(), AntiAbuseAction.PHONE_SMS)) {
            sender.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
            return true;
        }

        String message = sanitize(rawMessage, maxLen);
        if (message.isBlank()) {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /sms <joueur> <message>");
            return true;
        }

        String fromName = rpName(sender);
        String toName = rpName(target);
        String fromNum = ensureNumber(sender.getUniqueId());
        String toNum = ensureNumber(target.getUniqueId());

        String outToTarget = ChatColor.AQUA + "[SMS] " + ChatColor.GRAY + fromName + ChatColor.DARK_GRAY + " (" + fromNum + ")"
                + ChatColor.GRAY + " -> " + ChatColor.WHITE + message;
        String outToSender = ChatColor.AQUA + "[SMS] " + ChatColor.GRAY + fromName + ChatColor.DARK_GRAY + " (" + fromNum + ")"
                + ChatColor.GRAY + " -> " + ChatColor.GRAY + toName + ChatColor.DARK_GRAY + " (" + toNum + ")"
                + ChatColor.GRAY + ": " + ChatColor.WHITE + message;

        sender.sendMessage(outToSender);
        if (!sender.equals(target)) {
            target.sendMessage(outToTarget);
        }

        auditLog.logSensitive("SMS from=" + sender.getUniqueId() + " to=" + target.getUniqueId() + " len=" + message.length());
        return true;
    }

    private String rpName(Player player) {
        PlayerData data = repo.get(player.getUniqueId());
        if (data.hasCharacter()) {
            return data.firstName() + " " + data.lastName();
        }

        JobType job = jobs.get(player.getUniqueId());
        return switch (job) {
            case POLICE, EMS -> job.name();
            default -> player.getName();
        };
    }

    private String sanitize(String raw, int maxLen) {
        if (raw == null) return "";
        String out = raw.replace('\n', ' ').replace('\r', ' ').trim();
        out = out.replace('§', '&');
        out = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', out));
        if (maxLen > 0 && out.length() > maxLen) {
            out = out.substring(0, maxLen);
        }
        return out;
    }

    private String generateNumber(UUID uuid) {
        long mixed = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        int n = Math.floorMod((int) mixed, 100_000_000);
        return "06" + String.format("%08d", n);
    }
}

