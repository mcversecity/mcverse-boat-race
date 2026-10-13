package mcverse.boatrace.integration;

import mcverse.boatrace.arena.Arena;
import mcverse.boatrace.arena.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public final class WorldGuardSync {
    public enum Result {
        UPDATED,
        NOT_INSTALLED,
        WORLD_UNLOADED,
        FAILED
    }

    private final JavaPlugin plugin;

    public WorldGuardSync(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean installed() {
        return plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
    }

    public Result upsert(String arena, String part, Cuboid cuboid) {
        if (!installed()) {
            return Result.NOT_INSTALLED;
        }
        World world = Bukkit.getWorld(cuboid.world());
        if (world == null) {
            return Result.WORLD_UNLOADED;
        }
        try {
            WorldGuardArenas.upsert(world, WorldGuardArenas.id(arena, part), cuboid);
            return Result.UPDATED;
        } catch (Throwable ex) {
            plugin.getLogger().warning("Could not update WorldGuard region for " + arena + " " + part + ": " + ex.getMessage());
            return Result.FAILED;
        }
    }

    public Result syncCheckpoints(Arena arena) {
        if (!installed()) {
            return Result.NOT_INSTALLED;
        }
        if (arena.world() == null) {
            return Result.WORLD_UNLOADED;
        }
        World world = Bukkit.getWorld(arena.world());
        if (world == null) {
            return Result.WORLD_UNLOADED;
        }
        try {
            WorldGuardArenas.replaceCheckpoints(world, arena);
            return Result.UPDATED;
        } catch (Throwable ex) {
            plugin.getLogger().warning("Could not update checkpoint regions for " + arena.name() + ": " + ex.getMessage());
            return Result.FAILED;
        }
    }

    public Result deleteArena(Arena arena) {
        if (!installed() || arena.world() == null) {
            return installed() ? Result.WORLD_UNLOADED : Result.NOT_INSTALLED;
        }
        World world = Bukkit.getWorld(arena.world());
        if (world == null) {
            return Result.WORLD_UNLOADED;
        }
        try {
            WorldGuardArenas.deleteArena(world, arena);
            return Result.UPDATED;
        } catch (Throwable ex) {
            plugin.getLogger().warning("Could not delete WorldGuard regions for " + arena.name() + ": " + ex.getMessage());
            return Result.FAILED;
        }
    }

    public static String describe(Result result, String id) {
        return switch (result) {
            case UPDATED -> " WorldGuard region " + id + " updated.";
            case NOT_INSTALLED -> " WorldGuard is not installed, so only the arena file was saved.";
            case WORLD_UNLOADED -> " The world is not loaded, so the WorldGuard region was not updated.";
            case FAILED -> " The arena was saved, but the WorldGuard region could not be updated.";
        };
    }
}
