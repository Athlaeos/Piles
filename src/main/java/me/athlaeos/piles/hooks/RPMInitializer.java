package me.athlaeos.piles.hooks;

import com.magmaguy.resourcepackmanager.api.ResourcePackManagerAPI;

import java.io.File;

public class RPMInitializer {
    public RPMInitializer() {
        ResourcePackManagerAPI.registerResourcePack("Piles",
                "Piles" + File.separatorChar + "resourcepack",
                false,
                false,
                false,
                true,
                "");
    }
}
