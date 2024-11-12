package me.athlaeos.piles.commands;

import me.athlaeos.piles.PileRegistry;
import me.athlaeos.piles.Piles;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class DeletePileCommand implements Command{
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 3) return false;
        String name = args[2];
        if (PileRegistry.getPileType(name) == null){
            Utils.sendMessage(sender, Piles.getPluginConfig().getString("message_invalid_type", "").replace("%type%", name));
            return true;
        }
        if (PileRegistry.unregister(PileRegistry.getPileType(name))){
            Utils.sendMessage(sender, Piles.getPluginConfig().getString("message_pile_deleted"));
        } else Utils.sendMessage(sender, Piles.getPluginConfig().getString("message_invalid_type", "").replace("%type%", name));
        return true;
    }

    @Override
    public String getFailureMessage(String[] args) {
        return "&c/piles delete <pile>";
    }

    @Override
    public String getDescription() {
        return Piles.getPluginConfig().getString("command_description_delete", "");
    }

    @Override
    public String getCommand() {
        return "/piles delete <pile>";
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{"piles.delete"};
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender instanceof Player && sender.hasPermission("piles.delete");
    }

    @Override
    public List<String> getSubcommandArgs(CommandSender sender, String[] args) {
        if (args.length == 3) return PileRegistry.getRegisteredPiles().keySet().stream().toList();
        return Command.noSubcommandArgs();
    }
}
