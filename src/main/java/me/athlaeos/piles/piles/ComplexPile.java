package me.athlaeos.piles.piles;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class ComplexPile extends PileType {
    protected final ItemStack pileItem;
    public ComplexPile(String type, ItemStack pileItem, int maxSize, boolean solid, Material displayItem, String placementSound, String takeSound, String destroySound, int... customModelData) {
        super(type, maxSize, displayItem, solid, placementSound, takeSound, destroySound, customModelData);
        this.pileItem = pileItem;
    }

    @Override
    public boolean canPlace(Block b) {
        return b.getType().isAir();
    }

    @Override
    public int priority() {
        return 10000;
    }

    @Override
    public boolean acceptsItem(ItemStack i) {
        return i.isSimilar(pileItem);
    }

    public ItemStack getPileItem() {
        return pileItem;
    }
}
