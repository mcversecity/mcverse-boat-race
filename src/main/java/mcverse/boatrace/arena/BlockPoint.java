package mcverse.boatrace.arena;

public record BlockPoint(int x, int y, int z) {
    @Override
    public String toString() {
        return x + ", " + y + ", " + z;
    }
}
