package mcverse.boatrace.arena;

public final class Selection {
    private String world;
    private BlockPoint pos1;
    private BlockPoint pos2;

    public String world() {
        return world;
    }

    public BlockPoint pos1() {
        return pos1;
    }

    public BlockPoint pos2() {
        return pos2;
    }

    public void setPos1(String world, BlockPoint point) {
        if (this.world != null && !this.world.equals(world)) {
            pos2 = null;
        }
        this.world = world;
        this.pos1 = point;
    }

    public void setPos2(String world, BlockPoint point) {
        if (this.world != null && !this.world.equals(world)) {
            pos1 = null;
        }
        this.world = world;
        this.pos2 = point;
    }

    public boolean complete() {
        return world != null && pos1 != null && pos2 != null;
    }

    public Cuboid toCuboid() {
        return Cuboid.between(world, pos1, pos2);
    }
}
