package me.athlaeos.piles.piles;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CustomModelDataPile extends PileType {
    protected final int pileData;
    protected final Material pileItem;
    public CustomModelDataPile(String type, ItemStack pileItem, int maxSize, boolean solid, Material displayItem, String placementSound, String takeSound, String destroySound, int... customModelData) {
        super(type, maxSize, displayItem, solid, placementSound, takeSound, destroySound, customModelData);
        this.pileItem = pileItem.getType();
        ItemMeta meta = pileItem.getItemMeta();
        this.pileData = meta == null || !meta.hasCustomModelData() ? -1 : meta.getCustomModelData();
    }

    @Override
    public boolean canPlace(Block b) {
        return b.getType().isAir();
    }

    @Override
    public int priority() {
        return 5000;
    }

    @Override
    public boolean acceptsItem(ItemStack i) {
        ItemMeta meta = i.getItemMeta();
        if (meta == null) return false;
        if (pileData < 0 == meta.hasCustomModelData()) return false; // if the pile expects no custom model data, but the meta has it, it doesn't accept the item
        if (!meta.hasCustomModelData()) return false; // at this point we've established a custom model data is required, if the item has none it doesn't accept the item
        return i.getType() == pileItem && pileData == meta.getCustomModelData(); // now custom model datas and item type must match
    }

    public int getPileData() {
        return pileData;
    }

    public Material getPileItem() {
        return pileItem;
    }
}
