package me.athlaeos.piles.commands;

import me.athlaeos.piles.Piles;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HelpCommand implements Command {
	private final String[] helpFormat = new String[]{
			"&8&m                              ",
			"&7Command: &e%command%",
			"&7Description: &e%description%",
			"&7Permission: &e%permissions%"
	};

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		Map<Integer, List<String>> pages;
		List<String> helpLines = new ArrayList<>();
		
		for (Command c : CommandManager.getCommands().values()) {
			if (c.hasPermission(sender)) {
				for (String line : helpFormat){
					helpLines.add(line
							.replace("%description%", c.getDescription())
							.replace("%permissions%", String.join("|", c.getRequiredPermissions()))
							.replace("%command%", c.getCommand()));
				}
			}
		}

		pages = Utils.paginate(helpFormat.length * 3, helpLines);
		
		if (pages.isEmpty()) return true;

		int page = 0;
		if (args.length >= 2){
			try {
				page = Integer.parseInt(args[1]);
			} catch (NumberFormatException nfe) {
				Utils.sendMessage(sender, Utils.chat(Piles.getPluginConfig().getString("message_invalid_number")));
				return true;
			}
		}

		for (String line : pages.get(Math.max(0, Math.min(pages.size() - 1, page)))) {
			sender.sendMessage(Utils.chat(line));
		}
		Utils.chat("&8&m                                             ");
		sender.sendMessage(Utils.chat(String.format("&8[&e1&8/&e%s&8]", pages.size())));
		return true;
	}

	@Override
	public String getFailureMessage(String[] args) {
		return "/piles help <page>";
	}

	@Override
	public String getCommand() {
		return "/piles help <page>";
	}

	@Override
	public String[] getRequiredPermissions() {
		return new String[]{"piles.help"};
	}

	@Override
	public boolean hasPermission(CommandSender sender) {
		return sender.hasPermission("piles.help");
	}

	@Override
	public String getDescription() {
		return Piles.getPluginConfig().getString("command_description_help");
	}

	@Override
	public List<String> getSubcommandArgs(CommandSender sender, String[] args) {
		if (args.length == 2) return List.of("1", "2", "3", "...");
		return Command.noSubcommandArgs();
	}
}
