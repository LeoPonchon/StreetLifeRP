package org.shimakuro.streetLifeRP.billing;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.shimakuro.streetLifeRP.core.config.ConfigService;
import org.shimakuro.streetLifeRP.economy.EconomyService;
import org.shimakuro.streetLifeRP.jobs.JobType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class BillingService {
    private final ConfigService config;
    private final EconomyService economy;
    private final NamespacedKey typeKey;
    private final NamespacedKey issuerKey;
    private final NamespacedKey amountKey;
    private final NamespacedKey jobKey;

    public BillingService(JavaPlugin plugin, ConfigService config, EconomyService economy) {
        this.config = config;
        this.economy = economy;
        this.typeKey = new NamespacedKey(plugin, "billing_type");
        this.issuerKey = new NamespacedKey(plugin, "billing_issuer");
        this.amountKey = new NamespacedKey(plugin, "billing_amount");
        this.jobKey = new NamespacedKey(plugin, "billing_job");
    }

    public ItemStack createTool() {
        FileConfiguration cfg = config.billingRaw();
        String materialName = cfg.getString("billing.tool.material");
        Material mat = materialName != null ? Material.matchMaterial(materialName) : null;
        if (mat == null) mat = Material.PAPER;

        String name = translate(cfg.getString("billing.tool.name"));
        List<String> lore = translateList(cfg.getStringList("billing.tool.lore"));

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "tool");
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createInvoice(UUID issuer, JobType job, double amount) {
        FileConfiguration cfg = config.billingRaw();
        String kind = kindForJob(cfg, job);
        String titleKey = "fine".equalsIgnoreCase(kind) ? "billing.invoice.title_fine" : "billing.invoice.title_invoice";
        String title = cfg.getString(titleKey);

        String nameTpl = cfg.getString("billing.invoice.item_name");
        List<String> loreTpl = cfg.getStringList("billing.invoice.item_lore");

        String currency = config.currencySymbol();
        String amountFmt = economy.format(amount);

        String name = translate(applyVars(nameTpl, title, amountFmt, currency, job));
        List<String> lore = translateList(applyVars(loreTpl, title, amountFmt, currency, job));

        String materialName = cfg.getString("billing.invoice.material");
        Material mat = materialName != null ? Material.matchMaterial(materialName) : null;
        if (mat == null) mat = Material.PAPER;

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, "invoice");
            meta.getPersistentDataContainer().set(issuerKey, PersistentDataType.STRING, issuer.toString());
            meta.getPersistentDataContainer().set(amountKey, PersistentDataType.DOUBLE, amount);
            meta.getPersistentDataContainer().set(jobKey, PersistentDataType.STRING, job != null ? job.name() : "");
            item.setItemMeta(meta);
        }
        return item;
    }

    public BillingData read(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String t = meta.getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
        if (t == null || t.isBlank()) return null;

        UUID issuer = null;
        String rawIssuer = meta.getPersistentDataContainer().get(issuerKey, PersistentDataType.STRING);
        if (rawIssuer != null && !rawIssuer.isBlank()) {
            try {
                issuer = UUID.fromString(rawIssuer);
            } catch (IllegalArgumentException ignored) {
                issuer = null;
            }
        }

        Double amount = meta.getPersistentDataContainer().get(amountKey, PersistentDataType.DOUBLE);
        String rawJob = meta.getPersistentDataContainer().get(jobKey, PersistentDataType.STRING);
        JobType job = null;
        if (rawJob != null && !rawJob.isBlank()) {
            try {
                job = JobType.valueOf(rawJob);
            } catch (IllegalArgumentException ignored) {
                job = null;
            }
        }

        return new BillingData(t, issuer, amount != null ? amount : 0.0, job);
    }

    private String kindForJob(FileConfiguration cfg, JobType job) {
        if (job == null) return "invoice";
        String v = cfg.getString("billing.kind_by_job." + job.name().toLowerCase());
        return v != null ? v : "invoice";
    }

    private String applyVars(String raw, String title, String amount, String currency, JobType job) {
        if (raw == null) return null;
        String jobName = job != null ? job.name().toLowerCase() : null;
        String jobDisplay = job != null ? config.jobsRaw().getString("jobs." + jobName + ".display_name") : null;
        return raw
                .replace("%title%", title != null ? title : "")
                .replace("%amount%", amount != null ? amount : "")
                .replace("%currency%", currency != null ? currency : "")
                .replace("%job%", jobDisplay != null ? jobDisplay : "");
    }

    private List<String> applyVars(List<String> lines, String title, String amount, String currency, JobType job) {
        if (lines == null) return List.of();
        ArrayList<String> out = new ArrayList<>(lines.size());
        for (String l : lines) out.add(applyVars(l, title, amount, currency, job));
        return out;
    }

    private String translate(String raw) {
        if (raw == null) return null;
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    private List<String> translateList(List<String> lines) {
        if (lines == null) return List.of();
        ArrayList<String> out = new ArrayList<>(lines.size());
        for (String l : lines) out.add(translate(l));
        return out;
    }

    public record BillingData(String type, UUID issuer, double amount, JobType issuerJob) {}
}
