package mcverse.boatrace.integration;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import mcverse.boatrace.arena.Arena;
import mcverse.boatrace.arena.Cuboid;
import org.bukkit.World;

import java.io.IOException;
import java.util.Locale;

public final class WorldGuardArenas {
    private WorldGuardArenas() {
    }

    public static String id(String arena, String part) {
        return ("mcvbr_" + arena + "_" + part).toLowerCase(Locale.ROOT);
    }

    public static void upsert(World world, String id, Cuboid cuboid) throws Exception {
        RegionManager regions = manager(world);
        if (regions == null) {
            throw new IOException("WorldGuard has no regions for " + world.getName());
        }
        ProtectedCuboidRegion region = new ProtectedCuboidRegion(
                id,
                BlockVector3.at(cuboid.minX(), cuboid.minY(), cuboid.minZ()),
                BlockVector3.at(cuboid.maxX(), cuboid.maxY(), cuboid.maxZ()));
        regions.addRegion(region);
        regions.save();
    }

    public static void delete(World world, String id) throws Exception {
        RegionManager regions = manager(world);
        if (regions == null) {
            return;
        }
        regions.removeRegion(id);
        regions.save();
    }

    public static void replaceCheckpoints(World world, Arena arena) throws Exception {
        RegionManager regions = manager(world);
        if (regions == null) {
            throw new IOException("WorldGuard has no regions for " + world.getName());
        }
        for (int i = 1; i <= Arena.MAX_CHECKPOINTS; i++) {
            regions.removeRegion(id(arena.name(), "cp" + i));
        }
        int index = 1;
        for (Cuboid checkpoint : arena.checkpoints()) {
            regions.addRegion(new ProtectedCuboidRegion(
                    id(arena.name(), "cp" + index),
                    BlockVector3.at(checkpoint.minX(), checkpoint.minY(), checkpoint.minZ()),
                    BlockVector3.at(checkpoint.maxX(), checkpoint.maxY(), checkpoint.maxZ())));
            index++;
        }
        regions.save();
    }

    public static void deleteArena(World world, Arena arena) throws Exception {
        RegionManager regions = manager(world);
        if (regions == null) {
            return;
        }
        regions.removeRegion(id(arena.name(), "spectate"));
        regions.removeRegion(id(arena.name(), "track"));
        regions.removeRegion(id(arena.name(), "finish"));
        for (int i = 1; i <= Arena.MAX_CHECKPOINTS; i++) {
            regions.removeRegion(id(arena.name(), "cp" + i));
        }
        regions.save();
    }

    private static RegionManager manager(World world) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt(world));
    }
}
