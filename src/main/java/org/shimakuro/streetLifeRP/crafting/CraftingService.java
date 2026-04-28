package org.shimakuro.streetLifeRP.crafting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.shimakuro.streetLifeRP.jobs.JobService;
import org.shimakuro.streetLifeRP.jobs.JobType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CraftingService implements Listener {
    private static final String ITEM_PAIN_AU_CHOCOLAT = "pain_au_chocolat";
    private static final String ITEM_SANDWICH = "sandwich";
    private static final String ITEM_COFFEE = "coffee";
    private static final String ITEM_SYNTH_DRUG = "synthetic_drug";
    private static final String ITEM_SPACE_COOKIE = "space_cookie";
    private static final String ITEM_REPAIR_KIT = "repair_kit";
    private static final String ITEM_AMMO_BOX = "ammo_box";

    private final JavaPlugin plugin;
    private final JobService jobs;
    private final NamespacedKey customItemKey;
    private final Map<NamespacedKey, JobType> recipeJobs = new HashMap<>();

    public CraftingService(JavaPlugin plugin, JobService jobs) {
        this.plugin = plugin;
        this.jobs = jobs;
        this.customItemKey = new NamespacedKey(plugin, "custom_item");
    }

    public void enable() {
        registerRecipes();
    }

    public void disable() {
        for (NamespacedKey key : recipeJobs.keySet()) {
            Bukkit.removeRecipe(key);
        }
        recipeJobs.clear();
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null || isStreetLifeRecipe(recipe)) return;
        event.getInventory().setResult(null);
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        Recipe recipe = event.getRecipe();
        if (recipe == null) {
            event.setCancelled(true);
            return;
        }
        NamespacedKey key = recipeKey(recipe);
        if (key == null || !"streetliferp".equals(key.getNamespace())) {
            event.setCancelled(true);
            return;
        }

        JobType required = recipeJobs.get(key);
        if (required == null) return;
        if (!(event.getWhoClicked() instanceof org.bukkit.entity.Player player)) {
            event.setCancelled(true);
            return;
        }
        if (jobs.get(player.getUniqueId()) != required && !adminBypassJobs().contains(jobs.get(player.getUniqueId()))) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.RED + "Métier requis: " + required.name());
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        String customItem = customItemId(event.getItem());
        if (customItem == null) return;

        switch (customItem) {
            case ITEM_PAIN_AU_CHOCOLAT, ITEM_SANDWICH -> {
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 80, 0));
            }
            case ITEM_COFFEE -> {
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 45, 0));
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 45, 0));
            }
            case ITEM_SYNTH_DRUG -> {
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 90, 1));
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 45, 0));
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, 20 * 12, 0));
            }
            case ITEM_SPACE_COOKIE -> {
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 20, 0));
                event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 25, 0));
            }
            default -> {
                // no-op
            }
        }
    }

    private void registerRecipes() {
        disable();
        register(painAuChocolatRecipe(), JobType.BAKER);
        register(sandwichRecipe(), JobType.BAKER);
        register(coffeeRecipe(), JobType.BAKER);
        register(syntheticDrugRecipe(), JobType.DEALER);
        register(spaceCookieRecipe(), JobType.DEALER);
        register(repairKitRecipe(), JobType.MECHANIC);
        register(ammoBoxRecipe(), JobType.GUNSMITH);
    }

    private void register(Recipe recipe, JobType job) {
        NamespacedKey key = recipeKey(recipe);
        if (key == null) return;
        Bukkit.removeRecipe(key);
        Bukkit.addRecipe(recipe);
        recipeJobs.put(key, job);
    }

    private Recipe painAuChocolatRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(key(ITEM_PAIN_AU_CHOCOLAT), item(Material.BREAD, ChatColor.GOLD + "Pain au chocolat", ITEM_PAIN_AU_CHOCOLAT, List.of(
                ChatColor.GRAY + "Pain + cacao",
                ChatColor.DARK_GRAY + "Métier: Boulanger"
        )));
        recipe.addIngredient(Material.BREAD);
        recipe.addIngredient(Material.COCOA_BEANS);
        return recipe;
    }

    private Recipe sandwichRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(key(ITEM_SANDWICH), item(Material.BREAD, ChatColor.YELLOW + "Sandwich jambon-fromage", ITEM_SANDWICH, List.of(
                ChatColor.GRAY + "Pain + viande + lait",
                ChatColor.DARK_GRAY + "Métier: Boulanger"
        )));
        recipe.addIngredient(Material.BREAD);
        recipe.addIngredient(Material.COOKED_BEEF);
        recipe.addIngredient(Material.MILK_BUCKET);
        return recipe;
    }

    private Recipe coffeeRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(key(ITEM_COFFEE), item(Material.HONEY_BOTTLE, ChatColor.DARK_AQUA + "Café serré", ITEM_COFFEE, List.of(
                ChatColor.GRAY + "Cacao + sucre + bouteille",
                ChatColor.DARK_GRAY + "Métier: Boulanger"
        )));
        recipe.addIngredient(Material.COCOA_BEANS);
        recipe.addIngredient(Material.SUGAR);
        recipe.addIngredient(Material.GLASS_BOTTLE);
        return recipe;
    }

    private Recipe syntheticDrugRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(key(ITEM_SYNTH_DRUG), item(Material.HONEY_BOTTLE, ChatColor.LIGHT_PURPLE + "Drogue de synthèse", ITEM_SYNTH_DRUG, List.of(
                ChatColor.GRAY + "Boost violent, descente sale",
                ChatColor.DARK_GRAY + "Métier: Dealer"
        )));
        recipe.addIngredient(Material.SUGAR);
        recipe.addIngredient(Material.REDSTONE);
        recipe.addIngredient(Material.GLOWSTONE_DUST);
        recipe.addIngredient(Material.GLASS_BOTTLE);
        return recipe;
    }

    private Recipe spaceCookieRecipe() {
        ShapelessRecipe recipe = new ShapelessRecipe(key(ITEM_SPACE_COOKIE), item(Material.COOKIE, ChatColor.GREEN + "Space cookie", ITEM_SPACE_COOKIE, List.of(
                ChatColor.GRAY + "Cookie artisanal très relaxant",
                ChatColor.DARK_GRAY + "Métier: Dealer"
        )));
        recipe.addIngredient(Material.COCOA_BEANS);
        recipe.addIngredient(Material.WHEAT);
        recipe.addIngredient(Material.SUGAR);
        recipe.addIngredient(Material.FERN);
        return recipe;
    }

    private Recipe repairKitRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(key(ITEM_REPAIR_KIT), item(Material.IRON_INGOT, ChatColor.GRAY + "Kit de réparation", ITEM_REPAIR_KIT, List.of(
                ChatColor.GRAY + "Pièces mécaniques prêtes à l'emploi",
                ChatColor.DARK_GRAY + "Métier: Mécano"
        )));
        recipe.shape("IRI", "RLR", "IRI");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('L', Material.LEVER);
        return recipe;
    }

    private Recipe ammoBoxRecipe() {
        ShapedRecipe recipe = new ShapedRecipe(key(ITEM_AMMO_BOX), item(Material.IRON_NUGGET, ChatColor.DARK_GRAY + "Boîte de munitions", ITEM_AMMO_BOX, List.of(
                ChatColor.GRAY + "Matériel d'armurerie",
                ChatColor.DARK_GRAY + "Métier: Armurier"
        )));
        recipe.shape("III", "RGR", "III");
        recipe.setIngredient('I', Material.IRON_NUGGET);
        recipe.setIngredient('R', Material.REDSTONE);
        recipe.setIngredient('G', Material.GUNPOWDER);
        return recipe;
    }

    private ItemStack item(Material material, String displayName, String id, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(customItemKey, PersistentDataType.STRING, id);
            item.setItemMeta(meta);
        }
        return item;
    }

    private NamespacedKey key(String id) {
        return new NamespacedKey(plugin, id);
    }

    private boolean isStreetLifeRecipe(Recipe recipe) {
        NamespacedKey key = recipeKey(recipe);
        return key != null && "streetliferp".equals(key.getNamespace());
    }

    private NamespacedKey recipeKey(Recipe recipe) {
        if (recipe instanceof org.bukkit.Keyed keyed) return keyed.getKey();
        return null;
    }

    private String customItemId(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        return meta.getPersistentDataContainer().get(customItemKey, PersistentDataType.STRING);
    }

    private EnumSet<JobType> adminBypassJobs() {
        return EnumSet.of(JobType.ADMINPLUS, JobType.ADMINMINUS);
    }
}
