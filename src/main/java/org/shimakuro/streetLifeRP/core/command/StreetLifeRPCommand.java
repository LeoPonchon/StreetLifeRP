package org.shimakuro.streetLifeRP.core.command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;
import org.shimakuro.streetLifeRP.data.PlayerData;
import org.shimakuro.streetLifeRP.items.SpecialItemType;
import org.shimakuro.streetLifeRP.jobs.JobService;
import org.shimakuro.streetLifeRP.jobs.JobType;

import java.util.ArrayList;
import java.util.List;

public final class StreetLifeRPCommand implements CommandExecutor, TabCompleter {
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
        if (sub.equals("reload")) {
            if (!sender.hasPermission("streetliferp.admin.reload")) {
                sender.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                return true;
            }
            ctx.reloadAll();
            sender.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Configuration rechargée.");
            ctx.auditLog().logInfo(sender.getName() + " reloaded configuration.");
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        return switch (sub) {
            case "character" -> handleCharacter(player, label, args);
            case "id" -> handleId(player, label, args);
            case "money" -> handleMoney(player);
            case "pay" -> handlePay(player, args);
            case "bank" -> handleBank(player, label, args);
            case "job" -> handleJob(player, label, args);
            case "police" -> handlePolice(player, label, args);
            case "fine" -> handleFine(player, label, args);
            case "admin" -> handleAdmin(player, label, args);
            default -> {
                sendHelp(player, label);
                yield true;
            }
        };
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Commandes :");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " reload");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " character create <prenom> <nom>");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " id [card]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " money");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " pay <joueur> <montant>");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " bank deposit|withdraw <montant>");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " job [list|set]");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " fine pay");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " police cuff|uncuff|fine ...");
        sender.sendMessage(ChatColor.DARK_GRAY + "RP: /me, /do, /ooc, /twt, /911");
        sender.sendMessage(ChatColor.DARK_GRAY + "Admin: /" + label + " admin character delete <joueur>");
        sender.sendMessage(ChatColor.DARK_GRAY + "Admin: /" + label + " admin cuff <joueur>");
    }

    private boolean handleCharacter(Player player, String label, String[] args) {
        if (args.length < 2 || !args[1].equalsIgnoreCase("create") || args.length < 4) {
            player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " character create <prenom> <nom>");
            return true;
        }
        String firstName = args[2];
        String lastName = args[3];
        boolean ok = ctx.characters().create(player.getUniqueId(), firstName, lastName);
        if (!ok) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Personnage déjà créé.");
            return true;
        }
        player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Personnage créé: " + firstName + " " + lastName);
        return true;
    }

    private boolean handleId(Player player, String label, String[] args) {
        PlayerData data = ctx.characters().data(player.getUniqueId());
        if (!data.hasCharacter()) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage: /" + label + " character create <prenom> <nom>");
            return true;
        }

        if (args.length >= 2 && args[1].equalsIgnoreCase("card")) {
            player.getInventory().addItem(ctx.identity().createIdCard(data.firstName(), data.lastName(), data.idNumber()));
            player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Carte d'identité ajoutée à l'inventaire.");
            return true;
        }

        player.sendMessage(ctx.config().prefix() + ChatColor.AQUA + "Identité RP:");
        player.sendMessage(ChatColor.GRAY + "Nom: " + ChatColor.WHITE + data.lastName());
        player.sendMessage(ChatColor.GRAY + "Prénom: " + ChatColor.WHITE + data.firstName());
        player.sendMessage(ChatColor.GRAY + "ID: " + ChatColor.WHITE + data.idNumber());
        player.sendMessage(ChatColor.DARK_GRAY + "Astuce: /" + label + " id card");
        return true;
    }

    private boolean handleMoney(Player player) {
        PlayerData data = ctx.characters().data(player.getUniqueId());
        if (!data.hasCharacter()) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage d'abord.");
            return true;
        }
        player.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Cash: " + ChatColor.GOLD + ctx.economy().format(data.cash()));
        player.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Banque: " + ChatColor.GOLD + ctx.economy().format(data.bank()));
        return true;
    }

    private boolean handlePay(Player player, String[] args) {
        PlayerData data = ctx.characters().data(player.getUniqueId());
        if (!data.hasCharacter()) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage d'abord.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /slrp pay <joueur> <montant>");
            return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur introuvable.");
            return true;
        }
        if (!ctx.characters().data(target.getUniqueId()).hasCharacter()) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Ce joueur n'a pas de personnage.");
            return true;
        }
        Double amount = parseAmount(args[2]);
        if (amount == null || amount <= 0) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Montant invalide.");
            return true;
        }
        if (!ctx.economy().transferCash(player.getUniqueId(), target.getUniqueId(), amount)) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Transaction refusée (fonds ou anti-abuse).");
            return true;
        }
        String targetRp = ctx.characters().rpNameOrNull(target.getUniqueId());
        String actorRp = ctx.characters().rpNameOrNull(player.getUniqueId());
        player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Payé " + ctx.economy().format(amount) + " à " + (targetRp != null ? targetRp : target.getUniqueId()) + ".");
        target.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Reçu " + ctx.economy().format(amount) + " de " + (actorRp != null ? actorRp : player.getUniqueId()) + ".");
        return true;
    }

    private boolean handleBank(Player player, String label, String[] args) {
        PlayerData data = ctx.characters().data(player.getUniqueId());
        if (!data.hasCharacter()) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage d'abord.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " bank deposit|withdraw <montant>");
            return true;
        }
        String action = args[1].toLowerCase();
        Double amount = parseAmount(args[2]);
        if (amount == null || amount <= 0) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Montant invalide.");
            return true;
        }
        boolean ok = switch (action) {
            case "deposit" -> ctx.economy().deposit(player.getUniqueId(), amount);
            case "withdraw" -> ctx.economy().withdraw(player.getUniqueId(), amount);
            default -> false;
        };
        if (!ok) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Opération refusée (fonds ou anti-abuse).");
            return true;
        }
        player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "OK.");
        return true;
    }

    private boolean handleJob(Player player, String label, String[] args) {
        PlayerData data = ctx.characters().data(player.getUniqueId());
        if (!data.hasCharacter()) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage d'abord.");
            return true;
        }

        if (args.length == 1) {
            player.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Métier: " + ChatColor.WHITE + ctx.jobs().get(player.getUniqueId()));
            return true;
        }

        String action = args[1].toLowerCase();
        if (action.equals("list")) {
            player.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Métiers: " + ChatColor.WHITE + "UNEMPLOYED, TAXI, BAR, GROCERY, LAWYER, JOURNALIST, REAL_ESTATE, MECHANIC, POLICE, BAKER, DEALER, STRIP_CLUB, EMS");
            return true;
        }
        if (action.equals("set")) {
            if (!player.hasPermission("streetliferp.admin.job.set")) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " job set <job>");
                return true;
            }
            JobType type;
            try {
                type = JobType.valueOf(args[2].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Job invalide.");
                return true;
            }
            ctx.jobs().set(player.getUniqueId(), type);
            player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Métier mis à jour: " + type);
            return true;
        }
        player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " job [list|set]");
        return true;
    }

    private boolean handleFine(Player player, String label, String[] args) {
        PlayerData data = ctx.characters().data(player.getUniqueId());
        if (!data.hasCharacter()) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage d'abord.");
            return true;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("pay")) {
            player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " fine pay");
            return true;
        }
        if (!data.hasFine()) {
            player.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Aucune amende.");
            return true;
        }
        boolean ok = ctx.justice().payFine(player.getUniqueId());
        if (!ok) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Paiement refusé (fonds ou anti-abuse).");
            return true;
        }
        player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Amende payée.");
        return true;
    }

    private boolean handlePolice(Player player, String label, String[] args) {
        if (!player.hasPermission("streetliferp.police")) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
            return true;
        }
        if (args.length < 3) {
            player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " police cuff|uncuff|search|fine <joueur> ...");
            return true;
        }
        String action = args[1].toLowerCase();
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur introuvable.");
            return true;
        }
        if (!ctx.characters().data(target.getUniqueId()).hasCharacter()) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Ce joueur n'a pas de personnage.");
            return true;
        }

        String actorRp = ctx.characters().rpNameOrNull(player.getUniqueId());
        if (actorRp == null) {
            player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage d'abord.");
            return true;
        }

        switch (action) {
            case "cuff" -> ctx.justice().setCuffed(target, true, ctx.config().prefix(), actorRp);
            case "uncuff" -> ctx.justice().setCuffed(target, false, ctx.config().prefix(), actorRp);
            case "fine" -> {
                if (args.length < 4) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " police fine <joueur> <montant> [raison]");
                    return true;
                }
                Double amount = parseAmount(args[3]);
                if (amount == null || amount <= 0) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Montant invalide.");
                    return true;
                }
                String reason = args.length >= 5 ? String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length)) : "Amende";
                ctx.justice().issueFine(target.getUniqueId(), actorRp, amount, reason);
                player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Amende émise.");
                target.sendMessage(ctx.config().prefix() + ChatColor.RED + "Vous avez reçu une amende: " + ctx.economy().format(amount) + " (" + reason + ").");
                target.sendMessage(ChatColor.DARK_GRAY + "Payer: /" + label + " fine pay");
            }
            default -> player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " police cuff|uncuff|fine ...");
        }
        return true;
    }

    private boolean handleAdmin(Player player, String label, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " admin character delete <joueur>");
            player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "       /" + label + " admin item give <joueur> <item>");
            player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "       /" + label + " admin cuff <joueur>");
            return true;
        }
        String area = args[1].toLowerCase();
        if (area.equals("character")) {
            if (args.length < 4 || !args[2].equalsIgnoreCase("delete")) {
                player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " admin character delete <joueur>");
                return true;
            }
            if (!player.hasPermission("streetliferp.admin.character.delete")) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur introuvable (doit être en ligne).");
                return true;
            }

            boolean deleted = ctx.characters().delete(target.getUniqueId());
            if (!deleted) {
                player.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Ce joueur n'a pas de personnage à supprimer.");
                return true;
            }

            target.sendMessage(ctx.config().prefix() + ChatColor.RED + "Votre personnage a été supprimé par un admin.");
            target.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Recréez-le: /" + label + " character create <prenom> <nom>");

            String targetRp = ctx.characters().rpNameOrNull(target.getUniqueId());
            player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Personnage supprimé pour " + (targetRp != null ? targetRp : target.getUniqueId()) + ".");
            ctx.auditLog().logInfo(player.getName() + " deleted character for " + target.getUniqueId());
            return true;
        }
        if (area.equals("item")) {
            if (!player.hasPermission("streetliferp.admin.item.give")) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                return true;
            }
            if (args.length < 5 || !args[2].equalsIgnoreCase("give")) {
                player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " admin item give <joueur> <item>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[3]);
            if (target == null) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur introuvable (doit être en ligne).");
                return true;
            }
            SpecialItemType type;
            try {
                type = SpecialItemType.valueOf(args[4].toUpperCase());
            } catch (IllegalArgumentException e) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Item invalide.");
                return true;
            }
            target.getInventory().addItem(ctx.items().create(type));
            String targetRp = ctx.characters().rpNameOrNull(target.getUniqueId());
            player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Item donné: " + type.name() + " -> " + (targetRp != null ? targetRp : target.getUniqueId()));
            return true;
        }
        if (area.equals("cuff")) {
            if (!player.hasPermission("streetliferp.admin.cuff.toggle")) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " admin cuff <joueur>");
                return true;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur introuvable (doit être en ligne).");
                return true;
            }
            boolean next = !ctx.justice().isCuffed(target.getUniqueId());
            String actorRp = ctx.characters().rpNameOrNull(player.getUniqueId());
            ctx.justice().setCuffed(target, next, ctx.config().prefix(), actorRp != null ? actorRp : player.getUniqueId().toString());
            String targetRp = ctx.characters().rpNameOrNull(target.getUniqueId());
            player.sendMessage(ctx.config().prefix() + ChatColor.GREEN + "Menottes " + (next ? "activées" : "retirées") + " pour " + (targetRp != null ? targetRp : target.getName()) + ".");
            return true;
        }

        player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /" + label + " admin character delete <joueur>");
        player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "       /" + label + " admin item give <joueur> <item>");
        player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "       /" + label + " admin cuff <joueur>");
        return true;
    }

    private Double parseAmount(String raw) {
        try {
            return Double.parseDouble(raw.replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> out = new ArrayList<>();
            out.add("reload");
            out.add("character");
            out.add("id");
            out.add("money");
            out.add("pay");
            out.add("bank");
            out.add("job");
            out.add("police");
            out.add("fine");
            out.add("admin");
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("character")) {
            return List.of("create");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("id")) {
            return List.of("card");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("bank")) {
            return List.of("deposit", "withdraw");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("job")) {
            return List.of("list", "set");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("fine")) {
            return List.of("pay");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("police")) {
            return List.of("cuff", "uncuff", "fine");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("job") && args[1].equalsIgnoreCase("set")) {
            return List.of("UNEMPLOYED", "TAXI", "BAR", "GROCERY", "LAWYER", "JOURNALIST", "REAL_ESTATE", "MECHANIC", "POLICE", "BAKER", "DEALER", "STRIP_CLUB", "EMS");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
            return List.of("character", "item", "cuff");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("character")) {
            return List.of("delete");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("item")) {
            return List.of("give");
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("item") && args[2].equalsIgnoreCase("give")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return names;
        }
        if (args.length == 5 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("item") && args[2].equalsIgnoreCase("give")) {
            List<String> out = new ArrayList<>();
            for (SpecialItemType t : SpecialItemType.values()) out.add(t.name());
            return out;
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("character") && args[2].equalsIgnoreCase("delete")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return names;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("admin") && args[1].equalsIgnoreCase("cuff")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                names.add(p.getName());
            }
            return names;
        }
        return List.of();
    }
}
