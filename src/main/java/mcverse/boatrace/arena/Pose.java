package mcverse.boatrace.arena;

public record Pose(double x, double y, double z, float yaw, float pitch) {
    public String format() {
        return Math.round(x) + ", " + Math.round(y) + ", " + Math.round(z) + " facing " + Math.round(yaw);
    }
}
