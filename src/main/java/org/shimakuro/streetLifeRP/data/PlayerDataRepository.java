package org.shimakuro.streetLifeRP.data;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class PlayerDataRepository {
    private final JavaPlugin plugin;
    private final File playersDir;
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public PlayerDataRepository(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playersDir = new File(plugin.getDataFolder(), "players");
    }

    public void init() {
        //noinspection ResultOfMethodCallIgnored
        playersDir.mkdirs();
    }

    public PlayerData get(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::load);
    }

    public void unload(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            save(data);
        }
    }

    public synchronized void save(PlayerData data) {
        File file = fileFor(data.uuid());
        YamlConfiguration cfg = new YamlConfiguration();

        cfg.set("character.first_name", data.firstName());
        cfg.set("character.last_name", data.lastName());
        cfg.set("identity.id_number", data.idNumber());
        cfg.set("phone.number", data.phoneNumber());

        cfg.set("economy.cash", data.cash());
        cfg.set("economy.bank", data.bank());

        cfg.set("jobs.type", data.job());
        cfg.set("jobs.last_work_at", data.lastWorkAtMillis());

        cfg.set("justice.cuffed", data.cuffed());

        cfg.set("justice.fine.amount", data.fineAmount());
        cfg.set("justice.fine.issuer", data.fineIssuer());
        cfg.set("justice.fine.reason", data.fineReason());
        cfg.set("justice.fine.issued_at", data.fineIssuedAtMillis());

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data: " + data.uuid(), e);
        }
    }

    public synchronized void saveAll() {
        for (PlayerData data : cache.values()) {
            save(data);
        }
    }

    private PlayerData load(UUID uuid) {
        File file = fileFor(uuid);
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        PlayerData data = new PlayerData(uuid);
        data.setFirstName(cfg.getString("character.first_name"));
        data.setLastName(cfg.getString("character.last_name"));
        data.setIdNumber(cfg.getString("identity.id_number"));
        data.setPhoneNumber(cfg.getString("phone.number"));

        data.setCash(cfg.getDouble("economy.cash", 0.0));
        data.setBank(cfg.getDouble("economy.bank", 0.0));

        data.setJob(cfg.getString("jobs.type"));
        data.setLastWorkAtMillis(cfg.getLong("jobs.last_work_at", 0L));

        data.setCuffed(cfg.getBoolean("justice.cuffed", false));

        data.setFineAmount(cfg.getDouble("justice.fine.amount", 0.0));
        data.setFineIssuer(cfg.getString("justice.fine.issuer"));
        data.setFineReason(cfg.getString("justice.fine.reason"));
        data.setFineIssuedAtMillis(cfg.getLong("justice.fine.issued_at", 0L));

        return data;
    }

    private File fileFor(UUID uuid) {
        return new File(playersDir, uuid + ".yml");
    }
}
