package me.athlaeos.piles.commands;

import me.athlaeos.piles.Piles;
import me.athlaeos.piles.config.ConfigManager;
import me.athlaeos.piles.resourcepacks.Host;
import me.athlaeos.piles.resourcepacks.ResourcePack;
import me.athlaeos.piles.resourcepacks.ResourcePackListener;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class ResourcePackCommand implements Command {

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2){
            return false;
        } else {
            if (args[1].equalsIgnoreCase("reload")){
                String newVersion = String.valueOf(System.currentTimeMillis());
                ConfigManager.getConfig("config.yml").set("resourcepack_version", newVersion);
                ConfigManager.getConfig("config.yml").save();
                ConfigManager.getConfig("config.yml").reload();
                ResourcePack.generate();
                ResourcePack.tryStart();
                Utils.sendMessage(sender, "&aReloaded resource pack!");
                return true;
            } else if (args[1].equalsIgnoreCase("resetplayer")){
                if (args.length > 2){
                    Collection<Player> targets = Utils.selectPlayers(sender, args[2]);

                    if (targets.isEmpty()){
                        Utils.sendMessage(sender, Utils.chat(Piles.getPluginConfig().getString("message_player_offline")));
                        return true;
                    }
                    for (Player target : targets){
                        ResourcePackListener.resetPackVersion(target);
                        ResourcePack.sendUpdate(target);
                    }
                    Utils.sendMessage(sender, "&aSent update!");
                    return true;
                } else return false;
            } else if (args[1].equalsIgnoreCase("stophost")) {
                ConfigManager.getConfig("config.yml").set("resourcepack_port", null);
                ConfigManager.getConfig("config.yml").set("server_ip", null);
                ConfigManager.getConfig("config.yml").save();
                ConfigManager.getConfig("config.yml").reload();
                Host.setData(null);
                Host.stop();
                Utils.sendMessage(sender, "&aHost stopped! Player resource packs will be updated upon re-logging");
            } else if (args[1].equalsIgnoreCase("setup")){
                if (args.length > 3){
                    if (!ResourcePack.downloadDefault()) {
                        Utils.sendMessage(sender, "&cCould not download default resource pack. View console for more details");
                        return true;
                    }
                    try {
                        String ip = args[2];
                        int port = Integer.parseInt(args[3]);

                        Host.setIp(ip);
                        Host.setPort(port);
                        ConfigManager.getConfig("config.yml").set("resourcepack_port", port);
                        ConfigManager.getConfig("config.yml").set("server_ip", ip);
                    } catch (IllegalArgumentException ignored){
                        Utils.sendMessage(sender, Utils.chat(Piles.getPluginConfig().getString("message_invalid_number")));
                        return true;
                    }
                    ConfigManager.getConfig("config.yml").set("resourcepack_version", String.valueOf(System.currentTimeMillis()));
                    ConfigManager.getConfig("config.yml").save();
                    ResourcePack.generate();
                    sender.sendMessage(Utils.chat(Piles.getPluginConfig().getString("message_resourcepack_setup")));
                } else {
                    ConfigManager.getConfig("config.yml").save();
                    if (!modify("resource-pack-sha1", ResourcePack.getResourcePackSha1())) {
                        Piles.logSevere("Could not set resource pack sha1 to server.properties");
                        Utils.sendMessage(sender, Piles.getPluginConfig().getString("message_resourcepack_failed"));
                        return true;
                    }

                    if (!modify("resource-pack", ResourcePack.getResourcePackURL())) {
                        Piles.logSevere("Could not set resource pack link to server.properties");
                        Utils.sendMessage(sender, Piles.getPluginConfig().getString("message_resourcepack_failed"));
                        return true;
                    }
                    sender.sendMessage(Utils.chat(Piles.getPluginConfig().getString("message_resourcepack_setup")));
                    return true;
                }
            } else if (args[1].equalsIgnoreCase("download")){
                if (!ResourcePack.downloadDefault()) {
                    Utils.sendMessage(sender, "&cCould not download default resource pack. View console for more details");
                    return true;
                }
                ResourcePack.generate();
                sender.sendMessage(Utils.chat("&a" + ResourcePack.getDefaultPackLink()));
                sender.sendMessage(Utils.chat(Piles.getPluginConfig().getString("message_resourcepack_downloaded")));
            } else return false;
        }
        return true;
    }

    @Override
    public String getFailureMessage(String[] args) {
        return "/piles resourcepack <setup/download/stophost/resetplayer/reload>";
    }

    @Override
    public String getDescription() {
        return Piles.getPluginConfig().getString("command_description_resourcepack");
    }

    @Override
    public String getCommand() {
        return "/piles resourcepack <setup/download/stophost/resetplayer/reload>";
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{"piles.resourcepack"};
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("piles.resourcepack");
    }

    @Override
    public List<String> getSubcommandArgs(CommandSender sender, String[] args) {
        if (args.length == 2) return List.of("resetplayer", "reload", "setup", "download", "stophost");
        if (args.length == 3 && args[1].equalsIgnoreCase("setup")) return List.of("<your_server_ip>");
        if (args.length == 4 && args[1].equalsIgnoreCase("setup")) return List.of("<available_port>", "30005");
        return null;
    }

    @SuppressWarnings("all")
    private boolean modify(String configKey, String configSetting) {
        File serverProperties;
        try {
            serverProperties = new File(Paths.get(Piles.getInstance().getDataFolder().getParentFile().getCanonicalFile().getParentFile().toString() + File.separatorChar + "server.properties").toString());
            if (!serverProperties.exists()) {
                Piles.logSevere("Could not find server.properties");
                return false;
            }
        } catch (Exception exception) {
            Piles.logSevere("Could not access server.properties");
            exception.printStackTrace();
            return false;
        }
        try {
            FileInputStream in = new FileInputStream(serverProperties);
            Properties props = new Properties();
            props.load(in);
            in.close();

            java.io.FileOutputStream out = new java.io.FileOutputStream(serverProperties);
            props.setProperty(configKey, configSetting);
            props.store(out, null);
            out.close();
        } catch (Exception ex) {
            Piles.logSevere("Could not write to server.properties");
            return false;
        }
        return true;
    }
}
