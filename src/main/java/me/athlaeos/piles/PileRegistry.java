package me.athlaeos.piles;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import me.athlaeos.piles.adapters.GsonAdapter;
import me.athlaeos.piles.adapters.ItemStackGSONAdapter;
import me.athlaeos.piles.config.ConfigManager;
import me.athlaeos.piles.domain.Pile;
import me.athlaeos.piles.domain.PileQuantityCounter;
import me.athlaeos.piles.piles.ComplexPile;
import me.athlaeos.piles.piles.CustomModelDataPile;
import me.athlaeos.piles.piles.PileType;
import me.athlaeos.piles.piles.SimplePile;
import me.athlaeos.piles.domain.Pos;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.persistence.PersistentDataType;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class PileRegistry {
    private static final NamespacedKey PILE_TYPE = new NamespacedKey(Piles.getInstance(), "pile_type");
    private static final NamespacedKey PILE_POSITION = new NamespacedKey(Piles.getInstance(), "pile_position");
    private static final NamespacedKey PILE_ITEMS = new NamespacedKey(Piles.getInstance(), "pile_items");
    private static final NamespacedKey PILE_OWNER = new NamespacedKey(Piles.getInstance(), "pile_owner");
    private static final NamespacedKey PLACEMENT_BLOCKER = new NamespacedKey(Piles.getInstance(), "pile_placement_blocker");
    
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(PileType.class, new GsonAdapter<PileType>("MOD_TYPE"))
            .registerTypeHierarchyAdapter(ConfigurationSerializable.class, new ItemStackGSONAdapter())
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .enableComplexMapKeySerialization()
            .create();
    private static final Map<String, PileType> registeredPiles = new HashMap<>();
    private static final Map<String, PileTypeSelector> registeredSelectors = new HashMap<>();
    private static final Map<Pos, Pile> activePilesByPosition = new HashMap<>();
    private static PileQuantityCounter quantityCounter = null;

    static {
        registerSelector(new PileTypeSelector() {
            @Override public String getIdentifier() { return "type_only"; }
            @Override public PileType createPile(String typeName, ItemStack from, Material display, boolean solid, int maxSize, int[] customModelDatas, String placementSound, String takeSound, String destroySound, String[] args) {return new SimplePile(typeName, from.getType(), maxSize, solid, display, placementSound, takeSound, destroySound, customModelDatas);}
            @Override public String requirementString(ItemStack item) { return Piles.getPluginConfig().getString("requirement_description_pile_simple", "").replace("%type%", item.getType().toString().toLowerCase()); }
        });
        registerSelector(new PileTypeSelector() {
            @Override public String getIdentifier() { return "exact"; }
            @Override public PileType createPile(String typeName, ItemStack from, Material display, boolean solid, int maxSize, int[] customModelDatas, String placementSound, String takeSound, String destroySound, String[] args) {return new ComplexPile(typeName, from, maxSize, solid, display, placementSound, takeSound, destroySound, customModelDatas);}
            @Override public String requirementString(ItemStack item) { return Piles.getPluginConfig().getString("requirement_description_pile_exact", "").replace("%item%", Utils.getItemName(item)); }
        });
        registerSelector(new PileTypeSelector() {
            @Override public String getIdentifier() { return "type_and_cmd"; }
            @Override public PileType createPile(String typeName, ItemStack from, Material display, boolean solid, int maxSize, int[] customModelDatas, String placementSound, String takeSound, String destroySound, String[] args) {return new CustomModelDataPile(typeName, from, maxSize, solid, display, placementSound, takeSound, destroySound, customModelDatas);}
            @Override public String requirementString(ItemStack item) {
                int data = Utils.getCustomModelData(item);
                return Piles.getPluginConfig().getString("requirement_description_pile_cmd", "").replace("%type%", item.getType().toString().toLowerCase()).replace("%data%", data < 0 ? "none" : String.valueOf(data));
            }
        });
    }

    public static void registerSelector(PileTypeSelector selector){
        registeredSelectors.put(selector.getIdentifier(), selector);
    }

    public static Map<String, PileTypeSelector> getRegisteredSelectors() {
        return new HashMap<>(registeredSelectors);
    }

    public static PileType typeFromItem(ItemStack item){
        List<PileType> pilesByPriority = new ArrayList<>(registeredPiles.values());
        pilesByPriority.sort(Comparator.comparingInt(PileType::priority));
        for (PileType pile : pilesByPriority){
            if (pile.acceptsItem(item)) return pile;
        }
        return null;
    }

    public static Pile fromBlock(Block b){
        Pos pos = new Pos(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        Pile pile = activePilesByPosition.get(pos);
        if (pile == null){
            for (Entity e : b.getWorld().getNearbyEntities(b.getLocation(), 1, 1, 1)){
                if (!(e instanceof ItemDisplay i) || !i.getPersistentDataContainer().has(PILE_TYPE, PersistentDataType.STRING)) continue;
                Pile p = fromEntity(i);
                if (p == null || !p.getPosition().getWorld().equalsIgnoreCase(b.getWorld().getName()) ||
                        p.getPosition().getX() != b.getX() || p.getPosition().getY() != b.getY() || p.getPosition().getZ() != b.getZ()) continue;
                pile = p;
                activePilesByPosition.put(pos, p);
            }
        }
        return pile;
    }

    public static Pile fromEntity(ItemDisplay display){
        if (!display.getPersistentDataContainer().has(PILE_TYPE, PersistentDataType.STRING)) return null;
        PileType pileType = getPileType(display.getPersistentDataContainer().get(PILE_TYPE, PersistentDataType.STRING));
        String encodedItems = display.getPersistentDataContainer().getOrDefault(PILE_ITEMS, PersistentDataType.STRING, "");
        String[] itemEntries = encodedItems.split("<item>");
        List<ItemStack> items = new ArrayList<>();
        if (!encodedItems.isEmpty()) for (String entry : itemEntries) items.add(Utils.deserialize(entry));
        if (pileType == null) {
            // destroy if pile type was deleted
            items.forEach(i -> display.getWorld().dropItemNaturally(display.getLocation().subtract(0.5, 0.5, 0.5), i));
            display.remove();
            if (display.getLocation().getBlock().getType() == Material.BARRIER) display.getLocation().getBlock().setType(Material.AIR);
            return null;
        }
        String uuid = display.getPersistentDataContainer().get(PILE_OWNER, PersistentDataType.STRING);
        UUID owner = uuid == null ? null : UUID.fromString(uuid);
        String encodedPos = display.getPersistentDataContainer().getOrDefault(PILE_POSITION, PersistentDataType.STRING, "");
        String[] posEntries = encodedPos.split(",");
        String world = posEntries[0];
        int x = Integer.parseInt(posEntries[1]);
        int y = Integer.parseInt(posEntries[2]);
        int z = Integer.parseInt(posEntries[3]);
        Pos realPos = new Pos(world, x, y, z);
        return new Pile(pileType, realPos, owner, display, items);
    }

    public static boolean isPile(ItemDisplay display){
        return display.getPersistentDataContainer().has(PILE_TYPE, PersistentDataType.STRING);
    }

    public static boolean placePile(Player by, ItemStack item, Block b, float rotation){
        PileType type = typeFromItem(item);
        if (type == null) return false;
        Pile existingPile = fromBlock(b);
        return placePile(by, item, type, existingPile, b.getLocation(), rotation);
    }

    public static boolean placePile(Player by, ItemStack item, ItemDisplay d, float rotation){
        PileType type = typeFromItem(item);
        if (type == null) return false;
        Pile existingPile = fromEntity(d);
        return placePile(by, item, type, existingPile, d.getLocation(), rotation);
    }

    private static boolean canPlace(Chunk chunk){
        int limit = Piles.getPluginConfig().getInt("chunk_pile_limit");
        int found = 0;
        for (Entity entity : chunk.getEntities()){
            if (entity instanceof ItemDisplay i && isPile(i)) found++;
        }
        return found < limit;
    }

    private static boolean placePile(Player by, ItemStack item, PileType type, Pile pile, Location l, float rotation){
        if (pile == null && (by == null || !by.isOp()) && !canPlace(l.getChunk())) {
            Utils.sendMessage(by, Piles.getPluginConfig().getString("message_pile_chunk_limit_reached", "").replace("%amount%", String.valueOf(Piles.getPluginConfig().getInt("chunk_pile_limit"))));
            return false;
        }
        if (l.getWorld() == null || (by != null && (!by.hasPermission("piles.place") || !canPlacePiles(by, pile == null) || hasPlacementBlocked(by))) || !type.acceptsItem(item) || !type.canPlace(l.getBlock())) return false;
        item = item.clone();
        item.setAmount(1);
        Pos pos = new Pos(l.getWorld().getName(), l.getBlockX(), l.getBlockY(), l.getBlockZ());
        ItemDisplay display;
        if (pile == null) {
            display = l.getWorld().spawn(l.add(0.5, 0.5, 0.5), ItemDisplay.class);
            display.setRotation(rotation, 0);
            display.getPersistentDataContainer().set(PILE_TYPE, PersistentDataType.STRING, type.getType());
            if (by != null) display.getPersistentDataContainer().set(PILE_OWNER, PersistentDataType.STRING, by.getUniqueId().toString());
            display.getPersistentDataContainer().set(PILE_POSITION, PersistentDataType.STRING, String.format("%s,%d,%d,%d", pos.getWorld(), pos.getX(), pos.getY(), pos.getZ()));
            pile = new Pile(type, pos, by == null ? null : by.getUniqueId(), display, new ArrayList<>());
            if (type.isSolid()) l.getWorld().getBlockAt(l).setType(Material.BARRIER);
            activePilesByPosition.put(pos, pile);
            if (by != null && !by.isOp()) quantityCounter.getPileQuantities().put(by.getUniqueId(), quantityCounter.getPileQuantities().getOrDefault(by.getUniqueId(), 0) + 1);
        } else {
            if (pile.getItems().size() >= type.getMaxSize()) return false; // pile is full
            display = pile.getDisplay();
            if (!display.getPersistentDataContainer().getOrDefault(PILE_TYPE, PersistentDataType.STRING, type.getType()).equals(type.getType())) return false;
        }
        if (type.getPlacementSound() != null) l.getWorld().playSound(l, type.getPlacementSound(), 1F, 1F);
        pile.addItem(item);
        display.getPersistentDataContainer().set(PILE_ITEMS, PersistentDataType.STRING, pile.getItems().stream().map(Utils::serialize).collect(Collectors.joining("<item>")));
        display.setItemStack(type.getFinalDisplay(pile.getItems().size()));
        return true;
    }

    public static void destroyPile(Player destroyer, Block b){
        Pile existingPile = fromBlock(b);
        if (existingPile == null) return;
        destroyPile(existingPile, destroyer);
    }

    public static void destroyPile(Player destroyer, ItemDisplay d){
        Pile existingPile = fromEntity(d);
        if (existingPile == null) return;
        destroyPile(existingPile, destroyer);
    }

    public static boolean canDestroy(Player p, Pile pile){
        boolean pileProtection = Piles.getPluginConfig().getBoolean("pile_protection", false);
        if (!pileProtection) return true;
        return p != null && (p.isOp() || pile.getOwner().equals(p.getUniqueId()));
    }

    private static void destroyPile(Pile pile, Player destroyer){
        if (!canDestroy(destroyer, pile)) return;
        ItemDisplay display = pile.getDisplay();
        PileType type = getPileType(pile.getPile());
        quantityCounter.getPileQuantities().put(pile.getOwner(), Math.max(0, quantityCounter.getPileQuantities().getOrDefault(pile.getOwner(), 0) - 1));
        if (type != null && type.isSolid()) display.getWorld().getBlockAt(display.getLocation()).setType(Material.AIR);
        if (type != null && type.getDestroySound() != null) display.getWorld().playSound(display.getLocation(), type.getDestroySound(), 1F, 1F);
        pile.getItems().forEach(i -> display.getWorld().dropItemNaturally(display.getLocation().subtract(0.5, 0.5, 0.5), i));
        display.remove();
        activePilesByPosition.remove(pile.getPosition());
    }

    public static ItemStack takeFromPile(ItemDisplay b, Player destroyer){
        Pile existingPile = fromEntity(b);
        if (existingPile == null || !canDestroy(destroyer, existingPile)) return null;
        ItemDisplay display = existingPile.getDisplay();
        PileType type = getPileType(existingPile.getPile());
        ItemStack item = existingPile.removeItem();
        if (existingPile.getItems().isEmpty()) destroyPile(existingPile, destroyer);
        else if (type.getTakeSound() != null) {
            display.getPersistentDataContainer().set(PILE_ITEMS, PersistentDataType.STRING, existingPile.getItems().stream().map(Utils::serialize).collect(Collectors.joining("<item>")));
            display.setItemStack(type.getFinalDisplay(existingPile.getItems().size()));
            display.getWorld().playSound(display.getLocation(), type.getTakeSound(), 1F, 1F);
        }
        return item;
    }

    public static void register(PileType pile){
        registeredPiles.put(pile.getType(), pile);
    }

    public static boolean unregister(PileType pile){
        return registeredPiles.remove(pile.getType()) != null;
    }

    public static PileType getPileType(String pile){
        return registeredPiles.get(pile);
    }

    public static int maxAllowedPiles(Player p){
        int def = Piles.getPluginConfig().getInt("default_pile_limit", 32);
        if (!p.isOp() && p.hasPermission("piles.extra")){
            for (PermissionAttachmentInfo permission : p.getEffectivePermissions()){
                if (permission.getPermission().startsWith("piles.extra")) {
                    String[] split = permission.getPermission().split("\\.");
                    if (split.length == 3) {
                        try {
                            def += Integer.parseInt(split[2]);
                        } catch (IllegalArgumentException ignored){
                            Piles.logSevere(p.getName() + "'s permission '" + permission.getPermission() + "' does not have a valid number attached! Please correct fast!");
                        }
                    }
                }
            }
        }
        return Math.max(0, def);
    }

    public static boolean canPlacePiles(Player p, boolean sendWarning){
        int limit = maxAllowedPiles(p);
        boolean allowed = p.isOp() || quantityCounter.getPileQuantities().getOrDefault(p.getUniqueId(), 0) <= limit;
        if (sendWarning && !allowed) Utils.sendMessage(p, Piles.getPluginConfig().getString("message_pile_limit_reached", "").replace("%amount%", String.valueOf(limit)));
        return allowed;
    }

    @SuppressWarnings("all")
    public static void load(){
        File f = new File(Piles.getInstance().getDataFolder(), "/piletypes.json");
        try {
            f.createNewFile();
        } catch (IOException ignored){}
        try (BufferedReader setsReader = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8))){
            PileType[] piles = gson.fromJson(setsReader, PileType[].class);
            if (piles == null) return;
            for (PileType pile : piles) {
                register(pile);
            }
        } catch (IOException | JsonSyntaxException exception){
            Piles.logSevere("Could not load items from piletypes.json, " + exception.getMessage());
        } catch (NoClassDefFoundError ignored){}

        File quantityFile = new File(Piles.getInstance().getDataFolder(), "/pile_quantities.json");
        try {
            quantityFile.createNewFile();
        } catch (IOException ignored){}
        try (BufferedReader setsReader = new BufferedReader(new FileReader(quantityFile, StandardCharsets.UTF_8))){
            PileQuantityCounter quantities = gson.fromJson(setsReader, PileQuantityCounter.class);
            if (quantities == null) quantityCounter = new PileQuantityCounter();
            else quantityCounter = quantities;
        } catch (IOException | JsonSyntaxException exception){
            Piles.logSevere("Could not load pile quantities from pile_quantities.json, " + exception.getMessage());
        } catch (NoClassDefFoundError ignored){}
    }

    @SuppressWarnings("all")
    public static void save(){
        File f = new File(Piles.getInstance().getDataFolder(), "/piletypes.json");
        try {
            f.createNewFile();
        } catch (IOException ignored){}
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(f, StandardCharsets.UTF_8))){
            JsonElement element = gson.toJsonTree(new ArrayList<>(registeredPiles.values()), new TypeToken<ArrayList<PileType>>(){}.getType());
            gson.toJson(element, writer);
            writer.flush();
        } catch (IOException | JsonSyntaxException exception){
            Piles.logSevere("Could not save items to piletypes.json, " + exception.getMessage());
        }

        File quantityFile = new File(Piles.getInstance().getDataFolder(), "/pile_quantities.json");
        try {
            quantityFile.createNewFile();
        } catch (IOException ignored){}
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(quantityFile, StandardCharsets.UTF_8))){
            gson.toJson(quantityCounter, writer);
            writer.flush();
        } catch (IOException | JsonSyntaxException exception){
            Piles.logSevere("Could not save pile quantities to pile_quantities.json, " + exception.getMessage());
        }
    }

    public interface PileTypeSelector {
        String getIdentifier();
        PileType createPile(String typeName, ItemStack from, Material display, boolean solid, int maxSize, int[] customModelDatas, String placementSound, String takeSound, String destroySound, String[] args);
        String requirementString(ItemStack item);
    }

    public static boolean hasPlacementBlocked(Player p){
        return p.getPersistentDataContainer().has(PLACEMENT_BLOCKER, PersistentDataType.BYTE);
    }

    /**
     * Toggles pile placement for the player.
     * @param p the player to toggle pile placement for
     * @return true if the player is now able to place piles, false if not
     */
    public static boolean togglePlacementBlocked(Player p){
        if (p.getPersistentDataContainer().has(PLACEMENT_BLOCKER, PersistentDataType.BYTE)) {
            p.getPersistentDataContainer().remove(PLACEMENT_BLOCKER);
            return true;
        } else {
            p.getPersistentDataContainer().set(PLACEMENT_BLOCKER, PersistentDataType.BYTE, (byte) 0);
            return false;
        }
    }

    public static Map<String, PileType> getRegisteredPiles() {
        return new HashMap<>(registeredPiles);
    }
}
