package org.shimakuro.streetLifeRP.chat;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseAction;
import org.shimakuro.streetLifeRP.antiabuse.AntiAbuseService;
import org.shimakuro.streetLifeRP.core.log.AuditLogService;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;
import org.shimakuro.streetLifeRP.jobs.JobService;
import org.shimakuro.streetLifeRP.jobs.JobType;

public final class ChatService {
    private final PlayerDataRepository playerData;
    private final JobService jobs;
    private final AntiAbuseService antiAbuse;
    private final AuditLogService auditLog;

    private volatile Settings settings = Settings.defaults();

    public ChatService(PlayerDataRepository playerData, JobService jobs, AntiAbuseService antiAbuse, AuditLogService auditLog) {
        this.playerData = playerData;
        this.jobs = jobs;
        this.antiAbuse = antiAbuse;
        this.auditLog = auditLog;
    }

    public void reloadFromConfig(ConfigurationSection section) {
        Settings next = Settings.fromConfig(section);
        settings = next;
    }

    public boolean proximityEnabled() {
        return settings.proximityEnabled();
    }

    public void sendLocalChat(Player sender, String rawMessage, String prefix) {
        String message = sanitize(rawMessage);
        if (message.isBlank()) return;

        Settings s = settings;
        String formatted = format(s.formatLocal(), sender, message, prefix);
        if (formatted == null) return;
        broadcastLocal(sender, s.proximityRadiusBlocks(), formatted);
    }

    public boolean sendMe(Player sender, String rawMessage, String prefix) {
        if (!antiAbuse.allowAndMark(sender.getUniqueId(), AntiAbuseAction.CHAT_ME)) {
            sender.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
            return true;
        }
        String message = sanitize(rawMessage);
        if (message.isBlank()) {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /me <action>");
            return true;
        }

        Settings s = settings;
        String formatted = format(s.formatMe(), sender, message, prefix);
        if (formatted == null) return true;
        broadcastLocal(sender, s.meRadiusBlocks(), formatted);
        return true;
    }

    public boolean sendDo(Player sender, String rawMessage, String prefix) {
        if (!antiAbuse.allowAndMark(sender.getUniqueId(), AntiAbuseAction.CHAT_DO)) {
            sender.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
            return true;
        }
        String message = sanitize(rawMessage);
        if (message.isBlank()) {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /do <description>");
            return true;
        }

        Settings s = settings;
        String formatted = format(s.formatDo(), sender, message, prefix);
        if (formatted == null) return true;
        broadcastLocal(sender, s.doRadiusBlocks(), formatted);
        return true;
    }

    public boolean sendOoc(Player sender, String rawMessage, String prefix) {
        if (!antiAbuse.allowAndMark(sender.getUniqueId(), AntiAbuseAction.CHAT_OOC)) {
            sender.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
            return true;
        }
        String message = sanitize(rawMessage);
        if (message.isBlank()) {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /ooc <message>");
            return true;
        }

        Settings s = settings;
        String formatted = format(s.formatOoc(), sender, message, prefix);
        if (formatted == null) return true;
        broadcastLocal(sender, s.oocRadiusBlocks(), formatted);
        return true;
    }

