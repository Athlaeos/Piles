package me.athlaeos.piles.listeners;

import me.athlaeos.piles.PileRegistry;
import me.athlaeos.piles.domain.Pile;
import me.athlaeos.piles.utils.Timer;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BlockSupport;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

public class PilesListener implements Listener {

    private float get8WayDirection(Location direction){
        float yaw = direction.getYaw() + 180;
        if (yaw < 22.5 || yaw >= 315+22.5) return 0; // south
        else if (yaw < 45+22.5) return 45; // southwest
        else if (yaw < 90+22.5) return 90; // west
        else if (yaw < 135+22.5) return 135; // northwest
        else if (yaw < 180+22.5) return 180; // north
        else if (yaw < 225+22.5) return 225; // northeast
        else if (yaw < 270+22.5) return 270; // east
        else return 315; // southeast
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event){
        Block block = event.getClickedBlock();
        Action action = event.getAction();
        Player player = event.getPlayer();
        Location eyeLocation = player.getEyeLocation();
        if (event.useItemInHand() == Event.Result.DENY
                || action == Action.PHYSICAL
                || event.getHand() == EquipmentSlot.OFF_HAND
                || !Timer.isCooldownPassed(player.getUniqueId(), "delay_item_placement")) {
            return;
        }

        boolean rightClick = event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK;
        Timer.setCooldown(event.getPlayer().getUniqueId(), 50, "delay_item_placement");
        RayTraceResult result = player.getWorld().rayTraceEntities(eyeLocation, eyeLocation.getDirection(), 5, 0.3, PileRegistry::isPile);
        float direction = get8WayDirection(eyeLocation);

        // interacting with existing pile entity
        if (result != null && result.getHitEntity() != null) {
            ItemDisplay display = (ItemDisplay) result.getHitEntity();
            block = display.getLocation().getBlock();
            if (rightClick) {
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType().isAir()) {
                    takeFromPile(player, display);
                } else {
                    addOrCreatePile(player, held, block, direction);
                }
            } else {
                PileRegistry.destroyPile(player, display);
            }
            event.setCancelled(true);
            return;
        }

        // If no found entity, rely on the block
        if (block == null) return;

        Pile pile = PileRegistry.fromBlock(block);
        if (pile == null) {
            // No found pile, must be adding/creating
            if (!rightClick || !player.isSneaking() || event.getBlockFace() != BlockFace.UP) return;
            block = block.getRelative(BlockFace.UP);
            if (!block.getRelative(BlockFace.DOWN).getBlockData().isFaceSturdy(BlockFace.UP, BlockSupport.CENTER)) return; // block below must be sturdy
            event.setCancelled(addOrCreatePile(player, player.getInventory().getItemInMainHand(), block, direction));
            return;
        }

        // interacting with existing pile
        ItemDisplay display = pile.getDisplay();
        if (display == null) return; // pile display must exist

        if (rightClick) {
            ItemStack hand = player.getInventory().getItemInMainHand();
            if (!hand.getType().isAir()) {
                if (player.isSneaking()) {
                    addOrCreatePile(player, hand, display, direction);
                } else {
                    takeFromPile(player, display);
                }
            } else {
                takeFromPile(player, display);
            }
        } else {
            PileRegistry.destroyPile(player, block);
        }
        event.setCancelled(true);
    }

    public void addOrCreatePile(Player player, ItemStack held, ItemDisplay display, float direction) {
        if (PileRegistry.placePile(player, held, display, direction)){
            onAdded(player, held);
        }
    }

    public boolean addOrCreatePile(Player player, ItemStack held, Block block, float direction) {
        if (!held.getType().isAir() && PileRegistry.placePile(player, held, block, direction)){
            onAdded(player, held);
            return true;
        }
        return false;
    }

    public void takeFromPile(Player player, ItemDisplay display) {
        ItemStack taken = PileRegistry.takeFromPile(display, player);
        if (taken != null && !taken.getType().isAir()) {
            Utils.addItem(player, taken, true);
        }
    }

    private void onAdded(Player player, ItemStack held) {
        player.swingMainHand();
        if (player.getGameMode() != GameMode.CREATIVE){
            held.setAmount(held.getAmount() - 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(BlockExplodeEvent e){
        for (Block block : e.blockList()){
            Block above = block.getRelative(BlockFace.UP);
            if (PileRegistry.fromBlock(above) != null) {
                PileRegistry.destroyPile(null, above);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent e){
        Player cause = null;
        if (e.getEntity() instanceof Player player) {
            cause = player;
        } else if (e.getEntity() instanceof TNTPrimed tnt && tnt.getSource() instanceof Player player) {
            cause = player;
        }

        for (Block block : e.blockList()){
            Block above = block.getRelative(BlockFace.UP);
            if (PileRegistry.fromBlock(above) != null) {
                PileRegistry.destroyPile(cause, above);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e){
        Block above = e.getBlock().getRelative(BlockFace.UP);
        if (PileRegistry.fromBlock(above) != null) {
            PileRegistry.destroyPile(e.getPlayer(), above);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof FallingBlock fallingBlock) {
            Block block = fallingBlock.getLocation().getBlock();
            if (PileRegistry.fromBlock(block) != null) {
                PileRegistry.destroyPile(null, block);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent e){
        Block block = e.getBlock();
        if (block.getType().isOccluding() && PileRegistry.fromBlock(block) != null) {
            PileRegistry.destroyPile(null, e.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureForm(StructureGrowEvent e){
        for (BlockState state : e.getBlocks()){
            Block block = state.getBlock();
            if (state.getType().isOccluding() && PileRegistry.fromBlock(block) != null) {
                PileRegistry.destroyPile(null, block);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonMove(BlockPistonExtendEvent e){
        if (!(e.getBlock().getBlockData() instanceof Directional d)) {
            return;
        }

        BlockFace face = d.getFacing();
        for (Block block : e.getBlocks()) {
            block = block.getRelative(face);
            if (block.getType().isOccluding() && PileRegistry.fromBlock(block) != null) {
                PileRegistry.destroyPile(null, block);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonMove(BlockPistonRetractEvent e){
        if (!(e.getBlock().getBlockData() instanceof Directional d)) {
            return;
        }

        BlockFace face = d.getFacing().getOppositeFace();
        for (Block block : e.getBlocks()) {
            block = block.getRelative(face);
            if (block.getType().isOccluding() && PileRegistry.fromBlock(block) != null) {
                PileRegistry.destroyPile(null, block);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e){
        Block block = e.getBlock();
        if (block.getType().isOccluding() && PileRegistry.fromBlock(block) != null) {
            e.setCancelled(true); // do not place solid blocks on piles
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockMultiPlaceEvent e){
        for (BlockState state : e.getReplacedBlockStates()) {
            if (PileRegistry.fromBlock(state.getBlock()) != null) {
                e.setCancelled(true); // do not place blocks on piles
                return;
            }
        }
    }
}
