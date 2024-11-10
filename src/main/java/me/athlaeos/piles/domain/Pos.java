package me.athlaeos.piles.domain;

public class Pos {
    private String world;
    private int x;
    private int y;
    private int z;

    public Pos(String world, int x, int y, int z){
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getWorld() { return world; }

    public void setWorld(String world) { this.world = world; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setZ(int z) { this.z = z; }
}
