package me.athlaeos.piles.commands;

import me.athlaeos.piles.Piles;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommandManager implements TabExecutor {
	private static final Map<String, Command> commands = new HashMap<>();

	public CommandManager() {
		PluginCommand command = Piles.getInstance().getCommand("piles");
		if (command != null) command.setExecutor(this);

		commands.put("help", new HelpCommand());
		commands.put("create", new CreatePileCommand());
		commands.put("delete", new DeletePileCommand());
		commands.put("toggle", new TogglePilePlacementCommand());
		commands.put("resourcepack", new ResourcePackCommand());
	}

	public static Map<String, Command> getCommands() {
		return commands;
	}

	@Override
	public boolean onCommand(CommandSender sender, org.bukkit.command.Command cmd, String name, String[] args) {
		if (args.length == 0) {
			Utils.sendMessage(sender, Utils.chat(String.format("&dPiles v%s by Athlaeos", Piles.getInstance().getDescription().getVersion())));
			Utils.sendMessage(sender, Utils.chat("&7/piles help"));
			return true;
		}

		Command command = commands.get(args[0].toLowerCase());
		if (command == null) {
			Utils.sendMessage(sender, Utils.chat(Piles.getPluginConfig().getString("message_invalid_command", "")));
			return true;
		}

		if (!command.hasPermission(sender)){
			Utils.sendMessage(sender, Utils.chat(Piles.getPluginConfig().getString("message_no_command_permission", "")));
			return true;
		}

		if (!command.execute(sender, args)) Utils.sendMessage(sender, Utils.chat(command.getFailureMessage(args)));
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, org.bukkit.command.Command cmd, String name, String[] args) {
		if (args.length == 1) {
			List<String> allowedCommands = new ArrayList<>();
			for (String arg : commands.keySet()) {
				Command command = commands.get(arg);
				if (command.hasPermission(sender)) {
					allowedCommands.add(arg);
				}
			}
			return allowedCommands;
		} else if (args.length > 1) {
			Command command = commands.get(args[0]);
			if (command == null || !command.hasPermission(sender)) return Command.noSubcommandArgs();
			return command.getSubcommandArgs(sender, args);
		}
		return null;
	}
}
