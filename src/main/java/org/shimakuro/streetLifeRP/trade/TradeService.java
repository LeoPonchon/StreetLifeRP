package org.shimakuro.streetLifeRP.trade;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.shimakuro.streetLifeRP.core.log.AuditLogService;
import org.shimakuro.streetLifeRP.data.PlayerDataRepository;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TradeService {
    public enum Mode { NONE, ONLY_A, ONLY_B, BOTH }

    private static final int SLOT_MODE = 4;
    private static final int SLOT_A_OFFER = 11;
    private static final int SLOT_B_OFFER = 15;
    private static final int SLOT_A_CONFIRM = 21;
    private static final int SLOT_B_CONFIRM = 23;
    private static final int SLOT_STATUS = 13;

    private final JavaPlugin plugin;
    private final AuditLogService audit;
    private final PlayerDataRepository playerData;

    private final Map<UUID, TradeSession> byPlayer = new ConcurrentHashMap<>();

    public TradeService(JavaPlugin plugin, AuditLogService audit, PlayerDataRepository playerData) {
        this.plugin = plugin;
        this.audit = audit;
        this.playerData = playerData;
    }

    public boolean hasSession(UUID uuid) {
        return byPlayer.containsKey(uuid);
    }

    public TradeSession session(UUID uuid) {
        return byPlayer.get(uuid);
    }

    public boolean openTrade(Player a, Player b, String prefix) {
        if (a.equals(b)) return false;
        if (hasSession(a.getUniqueId()) || hasSession(b.getUniqueId())) {
            a.sendMessage(prefix + ChatColor.RED + "Trade déjà en cours.");
            return true;
        }

        Inventory inv = Bukkit.createInventory(new TradeHolder(), 27, ChatColor.GOLD + "Trade");
        TradeSession session = new TradeSession(a.getUniqueId(), b.getUniqueId(), inv);
        session.mode = Mode.BOTH;
        updateUi(session);

        byPlayer.put(a.getUniqueId(), session);
        byPlayer.put(b.getUniqueId(), session);

        a.openInventory(inv);
        b.openInventory(inv);

        String aName = rpName(a);
        String bName = rpName(b);
        a.sendMessage(prefix + ChatColor.YELLOW + "Trade ouvert avec " + bName + ".");
        b.sendMessage(prefix + ChatColor.YELLOW + "Trade ouvert avec " + aName + ".");
        audit.logSensitive("TRADE_OPEN a=" + a.getUniqueId() + " b=" + b.getUniqueId());
        return true;
    }

    private String rpName(Player player) {
        String name = playerData.get(player.getUniqueId()).rpNameOrNull();
        return name != null ? name : player.getUniqueId().toString();
    }

    public void cancel(Player player, String prefix, String reason) {
        TradeSession session = byPlayer.get(player.getUniqueId());
        if (session == null) return;
        cancel(session, prefix, reason);
    }

    public void cancelAll() {
        for (TradeSession s : byPlayer.values()) {
            cancel(s, "", "shutdown");
        }
        byPlayer.clear();
    }

    private void cancel(TradeSession session, String prefix, String reason) {
        if (session.cancelled) return;
        session.cancelled = true;

        stopCountdown(session);
        returnOffer(session, session.a(), SLOT_A_OFFER);
        returnOffer(session, session.b(), SLOT_B_OFFER);

        Player a = Bukkit.getPlayer(session.a());
        Player b = Bukkit.getPlayer(session.b());
        if (a != null && a.getOpenInventory().getTopInventory().equals(session.inv())) {
            a.closeInventory();
            if (!prefix.isBlank()) a.sendMessage(prefix + ChatColor.RED + "Trade annulé.");
        }
        if (b != null && b.getOpenInventory().getTopInventory().equals(session.inv())) {
            b.closeInventory();
            if (!prefix.isBlank()) b.sendMessage(prefix + ChatColor.RED + "Trade annulé.");
        }

        byPlayer.remove(session.a());
        byPlayer.remove(session.b());
        audit.logSensitive("TRADE_CANCEL a=" + session.a() + " b=" + session.b() + " reason=" + reason);
    }

    private void returnOffer(TradeSession session, UUID owner, int slot) {
        ItemStack item = session.inv().getItem(slot);
        if (item == null || item.getType().isAir()) return;
        session.inv().setItem(slot, null);

        Player p = Bukkit.getPlayer(owner);
        if (p == null) return;
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
        for (ItemStack it : leftover.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), it);
        }
    }

    public void toggleConfirm(Player player, String prefix) {
        TradeSession session = byPlayer.get(player.getUniqueId());
        if (session == null) return;
        if (session.cancelled) return;

        if (player.getUniqueId().equals(session.a())) {
            session.aConfirmed = !session.aConfirmed;
        } else if (player.getUniqueId().equals(session.b())) {
            session.bConfirmed = !session.bConfirmed;
        }
        stopCountdown(session);
        updateUi(session);

        if (session.aConfirmed && session.bConfirmed) {
            startCountdown(session, prefix);
        }
    }

    public void cycleMode(Player player) {
        TradeSession session = byPlayer.get(player.getUniqueId());
        if (session == null) return;
        if (session.cancelled) return;
        if (!player.getUniqueId().equals(session.initiator)) return;

        session.mode = switch (session.mode) {
            case BOTH -> Mode.ONLY_A;
            case ONLY_A -> Mode.ONLY_B;
            case ONLY_B -> Mode.NONE;
            case NONE -> Mode.BOTH;
        };
        session.aConfirmed = false;
        session.bConfirmed = false;
        stopCountdown(session);
        updateUi(session);
    }

    public boolean canEditOffer(TradeSession session, UUID who, boolean offerA) {
        if (session.mode == Mode.NONE) return false;
        if (session.mode == Mode.BOTH) return true;
        if (session.mode == Mode.ONLY_A) return offerA && who.equals(session.a());
        if (session.mode == Mode.ONLY_B) return !offerA && who.equals(session.b());
        return false;
    }

    public void onOfferChanged(TradeSession session) {
        session.aConfirmed = false;
        session.bConfirmed = false;
        stopCountdown(session);
        updateUi(session);
    }

    private void startCountdown(TradeSession session, String prefix) {
        session.countdownSeconds = 3;
        session.countdownTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (session.cancelled) {
                stopCountdown(session);
                return;
            }
            if (!(session.aConfirmed && session.bConfirmed)) {
                stopCountdown(session);
                updateUi(session);
                return;
            }
            session.countdownSeconds--;
            if (session.countdownSeconds <= 0) {
                complete(session, prefix);
                stopCountdown(session);
                return;
            }
            updateUi(session);
        }, 20L, 20L);
        updateUi(session);
    }

    private void stopCountdown(TradeSession session) {
        if (session.countdownTaskId != -1) {
            plugin.getServer().getScheduler().cancelTask(session.countdownTaskId);
            session.countdownTaskId = -1;
        }
        session.countdownSeconds = 0;
    }

    private void complete(TradeSession session, String prefix) {
        ItemStack aOffer = safeClone(session.inv().getItem(SLOT_A_OFFER));
        ItemStack bOffer = safeClone(session.inv().getItem(SLOT_B_OFFER));
        session.inv().setItem(SLOT_A_OFFER, null);
        session.inv().setItem(SLOT_B_OFFER, null);

        Player a = Bukkit.getPlayer(session.a());
        Player b = Bukkit.getPlayer(session.b());
        if (a != null && bOffer != null) giveOrDrop(a, bOffer);
        if (b != null && aOffer != null) giveOrDrop(b, aOffer);

        if (a != null) a.sendMessage(prefix + ChatColor.GREEN + "Trade terminé.");
        if (b != null) b.sendMessage(prefix + ChatColor.GREEN + "Trade terminé.");
        audit.logSensitive("TRADE_COMPLETE a=" + session.a() + " b=" + session.b());

        cancel(session, prefix, "complete");
    }

    private ItemStack safeClone(ItemStack item) {
        if (item == null || item.getType().isAir()) return null;
        return item.clone();
    }

    private void giveOrDrop(Player p, ItemStack item) {
        Map<Integer, ItemStack> leftover = p.getInventory().addItem(item);
        for (ItemStack it : leftover.values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), it);
        }
    }

    public void updateUi(TradeSession session) {
        session.inv().setItem(SLOT_MODE, modeItem(session));
        session.inv().setItem(SLOT_A_CONFIRM, confirmItem("A", session.aConfirmed));
        session.inv().setItem(SLOT_B_CONFIRM, confirmItem("B", session.bConfirmed));
        session.inv().setItem(SLOT_STATUS, statusItem(session));
    }

    private ItemStack modeItem(TradeSession session) {
        ItemStack it = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.YELLOW + "Mode");
            meta.setLore(java.util.List.of(
                    ChatColor.GRAY + "BOTH / ONLY_A / ONLY_B / NONE",
                    ChatColor.WHITE + session.mode.name(),
                    ChatColor.DARK_GRAY + "Clic: changer (initiateur)"
            ));
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack confirmItem(String who, boolean confirmed) {
        ItemStack it = new ItemStack(confirmed ? Material.LIME_WOOL : Material.RED_WOOL);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName((confirmed ? ChatColor.GREEN : ChatColor.RED) + "Confirmer (" + who + ")");
            meta.setLore(java.util.List.of(ChatColor.DARK_GRAY + "Re-cliquer pour annuler"));
            it.setItemMeta(meta);
        }
        return it;
    }

    private ItemStack statusItem(TradeSession session) {
        ItemStack it = new ItemStack(Material.CLOCK);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            if (session.countdownTaskId != -1) {
                meta.setDisplayName(ChatColor.GOLD + "Validation...");
                meta.setLore(java.util.List.of(ChatColor.YELLOW + "Fin dans " + session.countdownSeconds + "s"));
            } else if (session.aConfirmed && session.bConfirmed) {
                meta.setDisplayName(ChatColor.GOLD + "Prêt");
                meta.setLore(java.util.List.of(ChatColor.YELLOW + "Validation en cours"));
            } else {
                meta.setDisplayName(ChatColor.GRAY + "En attente");
                meta.setLore(java.util.List.of(ChatColor.DARK_GRAY + "Les 2 doivent confirmer"));
            }
            it.setItemMeta(meta);
        }
        return it;
    }

    public static int offerSlotA() { return SLOT_A_OFFER; }
    public static int offerSlotB() { return SLOT_B_OFFER; }
    public static int modeSlot() { return SLOT_MODE; }
    public static int confirmSlotA() { return SLOT_A_CONFIRM; }
    public static int confirmSlotB() { return SLOT_B_CONFIRM; }

    public static final class TradeHolder implements InventoryHolder {
        @Override
        public Inventory getInventory() {
            return null;
        }
    }

    public static final class TradeSession {
        private final UUID a;
        private final UUID b;
        private final Inventory inv;
        private final UUID initiator;

        private volatile boolean aConfirmed;
        private volatile boolean bConfirmed;
        private volatile Mode mode;
        private volatile boolean cancelled;
        private volatile int countdownTaskId = -1;
        private volatile int countdownSeconds;

        private TradeSession(UUID a, UUID b, Inventory inv) {
            this.a = a;
            this.b = b;
            this.inv = inv;
            this.initiator = a;
        }

        public UUID a() { return a; }
        public UUID b() { return b; }
        public Inventory inv() { return inv; }
    }
}
