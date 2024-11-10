package me.athlaeos.piles.adapters;

import com.google.gson.*;
import me.athlaeos.piles.Piles;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;

/**
 * Provided by user Schottky on spigotmc.org
 */
public class ItemStackGSONAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    @Override
    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
        String element = Utils.serialize(src);
        if (element == null) throw new IllegalStateException("ItemStack could not be serialized");
        return new JsonPrimitive(element);
    }

    @Override
    public ItemStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        try {
            return Utils.deserialize(jsonElement.getAsString());
        } catch (Exception | Error e){
            Piles.logSevere("Could not load ItemStack: " + e.getMessage());
            return null;
        }
    }
}
