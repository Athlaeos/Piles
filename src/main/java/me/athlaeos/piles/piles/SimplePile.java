package me.athlaeos.piles.piles;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class SimplePile extends PileType {
    protected final Material pileItem;
    public SimplePile(String type, Material pileItem, int maxSize, boolean solid, Material displayItem, String placementSound, String takeSound, String destroySound, int... customModelData) {
        super(type, maxSize, displayItem, solid, placementSound, takeSound, destroySound, customModelData);
        this.pileItem = pileItem;
    }

    @Override
    public boolean canPlace(Block b) {
        return b.getType().isAir();
    }

    @Override
    public int priority() {
        return 1000;
    }

    @Override
    public boolean acceptsItem(ItemStack i) {
        ItemMeta meta = i.getItemMeta();
        if (meta == null || meta.hasCustomModelData()) return false;
        return i.getType() == pileItem;
    }

    public Material getPileItem() {
        return pileItem;
    }
}
