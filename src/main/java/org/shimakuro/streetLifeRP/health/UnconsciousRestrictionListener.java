package org.shimakuro.streetLifeRP.health;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * While unconscious, block most interactions (same spirit as cuffed restrictions).
 */
public final class UnconsciousRestrictionListener implements Listener {
    private final StreetLifeRPContext ctx;
    private final Map<UUID, Long> lastWarnMillis = new ConcurrentHashMap<>();

    public UnconsciousRestrictionListener(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        Player player = event.getPlayer();
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player damager = findDamagingPlayer(event.getDamager());
        if (damager == null) return;
        if (!ctx.unconscious().isUnconscious(damager)) return;

        event.setCancelled(true);
        warn(damager);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        ProjectileSource shooter = event.getEntity().getShooter();
        if (!(shooter instanceof Player player)) return;
        if (!ctx.unconscious().isUnconscious(player)) return;

        event.setCancelled(true);
        warn(player);
    }

    private Player findDamagingPlayer(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile) {
            ProjectileSource shooter = projectile.getShooter();
            if (shooter instanceof Player player) return player;
        }
        return null;
    }

    private void warn(Player player) {
        long now = System.currentTimeMillis();
        Long last = lastWarnMillis.get(player.getUniqueId());
        if (last != null && now - last < 1500L) return;
        lastWarnMillis.put(player.getUniqueId(), now);
        player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Inconscient: impossible d'utiliser des objets.");
    }
}