    public boolean sendTweet(Player sender, String rawMessage, String prefix) {
        if (!antiAbuse.allowAndMark(sender.getUniqueId(), AntiAbuseAction.CHAT_TWEET)) {
            sender.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
            return true;
        }
        String message = sanitize(rawMessage);
        if (message.isBlank()) {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /twt <message>");
            return true;
        }

        Settings s = settings;
        String formatted = format(s.formatTweet(), sender, message, prefix);
        if (formatted == null) return true;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendMessage(formatted);
        }
        return true;
    }

    public boolean sendEmergencyCall(Player sender, String rawMessage, String prefix) {
        if (!antiAbuse.allowAndMark(sender.getUniqueId(), AntiAbuseAction.EMERGENCY_CALL)) {
            sender.sendMessage(prefix + ChatColor.RED + "Action trop rapide.");
            return true;
        }
        String message = sanitize(rawMessage);
        if (message.isBlank()) {
            sender.sendMessage(prefix + ChatColor.GRAY + "Usage: /911 <message>");
            return true;
        }

        Settings s = settings;
        String formatted = format(s.formatEmergency(), sender, message, prefix);
        if (formatted == null) return true;

        boolean delivered = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission("streetliferp.dispatch.receive") || p.hasPermission("streetliferp.police") || p.hasPermission("streetliferp.ems")) {
                p.sendMessage(formatted);
                delivered = true;
            }
        }

        sender.sendMessage(prefix + (delivered ? ChatColor.GREEN + "Appel 911 envoyé." : ChatColor.YELLOW + "Aucun service en ligne, appel enregistré."));
        Location loc = sender.getLocation();
        String name = rpName(sender);
        if (name == null) name = "UNKNOWN";
        auditLog.logInfo("[911] " + sender.getUniqueId() + " (" + name + ") : " + message + " @ " + loc.getWorld().getName()
                + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ());
        return true;
    }

    private void broadcastLocal(Player sender, double radiusBlocks, String formatted) {
        if (radiusBlocks <= 0) {
            sender.sendMessage(formatted);
            return;
        }
        Location from = sender.getLocation();
        double r2 = radiusBlocks * radiusBlocks;
        for (Player p : sender.getWorld().getPlayers()) {
            if (p.getLocation().distanceSquared(from) <= r2) {
                p.sendMessage(formatted);
            }
        }
    }

    private String format(String pattern, Player sender, String message, String prefix) {
        PlayerData data = playerData.get(sender.getUniqueId());
        String name = rpName(sender);
        if (name == null) {
            sender.sendMessage(prefix + ChatColor.RED + "Crée ton personnage d'abord.");
            return null;
        }
        JobType job = jobs.get(sender.getUniqueId());

        Location loc = sender.getLocation();
        String out = pattern;
        out = out.replace("%name%", name);
        out = out.replace("%mcname%", name);
        out = out.replace("%job%", jobLabel(job));
        out = out.replace("%message%", message);
        out = out.replace("%world%", loc.getWorld() != null ? loc.getWorld().getName() : "world");
        out = out.replace("%x%", Integer.toString(loc.getBlockX()));
        out = out.replace("%y%", Integer.toString(loc.getBlockY()));
        out = out.replace("%z%", Integer.toString(loc.getBlockZ()));
        out = out.replace("%id%", data.idNumber() != null ? data.idNumber() : "N/A");
        return ChatColor.translateAlternateColorCodes('&', out);
    }

    private String rpName(Player sender) {
        return playerData.get(sender.getUniqueId()).rpNameOrNull();
    }

    private String jobLabel(JobType type) {
        return switch (type) {
            case UNEMPLOYED -> "Chômeur";
            case TAXI -> "Taxi";
            case BAKER -> "Boulanger";
            case BAR -> "Bar";
            case GROCERY -> "SupÃ©rette";
            case JOURNALIST -> "Journaliste";
            case MECHANIC -> "Mécano";
            case DEALER -> "Dealer";
            case STRIP_CLUB -> "Strip Club";
            case POLICE -> "Police";
            case EMS -> "EMS";
        };
    }

    private String sanitize(String raw) {
        if (raw == null) return "";
        String out = raw.replace('\n', ' ').replace('\r', ' ').trim();
        out = out.replace('§', '&');
        out = ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', out));
        if (out.length() > settings.maxMessageLength()) {
            out = out.substring(0, settings.maxMessageLength());
        }
        return out;
    }

    private record Settings(
            boolean proximityEnabled,
            double proximityRadiusBlocks,
            double meRadiusBlocks,
            double doRadiusBlocks,
            double oocRadiusBlocks,
            int maxMessageLength,
            String formatLocal,
            String formatMe,
            String formatDo,
            String formatOoc,
            String formatTweet,
            String formatEmergency
    ) {
        static Settings defaults() {
            return new Settings(
                    true,
                    25.0,
                    25.0,
                    25.0,
                    35.0,
                    200,
                    "&7[%job%] &f%name%&7: &r%message%",
                    "&d* &f%name%&d %message%",
                    "&5* &f%message% &7(%name%)",
                    "&8(( &7%name%&8: &7%message% &8))",
                    "&b[TWT] &f@%name%&7: &f%message%",
                    "&c[911] &f%name%&7: &f%message% &8(%world% %x% %y% %z%)"
            );
        }

        static Settings fromConfig(ConfigurationSection section) {
            Settings d = defaults();
            if (section == null) return d;
            ConfigurationSection formats = section.getConfigurationSection("formats");
            return new Settings(
                    section.getBoolean("proximity.enabled", d.proximityEnabled()),
                    section.getDouble("proximity.radius", d.proximityRadiusBlocks()),
                    section.getDouble("me_radius", d.meRadiusBlocks()),
                    section.getDouble("do_radius", d.doRadiusBlocks()),
                    section.getDouble("ooc_radius", d.oocRadiusBlocks()),
                    Math.max(40, section.getInt("max_message_length", d.maxMessageLength())),
                    getFormat(formats, "local", d.formatLocal()),
                    getFormat(formats, "me", d.formatMe()),
                    getFormat(formats, "do", d.formatDo()),
                    getFormat(formats, "ooc", d.formatOoc()),
                    getFormat(formats, "tweet", d.formatTweet()),
                    getFormat(formats, "emergency", d.formatEmergency())
            );
        }

        private static String getFormat(ConfigurationSection formats, String key, String def) {
            if (formats == null) return def;
            String raw = formats.getString(key);
            return raw == null || raw.isBlank() ? def : raw;
        }
    }
}
