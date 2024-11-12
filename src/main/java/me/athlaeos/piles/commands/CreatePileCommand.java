package me.athlaeos.piles.commands;

import me.athlaeos.piles.PileRegistry;
import me.athlaeos.piles.Piles;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class CreatePileCommand implements Command{
    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)){
            sender.sendMessage(Utils.chat("&cYou must be a player holding an item in order to do this"));
            return true;
        }
        ItemStack held = p.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) {
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_hand_item_required", ""));
            return true;
        }
        if (args.length < 10) return false;
        PileRegistry.PileTypeSelector selector = PileRegistry.getRegisteredSelectors().get(args[1]);
        if (selector == null) {
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_invalid_type", "").replace("%type%", args[1]));
            return true;
        }
        String name = args[2];
        if (PileRegistry.getPileType(name) != null){
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_already_exists", "").replace("%name%", name));
            return true;
        }
        Material displayType = null;
        try {
            displayType = Material.valueOf(args[3].toUpperCase());
        } catch (IllegalArgumentException ignored) {}
        if (displayType == null) {
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_invalid_type", "").replace("%type%", args[3]));
            return true;
        }
        int maxSize = -1;
        try {
            maxSize = Integer.parseInt(args[4]);
        } catch (IllegalArgumentException ignored){}
        if (maxSize <= 0) {
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_invalid_number", ""));
            return true;
        }
        boolean solid = args[5].equalsIgnoreCase("true");
        String placement = validate(args[6]);
        String destroy = validate(args[7]);
        String take = validate(args[8]);
        if (placement == null){
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_invalid_type", "").replace("%type%", args[6]));
            return true;
        }
        if (destroy == null){
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_invalid_type", "").replace("%type%", args[7]));
            return true;
        }
        if (take == null){
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_invalid_type", "").replace("%type%", args[8]));
            return true;
        }
        int[] data = parseModelData(args[9], maxSize);
        if (data.length == 0) {
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_invalid_type", "").replace("%type%", args[9]));
            return true;
        }
        if (data.length != maxSize){
            Utils.sendMessage(p, Piles.getPluginConfig().getString("message_data_mismatch", "").replace("%amount%", String.valueOf(maxSize)));
            return true;
        }
        String[] otherArgs = args.length >= 11 ? Arrays.copyOfRange(args, 10, args.length) : new String[0];
        PileRegistry.register(selector.createPile(name, held, displayType, solid, maxSize, data, placement, take, destroy, otherArgs));

        Utils.sendMessage(p, "&aPile created: " + name);
        Utils.sendMessage(p, "&7    Item Required: &a" + selector.requirementString(held));
        Utils.sendMessage(p, "&7    Max Size: &a" + maxSize);
        Utils.sendMessage(p, "&7    Solid: &a" + (solid ? "Yes" : "No"));
        Utils.sendMessage(p, "&7    Has the following custom model data per stage: ");
        for (int i = 1; i <= maxSize; i++){
            Utils.sendMessage(p, String.format("&7        %d: &e%d", i, data[i - 1]));
        }
        return true;
    }

    private int[] parseModelData(String data, int size){
        if (data.contains("-")) {
            // parse range
            String[] rangeArgs = data.split("-");
            if (rangeArgs.length != 2) return new int[0];
            try {
                int lowerBound = Integer.parseInt(rangeArgs[0]);
                int upperBound = Integer.parseInt(rangeArgs[1]);
                return IntStream.rangeClosed(lowerBound, upperBound).toArray();
            } catch (IllegalArgumentException ignored) { }
        } else if (data.contains(",")) {
            String[] dataArgs = data.split(",");
            try {
                return Arrays.stream(dataArgs).mapToInt(Integer::parseInt).toArray();
            } catch (IllegalArgumentException ignored) { }
        } else {
            try {
                int startingPoint = Integer.parseInt(data);
                return IntStream.rangeClosed(startingPoint, startingPoint + size - 1).toArray();
            } catch (IllegalArgumentException ignored) { }
        }
        return new int[0];
    }

    private String validate(String sound){
        try {
            Sound.valueOf(sound.toUpperCase());
            return sound;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    @Override
    public String getFailureMessage(String[] args) {
        return "&c/piles create <type> <name> <display> <max size> <solid?> <placement sound> <destruction sound> <take sound> <custom model datas>";
    }

    @Override
    public String getDescription() {
        return Piles.getPluginConfig().getString("command_description_create", "");
    }

    @Override
    public String getCommand() {
        return "/piles create <type> <name> <display> <max size> <solid?> <placement sound> <destruction sound> <take sound> <custom model datas>";
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{"piles.create"};
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender instanceof Player && sender.hasPermission("piles.create");
    }

    @Override
    public List<String> getSubcommandArgs(CommandSender sender, String[] args) {
        if (args.length == 2) return PileRegistry.getRegisteredSelectors().keySet().stream().toList();
        if (args.length == 3) return List.of("<name>");
        if (args.length == 4){
            List<String> list = new ArrayList<>(List.of("<display_material>"));
            list.addAll(Arrays.stream(Material.values()).map(Material::toString).map(String::toLowerCase).toList());
            return list;
        }
        if (args.length == 5) return List.of("<maxsize>");
        if (args.length == 6) return List.of("<solid?>" , "true", "false");
        if (args.length >= 7 && args.length <= 9){
            List<String> list = new ArrayList<>(List.of(switch (args.length) {
                case 7 -> "<placementsound>";
                case 8 -> "<destructionsound>";
                case 9 -> "<takesound>";
                default -> "what";
            }));
            list.addAll(Arrays.stream(Sound.values()).map(Sound::toString).map(String::toLowerCase).toList());
            return list;
        }
        if (args.length == 10) return List.of("<custom_model_datas_by_range_or_separated_by_commas>");
        return List.of("<custom_args>");
    }
}
