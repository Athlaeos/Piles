package me.athlaeos.piles.domain;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;

public class Pos {
    private String world;
    private int x;
    private int y;
    private int z;

    public Pos(Block block){
        this(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
    }

    public Pos(String world, int x, int y, int z){
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Block getBlock() {
        World world = Bukkit.getWorld(this.world);
        return world != null ? world.getBlockAt(x, y, z) : null;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getWorld() { return world; }

    public void setWorld(String world) { this.world = world; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setZ(int z) { this.z = z; }

    @Override
    public String toString() {
        return world + "," + x + "," + y + "," + z;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof Pos pos && x == pos.x && y == pos.y && z == pos.z && world.equals(pos.world);
    }
}
