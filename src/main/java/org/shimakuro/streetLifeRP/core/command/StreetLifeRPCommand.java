package org.shimakuro.streetLifeRP.core.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;
import org.shimakuro.streetLifeRP.items.SpecialItemType;
import org.shimakuro.streetLifeRP.jobs.JobType;

import java.util.ArrayList;
import java.util.List;

public final class StreetLifeRPCommand implements CommandExecutor, TabCompleter {
    private static final String PERM_RELOAD = "streetliferp.admin.reload";
    private static final String PERM_CHAR_DELETE = "streetliferp.admin.character.delete";
    private static final String PERM_ITEM_GIVE = "streetliferp.admin.item.give";
    private static final String PERM_JOB_SET = "streetliferp.admin.job.set";
    private static final String PERM_CUFF_TOGGLE = "streetliferp.admin.cuff.toggle";

    private final StreetLifeRPContext ctx;

    public StreetLifeRPCommand(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        String sub = args[0].toLowerCase();
        return switch (sub) {
            case "reload" -> handleReload(sender, label);
            case "admin" -> handleAdmin(sender, label, args);
            default -> {
                sendHelp(sender, label);
                yield true;
            }
        };
    }

    private boolean handleReload(CommandSender sender, String label) {
        if (!sender.hasPermission(PERM_RELOAD)) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Commande admin: /" + label + " reload");
            return true;
        }
        ctx.reloadAll();
        sender.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Configuration rechargée.");
        ctx.auditLog().logInfo(sender.getName() + " reloaded configuration.");
        return true;
    }

    private boolean handleAdmin(CommandSender sender, String label, String[] args) {
        if (args.length < 2) {
            sendAdminHelp(sender, label);
            return true;
        }

        String area = args[1].toLowerCase();
        return switch (area) {
            case "character" -> handleAdminCharacter(sender, label, args);
            case "item" -> handleAdminItem(sender, label, args);
            case "cuff" -> handleAdminCuff(sender, label, args);
            case "job" -> handleAdminJob(sender, label, args);
            default -> {
                sendAdminHelp(sender, label);
                yield true;
            }
        };
    }

    private boolean handleAdminCharacter(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERM_CHAR_DELETE)) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
            return true;
        }
        if (args.length < 4 || !args[2].equalsIgnoreCase("delete")) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " admin character delete <joueur>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[3]);
        if (target == null) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur introuvable (doit être en ligne).");
            return true;
        }

        boolean deleted = ctx.characters().delete(target.getUniqueId());
        if (!deleted) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Ce joueur n'a pas de personnage à supprimer.");
            return true;
        }

        target.sendMessage(ctx.config().prefix() + ChatColor.RED + "Votre personnage a été supprimé par un admin.");
        ctx.auditLog().logInfo(sender.getName() + " deleted character for " + target.getUniqueId());
        sender.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Personnage supprimé pour " + target.getName() + ".");
        return true;
    }

    private boolean handleAdminItem(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERM_ITEM_GIVE)) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
            return true;
        }
        if (args.length < 5 || !args[2].equalsIgnoreCase("give")) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " admin item give <joueur> <item>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[3]);
        if (target == null) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur introuvable (doit être en ligne).");
            return true;
        }
        SpecialItemType type;
        try {
            type = SpecialItemType.valueOf(args[4].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Item invalide.");
            return true;
        }
        target.getInventory().addItem(ctx.items().create(type));
        sender.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Item donné: " + type.name() + " -> " + target.getName());
        return true;
    }

    private boolean handleAdminCuff(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERM_CUFF_TOGGLE)) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " admin cuff <joueur>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur introuvable (doit être en ligne).");
            return true;
        }
        boolean next = !ctx.justice().isCuffed(target.getUniqueId());
        String actorRp = sender instanceof Player p ? ctx.characters().rpNameOrNull(p.getUniqueId()) : sender.getName();
        ctx.justice().setCuffed(target, next, ctx.config().prefix(), actorRp != null ? actorRp : sender.getName());
        sender.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Menottes " + (next ? "activées" : "retirées") + " pour " + target.getName() + ".");
        return true;
    }

    private boolean handleAdminJob(CommandSender sender, String label, String[] args) {
        if (!sender.hasPermission(PERM_JOB_SET)) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
            return true;
        }
        if (args.length < 5 || !args[2].equalsIgnoreCase("set")) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " admin job set <joueur> <job>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[3]);
        if (target == null) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur introuvable (doit être en ligne).");
            return true;
        }
        JobType type;
        try {
            type = JobType.valueOf(args[4].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Job invalide.");
            return true;
        }
        ctx.jobs().set(target.getUniqueId(), type);
        sender.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Metier mis a jour: " + target.getName() + " -> " + type.name());
        target.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Vous etes maintenant: " + type.name());
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        if (!sender.hasPermission(PERM_RELOAD)
                && !sender.hasPermission(PERM_CHAR_DELETE)
                && !sender.hasPermission(PERM_ITEM_GIVE)
                && !sender.hasPermission(PERM_JOB_SET)
                && !sender.hasPermission(PERM_CUFF_TOGGLE)) {
            sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Commandes admin.");
            return;
        }
        sender.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Commandes admin :");
        if (sender.hasPermission(PERM_RELOAD)) sender.sendMessage(ChatColor.GRAY + "/" + label + " reload");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " admin ...");
    }

    private void sendAdminHelp(CommandSender sender, String label) {
        sender.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " admin character delete <joueur>");
        sender.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "       /" + label + " admin item give <joueur> <item>");
        sender.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "       /" + label + " admin cuff <joueur>");
        sender.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "       /" + label + " admin job set <joueur> <job>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        boolean anyAdminPerm = sender.hasPermission(PERM_RELOAD)
                || sender.hasPermission(PERM_CHAR_DELETE)
                || sender.hasPermission(PERM_ITEM_GIVE)
                || sender.hasPermission(PERM_JOB_SET)
                || sender.hasPermission(PERM_CUFF_TOGGLE);
        if (!anyAdminPerm) return List.of();

        if (args.length == 1) {
            ArrayList<String> out = new ArrayList<>();
            if (sender.hasPermission(PERM_RELOAD)) out.add("reload");
            out.add("admin");
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return List.of("character", "item", "cuff", "job");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("character")) {
            return List.of("delete");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("item")) {
            return List.of("give");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("job")) {
            return List.of("set");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("item") && args[2].equalsIgnoreCase("give")) {
            ArrayList<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("item") && args[2].equalsIgnoreCase("give")) {
            ArrayList<String> out = new ArrayList<>();
            for (SpecialItemType t : SpecialItemType.values()) out.add(t.name());
            return out;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("character") && args[2].equalsIgnoreCase("delete")) {
            ArrayList<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("cuff")) {
            ArrayList<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("job") && args[2].equalsIgnoreCase("set")) {
            ArrayList<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) names.add(p.getName());
            return names;
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("job") && args[2].equalsIgnoreCase("set")) {
            return List.of("UNEMPLOYED", "TAXI", "BAR", "JOURNALIST", "MECHANIC", "POLICE", "BAKER", "DEALER", "STRIP_CLUB", "EMS");
        }
        return List.of();
    }
}
