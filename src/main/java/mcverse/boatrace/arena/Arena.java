package mcverse.boatrace.arena;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

public final class Arena {
    public static final int MAX_SLOTS = 32;
    public static final int MAX_CHECKPOINTS = 64;

    private final String name;
    private String world;
    private Pose spectatorSpawn;
    private Cuboid spectateArea;
    private Cuboid track;
    private Cuboid finish;
    private final List<Cuboid> checkpoints = new ArrayList<>();
    private final TreeMap<Integer, Pose> slots = new TreeMap<>();
    private int minPlayers;
    private int lobbySeconds;
    private int maxRaceSeconds;

    public Arena(String name, int minPlayers, int lobbySeconds, int maxRaceSeconds) {
        this.name = name;
        this.minPlayers = minPlayers;
        this.lobbySeconds = lobbySeconds;
        this.maxRaceSeconds = maxRaceSeconds;
    }

    public String name() {
        return name;
    }

    public String world() {
        return world;
    }

    public void setWorld(String world) {
        this.world = world;
    }

    public Pose spectatorSpawn() {
        return spectatorSpawn;
    }

    public void setSpectatorSpawn(Pose spectatorSpawn) {
        this.spectatorSpawn = spectatorSpawn;
    }

    public Cuboid spectateArea() {
        return spectateArea;
    }

    public void setSpectateArea(Cuboid spectateArea) {
        this.spectateArea = spectateArea;
    }

    public Cuboid track() {
        return track;
    }

    public void setTrack(Cuboid track) {
        this.track = track;
    }

    public Cuboid finish() {
        return finish;
    }

    public void setFinish(Cuboid finish) {
        this.finish = finish;
    }

    public List<Cuboid> checkpoints() {
        return Collections.unmodifiableList(checkpoints);
    }

    public void addCheckpoint(Cuboid cuboid) {
        checkpoints.add(cuboid);
    }

    public boolean removeCheckpoint(int index) {
        if (index < 0 || index >= checkpoints.size()) {
            return false;
        }
        checkpoints.remove(index);
        return true;
    }

    public void clearCheckpoints() {
        checkpoints.clear();
    }

    public TreeMap<Integer, Pose> slots() {
        return slots;
    }

    public int slotCount() {
        return slots.size();
    }

    public int minPlayers() {
        return minPlayers;
    }

    public void setMinPlayers(int minPlayers) {
        this.minPlayers = minPlayers;
    }

    public int lobbySeconds() {
        return lobbySeconds;
    }

    public void setLobbySeconds(int lobbySeconds) {
        this.lobbySeconds = lobbySeconds;
    }

    public int maxRaceSeconds() {
        return maxRaceSeconds;
    }

    public void setMaxRaceSeconds(int maxRaceSeconds) {
        this.maxRaceSeconds = maxRaceSeconds;
    }

    public List<String> missing() {
        List<String> missing = new ArrayList<>();
        if (world == null) {
            missing.add("world");
        }
        if (spectatorSpawn == null) {
            missing.add("spectator spawn");
        }
        if (spectateArea == null) {
            missing.add("spectate area");
        }
        if (track == null) {
            missing.add("track");
        }
        if (finish == null) {
            missing.add("finish");
        }
        if (checkpoints.isEmpty()) {
            missing.add("checkpoints");
        }
        if (slots.isEmpty()) {
            missing.add("start slots");
        }
        if (!slots.isEmpty() && minPlayers > slots.size()) {
            missing.add("min players is higher than start slots");
        }
        return missing;
    }
}
