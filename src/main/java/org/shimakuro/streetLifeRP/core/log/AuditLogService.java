package org.shimakuro.streetLifeRP.core.log;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

public final class AuditLogService {
    private final JavaPlugin plugin;
    private BufferedWriter writer;

    public AuditLogService(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void open() {
        try {
            Path logDir = plugin.getDataFolder().toPath().resolve("logs");
            Files.createDirectories(logDir);
            Path logFile = logDir.resolve("audit.log");
            writer = Files.newBufferedWriter(
                    logFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.WRITE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to open audit log.", e);
        }
    }

    public void close() {
        if (writer == null) return;
        try {
            writer.flush();
            writer.close();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to close audit log.", e);
        } finally {
            writer = null;
        }
    }

    public void logInfo(String message) {
        plugin.getLogger().info(message);
        appendLine("INFO", message);
    }

    public void logSensitive(String message) {
        appendLine("SENSITIVE", message);
    }

    private void appendLine(String level, String message) {
        if (writer == null) return;
        try {
            String ts = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
            writer.write(ts + " [" + level + "] " + message);
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to write audit log.", e);
        }
    }
}

