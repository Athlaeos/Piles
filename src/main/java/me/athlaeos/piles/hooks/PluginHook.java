package me.athlaeos.piles.hooks;

import me.athlaeos.piles.Piles;

public abstract class PluginHook {
    private final boolean isPresent;
    private final String plugin;

    public PluginHook(String name){
        this.isPresent = Piles.getInstance().getServer().getPluginManager().getPlugin(name) != null;
        this.plugin = name;
    }

    public boolean isPresent(){
        return isPresent;
    }

    public abstract void whenPresent();

    public String getPlugin() {
        return plugin;
    }
}
