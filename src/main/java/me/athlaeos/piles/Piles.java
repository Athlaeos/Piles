package me.athlaeos.piles;

import me.athlaeos.piles.commands.CommandManager;
import me.athlaeos.piles.config.ConfigManager;
import me.athlaeos.piles.config.ConfigUpdater;
import me.athlaeos.piles.hooks.PluginHook;
import me.athlaeos.piles.hooks.RPMHook;
import me.athlaeos.piles.listeners.PilesListener;
import me.athlaeos.piles.resourcepacks.Host;
import me.athlaeos.piles.resourcepacks.ResourcePack;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Piles extends JavaPlugin {

    private static Piles instance;
    private static YamlConfiguration pluginConfig;
    private static final Map<Class<? extends PluginHook>, PluginHook> activeHooks = new HashMap<>();

    {
        instance = this;
    }

    public static Piles getInstance() {
        return instance;
    }

    @Override
    public void onLoad(){
        //registerHook(new RPMHook());
    }

    @Override
    public void onEnable() {
        pluginConfig = saveAndUpdateConfig("config.yml");
        save("piletypes.json");
        new CommandManager();
        PileRegistry.load();
        ResourcePack.tryStart();

        for (PluginHook hook : activeHooks.values()) hook.whenPresent();

        getServer().getPluginManager().registerEvents(new PilesListener(), this);
    }

    @Override
    public void onDisable() {
        PileRegistry.save();
        Host.stop();
    }

    public static void logInfo(String message){
        instance.getServer().getLogger().info("[Piles] " + message);
    }

    public static void logWarning(String warning){
        instance.getServer().getLogger().warning("[Piles] " + warning);
    }
    public static void logFine(String warning){
        instance.getServer().getLogger().fine("[Piles] " + warning);
        Utils.sendMessage(instance.getServer().getConsoleSender(), "&a[Piles] " + warning);
    }

    public static void logSevere(String help){
        instance.getServer().getLogger().severe("[Piles] " + help);
    }

    public static YamlConfiguration getPluginConfig() {
        return pluginConfig;
    }

    private YamlConfiguration saveAndUpdateConfig(String config){
        save(config);
        updateConfig(config);
        return saveConfig(config);
    }

    public YamlConfiguration saveConfig(String name){
        save(name);
        return ConfigManager.saveConfig(name).get();
    }

    public void save(String name){
        File file = new File(this.getDataFolder(), name);
        if (!file.exists()) this.saveResource(name, false);
    }

    private void updateConfig(String name){
        File configFile = new File(getDataFolder(), name);
        try {
            ConfigUpdater.update(instance, name, configFile, new ArrayList<>());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void registerHook(PluginHook hook){
        if (hook.isPresent()) {
            activeHooks.put(hook.getClass(), hook);
            logInfo("Initialized plugin hook with " + hook.getPlugin());
        }
    }

    public static boolean isHookFunctional(Class<? extends PluginHook> hook){
        return activeHooks.containsKey(hook);
    }
    @SuppressWarnings("unchecked")
    public static <T extends PluginHook> T getHook(Class<T> hook){
        return (T) activeHooks.get(hook);
    }
}
