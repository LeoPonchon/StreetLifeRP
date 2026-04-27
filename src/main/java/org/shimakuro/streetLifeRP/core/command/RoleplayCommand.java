package org.shimakuro.streetLifeRP.core.command;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.shimakuro.streetLifeRP.core.StreetLifeRPContext;
import org.shimakuro.streetLifeRP.data.PlayerData;

public final class RoleplayCommand implements CommandExecutor {
    private final StreetLifeRPContext ctx;

    public RoleplayCommand(StreetLifeRPContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Cette commande est réservée aux joueurs.");
            return true;
        }

        String message = String.join(" ", args);
        String name = command.getName().toLowerCase();

        return switch (name) {
            case "phone" -> {
                PlayerData data = ctx.characters().data(player.getUniqueId());
                if (!data.hasCharacter()) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                String num = ctx.phone().ensureNumber(player.getUniqueId());
                player.sendMessage(ctx.config().prefix() + ChatColor.YELLOW + "Numéro: " + ChatColor.WHITE + num);
                yield true;
            }
            case "sms" -> {
                if (!player.hasPermission("streetliferp.chat.sms")) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                    yield true;
                }
                PlayerData data = ctx.characters().data(player.getUniqueId());
                if (!data.hasCharacter()) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Crée ton personnage d'abord.");
                    yield true;
                }
                if (args.length < 2) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.GRAY + "Usage: /sms <joueur> <message>");
                    yield true;
                }

                Player target = ctx.plugin().getServer().getPlayerExact(args[0]);
                if (target == null) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Joueur introuvable.");
                    yield true;
                }
                if (!ctx.characters().data(target.getUniqueId()).hasCharacter()) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Ce joueur n'a pas de personnage.");
                    yield true;
                }

                String raw = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
                int maxLen = ctx.config().raw().getInt("chat.max_message_length", 200);
                yield ctx.phone().sendSms(player, target, raw, ctx.config().prefix(), maxLen);
            }
            case "me" -> {
                if (!player.hasPermission("streetliferp.chat.me")) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                    yield true;
                }
                yield ctx.chat().sendMe(player, message, ctx.config().prefix());
            }
            case "do" -> {
                if (!player.hasPermission("streetliferp.chat.do")) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                    yield true;
                }
                yield ctx.chat().sendDo(player, message, ctx.config().prefix());
            }
            case "ooc" -> {
                if (!player.hasPermission("streetliferp.chat.ooc")) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                    yield true;
                }
                yield ctx.chat().sendOoc(player, message, ctx.config().prefix());
            }
            case "twt" -> {
                if (!player.hasPermission("streetliferp.chat.tweet")) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                    yield true;
                }
                yield ctx.chat().sendTweet(player, message, ctx.config().prefix());
            }
            case "call911" -> {
                if (!player.hasPermission("streetliferp.emergency.call")) {
                    player.sendMessage(ctx.config().prefix() + ChatColor.RED + "Permission manquante.");
                    yield true;
                }
                yield ctx.chat().sendEmergencyCall(player, message, ctx.config().prefix());
            }
            default -> true;
        };
    }
}
