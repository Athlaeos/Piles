package me.athlaeos.piles.domain;

import me.athlaeos.piles.piles.PileType;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public String serializeItems() {
        StringJoiner joiner = new StringJoiner("<item>");
        for (ItemStack item : items) {
            joiner.add(Utils.serialize(item));
        }
        return joiner.toString();
    }

    public ItemDisplay getDisplay(){
        return Bukkit.getEntity(entity) instanceof ItemDisplay display ? display : null;
    }

    public void addItem(ItemStack item){
        items.add(item);
    }

    public ItemStack removeItem(){
        return items.removeLast();
    }

    public boolean isValid() {
        ItemDisplay display = getDisplay();
        return display != null && display.isValid();
    }
}
