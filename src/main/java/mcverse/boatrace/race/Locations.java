package mcverse.boatrace.race;

import mcverse.boatrace.arena.Pose;
import mcverse.boatrace.track.Sample;
import org.bukkit.Location;
import org.bukkit.World;

public final class Locations {
    private Locations() {
    }

    public static Location of(World world, Pose pose) {
        return new Location(world, pose.x(), pose.y(), pose.z(), pose.yaw(), pose.pitch());
    }

    public static Location of(World world, Sample sample) {
        return new Location(world, sample.x(), sample.y(), sample.z(), sample.yaw(), 0f);
    }

    public static Sample sample(Location location) {
        return new Sample(location.getX(), location.getY(), location.getZ(), location.getYaw());
    }

    public static Pose pose(Location location) {
        return new Pose(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }
}
