package mcverse.boatrace.track;

public record Sample(double x, double y, double z, float yaw) {
    public double distanceSquared(Sample other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public static Sample fromPose(mcverse.boatrace.arena.Pose pose) {
        return new Sample(pose.x(), pose.y(), pose.z(), pose.yaw());
    }
}
