package me.athlaeos.piles.hooks;

import me.athlaeos.piles.resourcepacks.ResourcePack;

public class RPMHook extends PluginHook {

    public RPMHook() {
        super("ResourcePackManager");
    }

    @Override
    public void whenPresent() {
        ResourcePack.downloadDefault();

        new RPMInitializer();
    }
}
