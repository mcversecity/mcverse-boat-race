package mcverse.boatrace.arena;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ArenaStore {
    private final File file;

    public ArenaStore(JavaPlugin plugin) {
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public Map<String, Arena> load(int defaultMinPlayers, int defaultLobby, int defaultMaxTime) {
        Map<String, Arena> arenas = new LinkedHashMap<>();
        if (!file.exists()) {
            return arenas;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yaml.getConfigurationSection("arenas");
        if (root == null) {
            return arenas;
        }
        for (String name : root.getKeys(false)) {
            ConfigurationSection section = root.getConfigurationSection(name);
            if (section == null) {
                continue;
            }
            Arena arena = new Arena(
                    name,
                    section.getInt("min-players", defaultMinPlayers),
                    section.getInt("lobby-seconds", defaultLobby),
                    section.getInt("max-race-seconds", defaultMaxTime));
            String world = section.getString("world");
            arena.setWorld(world);
            arena.setSpectatorSpawn(readPose(section.getConfigurationSection("spectator-spawn")));
            if (world != null) {
                arena.setSpectateArea(readCuboid(world, section.getConfigurationSection("spectate-area")));
                arena.setTrack(readCuboid(world, section.getConfigurationSection("track")));
                arena.setFinish(readCuboid(world, section.getConfigurationSection("finish")));
                for (Map<?, ?> raw : section.getMapList("checkpoints")) {
                    Cuboid cuboid = readCuboidMap(world, raw);
                    if (cuboid != null) {
                        arena.addCheckpoint(cuboid);
                    }
                }
            }
            for (Map<?, ?> raw : section.getMapList("slots")) {
                int number = number(raw.get("number")).intValue();
                if (number < 1) {
                    continue;
                }
                arena.slots().put(number, new Pose(
                        number(raw.get("x")).doubleValue(),
                        number(raw.get("y")).doubleValue(),
                        number(raw.get("z")).doubleValue(),
                        number(raw.get("yaw")).floatValue(),
                        number(raw.get("pitch")).floatValue()));
            }
            arenas.put(name, arena);
        }
        return arenas;
    }

    public void save(Collection<Arena> arenas) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().header("Boat race arenas. Edit them in game with /mcvboatrace.");
        for (Arena arena : arenas) {
            String path = "arenas." + arena.name();
            yaml.set(path + ".world", arena.world());
            yaml.set(path + ".min-players", arena.minPlayers());
            yaml.set(path + ".lobby-seconds", arena.lobbySeconds());
            yaml.set(path + ".max-race-seconds", arena.maxRaceSeconds());
            writePose(yaml, path + ".spectator-spawn", arena.spectatorSpawn());
            writeCuboid(yaml, path + ".spectate-area", arena.spectateArea());
            writeCuboid(yaml, path + ".track", arena.track());
            writeCuboid(yaml, path + ".finish", arena.finish());
            List<Map<String, Object>> checkpoints = new ArrayList<>();
            for (Cuboid checkpoint : arena.checkpoints()) {
                checkpoints.add(cuboidMap(checkpoint));
            }
            yaml.set(path + ".checkpoints", checkpoints);
            List<Map<String, Object>> slots = new ArrayList<>();
            arena.slots().forEach((number, pose) -> {
                Map<String, Object> slot = new LinkedHashMap<>();
                slot.put("number", number);
                slot.put("x", pose.x());
                slot.put("y", pose.y());
                slot.put("z", pose.z());
                slot.put("yaw", pose.yaw());
                slot.put("pitch", pose.pitch());
                slots.add(slot);
            });
            yaml.set(path + ".slots", slots);
        }
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                throw new IOException("Could not create " + file.getParentFile());
            }
            yaml.save(file);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save arenas.yml", ex);
        }
    }

    private static void writePose(YamlConfiguration yaml, String path, Pose pose) {
        if (pose == null) {
            return;
        }
        yaml.set(path + ".x", pose.x());
        yaml.set(path + ".y", pose.y());
        yaml.set(path + ".z", pose.z());
        yaml.set(path + ".yaw", pose.yaw());
        yaml.set(path + ".pitch", pose.pitch());
    }

    private static void writeCuboid(YamlConfiguration yaml, String path, Cuboid cuboid) {
        if (cuboid == null) {
            return;
        }
        yaml.set(path + ".min.x", cuboid.minX());
        yaml.set(path + ".min.y", cuboid.minY());
        yaml.set(path + ".min.z", cuboid.minZ());
        yaml.set(path + ".max.x", cuboid.maxX());
        yaml.set(path + ".max.y", cuboid.maxY());
        yaml.set(path + ".max.z", cuboid.maxZ());
    }

    private static Pose readPose(ConfigurationSection section) {
        if (section == null || !section.contains("x")) {
            return null;
        }
        return new Pose(
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch"));
    }

    private static Map<String, Object> cuboidMap(Cuboid cuboid) {
        Map<String, Object> min = new LinkedHashMap<>();
        min.put("x", cuboid.minX());
        min.put("y", cuboid.minY());
        min.put("z", cuboid.minZ());
        Map<String, Object> max = new LinkedHashMap<>();
        max.put("x", cuboid.maxX());
        max.put("y", cuboid.maxY());
        max.put("z", cuboid.maxZ());
        Map<String, Object> cuboidMap = new LinkedHashMap<>();
        cuboidMap.put("min", min);
        cuboidMap.put("max", max);
        return cuboidMap;
    }

    private static Cuboid readCuboidMap(String world, Map<?, ?> raw) {
        if (!(raw.get("min") instanceof Map<?, ?> min) || !(raw.get("max") instanceof Map<?, ?> max)) {
            return null;
        }
        return new Cuboid(
                world,
                number(min.get("x")).intValue(),
                number(min.get("y")).intValue(),
                number(min.get("z")).intValue(),
                number(max.get("x")).intValue(),
                number(max.get("y")).intValue(),
                number(max.get("z")).intValue());
    }
    private static Cuboid readCuboid(String world, ConfigurationSection section) {
        if (section == null || !section.contains("min.x") || !section.contains("max.x")) {
            return null;
        }
        return new Cuboid(
                world,
                section.getInt("min.x"),
                section.getInt("min.y"),
                section.getInt("min.z"),
                section.getInt("max.x"),
                section.getInt("max.y"),
                section.getInt("max.z"));
    }

    private static Number number(Object value) {
        return value instanceof Number parsed ? parsed : 0;
    }
}
