package mcverse.boatrace.arena;

import mcverse.boatrace.config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ArenaValidator {
    private ArenaValidator() {
    }

    public static List<String> problems(Arena arena, World world, Set<Material> ice) {
        List<String> problems = new ArrayList<>(arena.missing());
        if (arena.world() != null && world == null) {
            problems.add("world is not loaded");
            return problems;
        }
        if (world == null) {
            return problems;
        }
        if (arena.spectatorSpawn() != null && arena.spectateArea() != null) {
            Pose spawn = arena.spectatorSpawn();
            if (!arena.spectateArea().contains(world.getName(), spawn.x(), spawn.y(), spawn.z())) {
                problems.add("spectator spawn is outside the spectate area");
            }
        }
        arena.slots().forEach((number, pose) -> {
            if (arena.track() != null && !arena.track().contains(world.getName(), pose.x(), pose.y(), pose.z())) {
                problems.add("slot " + number + " is outside the track");
            }
            Location location = new Location(world, pose.x(), pose.y(), pose.z());
            if (!Ice.under(location, ice)) {
                problems.add("slot " + number + " is not on ice");
            }
        });
        return problems;
    }

    public static final class Ice {
        private Ice() {
        }

        public static boolean under(Location location, Set<Material> ice) {
            Block feet = location.getBlock();
            if (ice.contains(feet.getType())) {
                return true;
            }
            return ice.contains(feet.getRelative(BlockFace.DOWN).getType());
        }

        public static boolean boatSpace(Location location) {
            return location.getBlock().isPassable();
        }
    }

    public static List<String> problems(Arena arena, World world, PluginConfig config) {
        return problems(arena, world, config.iceBlocks());
    }
}
