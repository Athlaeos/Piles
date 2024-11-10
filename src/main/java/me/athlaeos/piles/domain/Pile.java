package me.athlaeos.piles.domain;

import me.athlaeos.piles.Piles;
import me.athlaeos.piles.piles.PileType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Pile {
    private final String pile;
    private final UUID entity;
    private final UUID owner;
    private final Pos position;
    private final List<ItemStack> items;

    public Pile(PileType pile, Pos pos, UUID owner, ItemDisplay display, List<ItemStack> items){
        this.pile = pile.getType();
        this.position = pos;
        this.owner = owner;
        this.items = items;
        this.entity = display.getUniqueId();
    }

    public String getPile() {
        return pile;
    }

    public UUID getOwner() {
        return owner;
    }

    public Pos getPosition() {
        return position;
    }

    public List<ItemStack> getItems() {
        return new ArrayList<>(items);
    }

    public ItemDisplay getDisplay(){
        Entity e = Piles.getInstance().getServer().getEntity(entity);
        return e == null ? null : (ItemDisplay) e;
    }

    public void addItem(ItemStack item){
        items.add(item);
    }

    public ItemStack removeItem(){
        return items.removeLast();
    }
}
