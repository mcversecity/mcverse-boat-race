package mcverse.boatrace.config;

import mcverse.boatrace.BoatRacePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Boat;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class PluginConfig {
    private final BoatRacePlugin plugin;
    private int lobbySeconds;
    private int fullLobbySeconds;
    private int minPlayers;
    private int maxRaceSeconds;
    private int laps;
    private int resultsSeconds;
    private int offTrackGraceTicks;
    private int recoveryCooldownTicks;
    private int trailSamples;
    private int trailIntervalTicks;
    private Boat.Type boatType;
    private Set<Material> iceBlocks;

    public PluginConfig(BoatRacePlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();
        lobbySeconds = positive(plugin.getConfig().getInt("lobby-seconds", 20), 20);
        fullLobbySeconds = positive(plugin.getConfig().getInt("full-lobby-seconds", 5), 5);
        minPlayers = positive(plugin.getConfig().getInt("min-players", 1), 1);
        maxRaceSeconds = positive(plugin.getConfig().getInt("max-race-seconds", 300), 300);
        laps = positive(plugin.getConfig().getInt("laps", 1), 1);
        resultsSeconds = positive(plugin.getConfig().getInt("results-seconds", 5), 5);
        offTrackGraceTicks = positive(plugin.getConfig().getInt("off-track-grace-ticks", 10), 10);
        recoveryCooldownTicks = positive(plugin.getConfig().getInt("recovery-cooldown-ticks", 20), 20);
        trailSamples = positive(plugin.getConfig().getInt("trail-samples", 40), 40);
        trailIntervalTicks = positive(plugin.getConfig().getInt("trail-interval-ticks", 5), 5);
        boatType = parseBoat(plugin.getConfig().getString("boat-type", "OAK"));
        iceBlocks = parseIce(plugin.getConfig().getStringList("ice-blocks"));
    }

    private Boat.Type parseBoat(String raw) {
        if (raw == null) {
            return Boat.Type.OAK;
        }
        try {
            return Boat.Type.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown boat-type '" + raw + "'. Using OAK.");
            return Boat.Type.OAK;
        }
    }

    private Set<Material> parseIce(List<String> names) {
        EnumSet<Material> ice = EnumSet.noneOf(Material.class);
        if (names.isEmpty()) {
            names = List.of("ICE", "PACKED_ICE", "BLUE_ICE", "FROSTED_ICE");
        }
        for (String name : names) {
            Material material = Material.matchMaterial(name);
            if (material == null || !material.isBlock()) {
                plugin.getLogger().warning("Unknown ice block '" + name + "'.");
                continue;
            }
            ice.add(material);
        }
        if (ice.isEmpty()) {
            ice.add(Material.ICE);
            ice.add(Material.PACKED_ICE);
            ice.add(Material.BLUE_ICE);
            ice.add(Material.FROSTED_ICE);
        }
        return Set.copyOf(ice);
    }

    private static int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
    }

    public int lobbySeconds() {
        return lobbySeconds;
    }

    public int fullLobbySeconds() {
        return fullLobbySeconds;
    }

    public int minPlayers() {
        return minPlayers;
    }

    public int maxRaceSeconds() {
        return maxRaceSeconds;
    }

    public int laps() {
        return laps;
    }

    public int resultsSeconds() {
        return resultsSeconds;
    }

    public int offTrackGraceTicks() {
        return offTrackGraceTicks;
    }

    public int recoveryCooldownTicks() {
        return recoveryCooldownTicks;
    }

    public int trailSamples() {
        return trailSamples;
    }

    public int trailIntervalTicks() {
        return trailIntervalTicks;
    }

    public Boat.Type boatType() {
        return boatType;
    }

    public Set<Material> iceBlocks() {
        return iceBlocks;
    }
}
