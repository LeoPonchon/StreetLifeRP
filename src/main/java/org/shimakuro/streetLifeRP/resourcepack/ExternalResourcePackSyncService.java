package org.shimakuro.streetLifeRP.resourcepack;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class ExternalResourcePackSyncService {
    private final JavaPlugin plugin;
    private final HttpClient http;

    public ExternalResourcePackSyncService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void syncNow(String reason) {
        File pluginsDir = plugin.getDataFolder().getParentFile();
        if (pluginsDir == null) return;

        File oraxenPack = new File(pluginsDir, "Oraxen" + File.separator + "pack");
        if (!oraxenPack.exists() && !oraxenPack.mkdirs()) {
            plugin.getLogger().warning("Unable to create Oraxen pack directory: " + oraxenPack.getAbsolutePath());
            return;
        }

        disablePluginResourcePackSends(pluginsDir);

        List<Source> sources = List.of(
                new Source("qa", new File(pluginsDir, "QualityArmory" + File.separator + "config.yml"), "DefaultResourcepack"),
                new Source("qav2", new File(pluginsDir, "QualityArmoryVehicles2" + File.separator + "config.yml"), "QAMini.resourcepackurl")
        );

        File cacheDir = new File(plugin.getDataFolder(), "resourcepack-cache");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            plugin.getLogger().warning("Unable to create resourcepack cache directory: " + cacheDir.getAbsolutePath());
            return;
        }

        for (Source source : sources) {
            try {
                syncSource(source, cacheDir.toPath(), oraxenPack.toPath());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to sync " + source.id() + " resourcepack (" + reason + "): "
                        + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    public void syncAsync(String reason) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> syncNow(reason));
    }

    private void syncSource(Source source, Path cacheDir, Path oraxenPack) throws Exception {
        if (!source.config().exists()) return;

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(source.config());
        String url = pickResourcePackUrl(cfg.getConfigurationSection(source.urlSectionPath()));
        if (url == null || url.isBlank()) return;

        Path zipPath = cacheDir.resolve(source.id() + ".zip");
        download(url, zipPath);
        int files = extractAssets(zipPath, oraxenPack);
        plugin.getLogger().info("Synced " + source.id() + " resourcepack into Oraxen pack (" + files + " files).");
    }

    private String pickResourcePackUrl(ConfigurationSection section) {
        if (section == null) return null;
        ArrayList<String> keys = new ArrayList<>(section.getKeys(false));
        keys.sort(Comparator.comparingInt(this::resourcePackKeyWeight).reversed());
        for (String key : keys) {
            String value = section.getString(key);
            if (value != null && !value.isBlank()) return value.trim();
        }
        return null;
    }

    private int resourcePackKeyWeight(String key) {
        if (key == null) return 0;
        String normalized = key.toLowerCase(Locale.ROOT).replace('_', '-');
        if (normalized.equals("0")) return 0;
        String digits = normalized.replaceAll("[^0-9]", "");
        if (digits.isBlank()) return 1;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 1;
        }
    }

    private void download(String url, Path out) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<InputStream> response = http.send(request, HttpResponse.BodyHandlers.ofInputStream());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new IOException("HTTP " + status + " for " + url);
        }

        Path tmp = out.resolveSibling(out.getFileName() + ".tmp");
        try (InputStream in = response.body()) {
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
        }
        Files.move(tmp, out, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private int extractAssets(Path zipPath, Path packRoot) throws IOException {
        int copied = 0;
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) continue;

                String normalized = entry.getName().replace('\\', '/');
                int assetsIndex = normalized.indexOf("assets/");
                if (assetsIndex < 0) continue;

                String relative = normalized.substring(assetsIndex);
                Path target = packRoot.resolve(relative).normalize();
                if (!target.startsWith(packRoot.normalize())) continue;

                Path parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.copy(zip, target, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            }
        }
        return copied;
    }

    private void disablePluginResourcePackSends(File pluginsDir) {
        updateYaml(new File(pluginsDir, "QualityArmory" + File.separator + "config.yml"), cfg -> {
            cfg.set("sendOnJoin", false);
            cfg.set("useDefaultResourcepack", false);
            cfg.set("KickPlayerIfDeniedResourcepack", false);
        });

        updateYaml(new File(pluginsDir, "QualityArmoryVehicles2" + File.separator + "config.yml"), cfg -> {
            cfg.set("QAMini.sendResourcepack", false);
            cfg.set("QAMini.sendResourcepackOnJoin", false);
            cfg.set("QAMini.sendResourcepackTitleOnJoin", false);
            cfg.set("QAMini.kickIfRejectResourcepack", false);
        });
    }

    private void updateYaml(File file, YamlEdit edit) {
        if (!file.exists()) return;
        try {
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
            edit.apply(cfg);
            cfg.save(file);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to update " + file.getAbsolutePath() + ": "
                    + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private record Source(String id, File config, String urlSectionPath) {}

    private interface YamlEdit {
        void apply(YamlConfiguration cfg);
    }
}

