package me.athlaeos.piles.piles;

import me.athlaeos.piles.Piles;
import me.athlaeos.piles.domain.MinecraftVersion;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public abstract class PileType {
    private final String type;
    private final int maxSize;
    private final Material displayItem;
    private final int[] customModelData;
    private final boolean solid;
    private final String placementSound;
    private final String takeSound;
    private final String destroySound;

    protected PileType(String type, int maxSize, Material displayItem, boolean solid, String placementSound, String takeSound, String destroySound, int... customModelData){
        this.type = type;
        this.maxSize = maxSize;
        this.displayItem = displayItem;
        this.solid = solid;
        this.customModelData = customModelData;
        this.placementSound = placementSound;
        this.takeSound = takeSound;
        this.destroySound = destroySound;
    }

    public boolean isValid(){
        return customModelData.length == maxSize;
    }
    public abstract boolean canPlace(Block b);

    public String getType() { return type; }
    public int getMaxSize() { return maxSize; }
    public Material getDisplayItem() { return displayItem; }
    public int[] getCustomModelData() { return customModelData; }
    public boolean isSolid() { return solid; }
    public Sound getDestroySound() { return parse(destroySound); }
    public Sound getPlacementSound() { return parse(placementSound); }
    public Sound getTakeSound() {
        return parse(takeSound);
    }

    @SuppressWarnings("all")
    private Sound parse(String string){
        Sound sound = null;
        try {
            sound = Sound.valueOf(string);
        } catch (Throwable ignored){
            NamespacedKey key = NamespacedKey.fromString(string);
            if (key == null) return null;
            sound = Registry.SOUNDS.get(key);
        }
        return sound;
    }

    public abstract int priority();

    public ItemStack getFinalDisplay(int stackSize){
        if (!isValid()) {
            Piles.logWarning(type + " is not a valid pile, please stop using it!");
            return null;
        }
        ItemStack pileDisplay = new ItemStack(displayItem);
        ItemMeta meta = pileDisplay.getItemMeta();
        if (meta == null) return null;
        meta.setCustomModelData(customModelData[stackSize - 1]);
        pileDisplay.setItemMeta(meta);
        return pileDisplay;
    }

    public abstract boolean acceptsItem(ItemStack i);
}
