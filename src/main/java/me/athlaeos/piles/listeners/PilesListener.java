package me.athlaeos.piles.listeners;

import me.athlaeos.piles.PileRegistry;
import me.athlaeos.piles.Piles;
import me.athlaeos.piles.domain.Pile;
import me.athlaeos.piles.piles.PileType;
import me.athlaeos.piles.utils.Timer;
import me.athlaeos.piles.utils.Utils;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.stream.Collectors;

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
    public void onInteract(PlayerInteractEvent e){
        if (e.useItemInHand() == Event.Result.DENY || e.getHand() == EquipmentSlot.OFF_HAND || !Timer.isCooldownPassed(e.getPlayer().getUniqueId(), "delay_item_placement")) return;
        RayTraceResult result = e.getPlayer().getWorld().rayTraceEntities(e.getPlayer().getEyeLocation(), e.getPlayer().getEyeLocation().getDirection(), 5, 0.3, en -> en instanceof ItemDisplay d && PileRegistry.isPile(d));
        // full destroy or placement of pile

        float direction = get8WayDirection(e.getPlayer().getEyeLocation());
        Timer.setCooldown(e.getPlayer().getUniqueId(), 50, "delay_item_placement");
        if (result != null && result.getHitEntity() != null){
            // interacting with existing pile
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK){
                ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
                if (canPlace(e.getPlayer(), result.getHitEntity().getLocation().getBlock(), BlockFace.UP)){
                    // do not trust compiler warning saying hand is hand != null is always false, it isn't and this depends on server software like papermc or purpur
                    if (hand != null && !hand.getType().isAir()) {
                        if (PileRegistry.placePile(e.getPlayer(), hand, (ItemDisplay) result.getHitEntity(), direction)){
                            e.getPlayer().swingMainHand();
                            if (e.getPlayer().getGameMode() != GameMode.CREATIVE){
                                if (hand.getAmount() == 1) e.getPlayer().getInventory().setItemInMainHand(null);
                                else hand.setAmount(hand.getAmount() - 1);
                            }
                        }
                    } else {
                        ItemStack taken = PileRegistry.takeFromPile((ItemDisplay) result.getHitEntity(), e.getPlayer());
                        if (taken != null && !taken.getType().isAir()) Utils.addItem(e.getPlayer(), taken, true);
                    }
                    e.setCancelled(true);
                }
            } else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK){
                if (canTake(e.getPlayer(), result.getHitEntity().getLocation().getBlock())) {
                    PileRegistry.destroyPile(e.getPlayer(), (ItemDisplay) result.getHitEntity());
                    e.setCancelled(true);
                }
            }
        } else {
            if (e.getClickedBlock() == null) return;
            Pile pile = PileRegistry.fromBlock(e.getClickedBlock());
            if (pile == null){
                if (e.getBlockFace() != BlockFace.UP || e.getAction().toString().contains("LEFT_")) return; // must be sneaking and right-clicking to place pile
                Block b = e.getClickedBlock().getRelative(BlockFace.UP);
                if (!b.getRelative(BlockFace.DOWN).getType().isSolid()) return; // block below must be solid
                ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
                if (e.getPlayer().isSneaking() && hand != null && !hand.getType().isAir() && canPlace(e.getPlayer(), e.getClickedBlock(), BlockFace.UP)) {
                    if (PileRegistry.placePile(e.getPlayer(), hand, b, direction)){
                        e.getPlayer().swingMainHand();
                        if (e.getPlayer().getGameMode() != GameMode.CREATIVE){
                            if (hand.getAmount() == 1) e.getPlayer().getInventory().setItemInMainHand(null);
                            else hand.setAmount(hand.getAmount() - 1);
                        }
                        e.setCancelled(true);
                    }
                }
            } else {
                // interacting with existing pile
                if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK){
                    ItemStack hand = e.getPlayer().getInventory().getItemInMainHand();
                    if (!hand.getType().isAir()) {
                        if (!e.getPlayer().isSneaking()){
                            if (canPlace(e.getPlayer(), e.getClickedBlock().getLocation().subtract(0, 1, 0).getBlock(), BlockFace.UP)){
                                if (PileRegistry.placePile(e.getPlayer(), hand, e.getClickedBlock(), direction)){
                                    e.getPlayer().swingMainHand();
                                    if (e.getPlayer().getGameMode() != GameMode.CREATIVE){
                                        if (hand.getAmount() == 1) e.getPlayer().getInventory().setItemInMainHand(null);
                                        else hand.setAmount(hand.getAmount() - 1);
                                    }
                                    e.setCancelled(true);
                                }
                            }
                        } else if (canTake(e.getPlayer(), e.getClickedBlock())) {
                            ItemStack taken = PileRegistry.takeFromPile(pile.getDisplay(), e.getPlayer());
                            if (taken != null && !taken.getType().isAir()) {
                                Utils.addItem(e.getPlayer(), taken, true);
                                e.setCancelled(true);
                            }
                        }
                    } else if (canTake(e.getPlayer(), e.getClickedBlock())) {
                        ItemStack taken = PileRegistry.takeFromPile(pile.getDisplay(), e.getPlayer());
                        if (taken != null && !taken.getType().isAir()) {
                            Utils.addItem(e.getPlayer(), taken, true);
                            e.setCancelled(true);
                        }
                    }
                } else if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK){
                    if (canTake(e.getPlayer(), e.getClickedBlock())) {
                        PileRegistry.destroyPile(e.getPlayer(), e.getClickedBlock());
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExplosion(BlockExplodeEvent e){
        if (e.isCancelled()) return;
        for (Block b : e.blockList()){
            Block above = b.getRelative(BlockFace.UP);
            Pile pile = PileRegistry.fromBlock(above);
            if (pile != null) PileRegistry.destroyPile(null, above);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onExplosion(EntityExplodeEvent e){
        if (e.isCancelled()) return;
        for (Block b : e.blockList()){
            Block above = b.getRelative(BlockFace.UP);
            Pile pile = PileRegistry.fromBlock(above);
            if (pile != null) PileRegistry.destroyPile(null, above);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent e){
        if (e.isCancelled()) return;
        Block above = e.getBlock().getRelative(BlockFace.UP);
        Pile pile = PileRegistry.fromBlock(above);
        if (pile != null) PileRegistry.destroyPile(e.getPlayer(), above);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onFallBlock(EntityChangeBlockEvent event) {
        if (event.isCancelled()) return;
        if (event.getEntity() instanceof FallingBlock fallingBlock) {
            Pile pile = PileRegistry.fromBlock(fallingBlock.getLocation().getBlock());
            if (pile != null) PileRegistry.destroyPile(null, fallingBlock.getLocation().getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockForm(BlockFormEvent e){
        if (e.isCancelled()) return;
        Pile pile = PileRegistry.fromBlock(e.getBlock());
        if (pile != null && e.getBlock().getType().isOccluding()) PileRegistry.destroyPile(null, e.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onStructureForm(StructureGrowEvent e){
        if (e.isCancelled()) return;
        for (BlockState b : e.getBlocks()){
            Pile pile = PileRegistry.fromBlock(b.getBlock());
            if (pile != null && b.getType().isOccluding()) PileRegistry.destroyPile(null, b.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonMove(BlockPistonExtendEvent e){
        if (e.isCancelled()) return;
        if (e.getBlock().getBlockData() instanceof Directional d){
            for (Block b : e.getBlocks().stream().map(b -> b.getRelative(d.getFacing())).collect(Collectors.toSet())){
                Pile pile = PileRegistry.fromBlock(b);
                if (pile != null && b.getType().isOccluding()) PileRegistry.destroyPile(null, b);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPistonMove(BlockPistonRetractEvent e){
        if (e.isCancelled()) return;
        if (e.getBlock().getBlockData() instanceof Directional d){
            for (Block b : e.getBlocks().stream().map(b -> b.getRelative(d.getFacing().getOppositeFace())).collect(Collectors.toSet())){
                Pile pile = PileRegistry.fromBlock(b);
                if (pile != null && b.getType().isOccluding()) PileRegistry.destroyPile(null, b);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent e){
        if (e.isCancelled()) return;
        Pile pile = PileRegistry.fromBlock(e.getBlock());
        if (pile != null && e.getBlock().getType().isOccluding()) e.setCancelled(true); // do not place solid blocks on piles
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockPlace(BlockMultiPlaceEvent e){
        if (e.isCancelled()) return;
        for (BlockState state : e.getReplacedBlockStates()) {
            Pile pile = PileRegistry.fromBlock(state.getBlock());
            if (pile != null) {
                e.setCancelled(true); // do not place blocks on piles
                return;
            }
        }
    }

    private boolean canPlace(Player player, Block against, BlockFace face){
        ItemStack hand = player.getInventory().getItemInMainHand();
        PileType type = PileRegistry.typeFromItem(hand);
        if (type == null) return true;
        if (PileRegistry.preventionPreConditions(player, against.getRelative(face).getLocation(), type, null, hand)) return false;
        BlockPlaceEvent event = new BlockPlaceEvent(against.getRelative(face), against.getState(), against, hand, player, true, EquipmentSlot.HAND);
        Piles.getInstance().getServer().getPluginManager().callEvent(event);
        return !event.isCancelled();
    }

    private boolean canTake(Player player, Block from){
        BlockBreakEvent event = new BlockBreakEvent(from, player);
        Piles.getInstance().getServer().getPluginManager().callEvent(event);
        return !event.isCancelled();
    }
}
