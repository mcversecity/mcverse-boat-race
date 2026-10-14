package mcverse.boatrace.arena;

import mcverse.boatrace.config.PluginConfig;
import mcverse.boatrace.integration.WorldGuardSync;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class ArenaManager {
    public static final Pattern NAME = Pattern.compile("[a-z0-9_-]{1,24}");

    private final ArenaStore store;
    private final PluginConfig config;
    private final WorldGuardSync worldGuard;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<UUID, String> selected = new LinkedHashMap<>();

    public ArenaManager(ArenaStore store, PluginConfig config, WorldGuardSync worldGuard) {
        this.store = store;
        this.config = config;
        this.worldGuard = worldGuard;
    }

    public void load() {
        arenas.clear();
        arenas.putAll(store.load(config.minPlayers(), config.lobbySeconds(), config.maxRaceSeconds(), config.laps()));
    }

    public List<String> reloadIdle(Predicate<String> busy) {
        List<String> skipped = new ArrayList<>();
        Map<String, Arena> loaded = store.load(config.minPlayers(), config.lobbySeconds(), config.maxRaceSeconds(), config.laps());
        arenas.keySet().removeIf(name -> !loaded.containsKey(name) && !busy.test(name));
        loaded.forEach((name, arena) -> {
            if (busy.test(name)) {
                skipped.add(name);
            } else {
                arenas.put(name, arena);
            }
        });
        return skipped;
    }

    public Collection<String> names() {
        return List.copyOf(arenas.keySet());
    }

    public Collection<Arena> all() {
        return List.copyOf(arenas.values());
    }

    public Arena get(String name) {
        if (name == null) {
            return null;
        }
        return arenas.get(name.toLowerCase(Locale.ROOT));
    }

    public Arena create(String name) {
        String id = name.toLowerCase(Locale.ROOT);
        Arena arena = new Arena(id, config.minPlayers(), config.lobbySeconds(), config.maxRaceSeconds(), config.laps());
        arenas.put(id, arena);
        store.save(arenas.values());
        return arena;
    }

    public void delete(Arena arena) {
        worldGuard.deleteArena(arena);
        arenas.remove(arena.name());
        selected.entrySet().removeIf(entry -> entry.getValue().equals(arena.name()));
        store.save(arenas.values());
    }

    public void select(UUID playerId, Arena arena) {
        selected.put(playerId, arena.name());
    }

    public Arena selectedOrOnly(UUID playerId) {
        Arena chosen = get(selected.get(playerId));
        if (chosen != null) {
            return chosen;
        }
        if (arenas.size() == 1) {
            return arenas.values().iterator().next();
        }
        return null;
    }

    public List<Arena> containing(Location location) {
        List<Arena> matches = new ArrayList<>();
        for (Arena arena : arenas.values()) {
            if (arena.spectateArea() != null && contains(arena.spectateArea(), location)) {
                matches.add(arena);
            }
        }
        return matches;
    }

    public void setSpectatorSpawn(Arena arena, String world, Pose pose) {
        arena.setWorld(world);
        arena.setSpectatorSpawn(pose);
        save();
    }

    public WorldGuardSync.Result setSpectateArea(Arena arena, Cuboid cuboid) {
        arena.setWorld(cuboid.world());
        arena.setSpectateArea(cuboid);
        save();
        return worldGuard.upsert(arena.name(), "spectate", cuboid);
    }

    public WorldGuardSync.Result setTrack(Arena arena, Cuboid cuboid) {
        arena.setWorld(cuboid.world());
        arena.setTrack(cuboid);
        save();
        return worldGuard.upsert(arena.name(), "track", cuboid);
    }

    public WorldGuardSync.Result setFinish(Arena arena, Cuboid cuboid) {
        arena.setWorld(cuboid.world());
        arena.setFinish(cuboid);
        save();
        return worldGuard.upsert(arena.name(), "finish", cuboid);
    }

    public WorldGuardSync.Result addCheckpoint(Arena arena, Cuboid cuboid) {
        arena.setWorld(cuboid.world());
        arena.addCheckpoint(cuboid);
        save();
        return worldGuard.syncCheckpoints(arena);
    }

    public WorldGuardSync.Result removeCheckpoint(Arena arena, int index) {
        arena.removeCheckpoint(index);
        save();
        return worldGuard.syncCheckpoints(arena);
    }

    public WorldGuardSync.Result clearCheckpoints(Arena arena) {
        arena.clearCheckpoints();
        save();
        return worldGuard.syncCheckpoints(arena);
    }

    public void setSlot(Arena arena, int number, String world, Pose pose) {
        arena.setWorld(world);
        arena.slots().put(number, pose);
        save();
    }

    public boolean removeSlot(Arena arena, int number) {
        if (arena.slots().remove(number) == null) {
            return false;
        }
        save();
        return true;
    }

    public void setMinPlayers(Arena arena, int minPlayers) {
        arena.setMinPlayers(minPlayers);
        save();
    }

    public void setLobbySeconds(Arena arena, int seconds) {
        arena.setLobbySeconds(seconds);
        save();
    }

    public void setMaxRaceSeconds(Arena arena, int seconds) {
        arena.setMaxRaceSeconds(seconds);
        save();
    }

    public void setLaps(Arena arena, int laps) {
        arena.setLaps(laps);
        save();
    }

    public List<String> problems(Arena arena) {
        return ArenaValidator.problems(arena, arena.world() == null ? null : Bukkit.getWorld(arena.world()), config);
    }

    public static boolean contains(Cuboid cuboid, Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        return cuboid.contains(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    private void save() {
        store.save(arenas.values());
    }
}
