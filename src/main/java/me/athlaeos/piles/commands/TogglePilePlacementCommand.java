package me.athlaeos.piles.commands;

import me.athlaeos.piles.PileRegistry;
import me.athlaeos.piles.Piles;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TogglePilePlacementCommand implements Command{
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)){
            sender.sendMessage(Utils.chat("&cYou must be a player to perform this command"));
            return true;
        }

        if (PileRegistry.togglePlacementBlocked(p)) {
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_pile_placement_enabled", ""));
        } else {
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_pile_placement_disabled", ""));
        }
        return true;
    }

    @Override
    public String getFailureMessage(String[] args) {
        return "&c/piles toggle";
    }

    @Override
    public String getDescription() {
        return Piles.getPluginConfig().getString("command_description_toggle", "");
    }

    @Override
    public String getCommand() {
        return "/piles toggle";
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{"piles.toggle"};
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender instanceof Player && sender.hasPermission("piles.toggle");
    }

    @Override
    public List<String> getSubcommandArgs(CommandSender sender, String[] args) {
        return Command.noSubcommandArgs();
    }
}
