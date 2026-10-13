package mcverse.boatrace;

import mcverse.boatrace.arena.ArenaManager;
import mcverse.boatrace.arena.ArenaStore;
import mcverse.boatrace.arena.SelectionManager;
import mcverse.boatrace.command.BoatRaceCommand;
import mcverse.boatrace.command.Wand;
import mcverse.boatrace.config.PluginConfig;
import mcverse.boatrace.integration.WorldGuardSync;
import mcverse.boatrace.listener.RaceListener;
import mcverse.boatrace.race.BoatTracker;
import mcverse.boatrace.race.RaceManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BoatRacePlugin extends JavaPlugin {
    private PluginConfig config;
    private ArenaManager arenas;
    private SelectionManager selections;
    private RaceManager races;
    private BoatTracker boats;
    private Wand wand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = new PluginConfig(this);
        WorldGuardSync worldGuard = new WorldGuardSync(this);
        arenas = new ArenaManager(new ArenaStore(this), config, worldGuard);
        arenas.load();
        selections = new SelectionManager();
        boats = new BoatTracker(this);
        races = new RaceManager(arenas, boats, config);
        wand = new Wand(this);
        getServer().getPluginManager().registerEvents(new RaceListener(this), this);
        PluginCommand command = getCommand("mcvboatrace");
        if (command == null) {
            getLogger().severe("mcvboatrace is missing from plugin.yml");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        BoatRaceCommand executor = new BoatRaceCommand(this);
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getServer().getScheduler().runTaskTimer(this, races::tick, 1L, 1L);
        getServer().getScheduler().runTask(this, this::sweepLeftoverBoats);
        getLogger().info("Loaded " + arenas.names().size() + " arena(s).");
        if (!worldGuard.installed()) {
            getLogger().info("WorldGuard is not installed. Arenas still work, and regions will not be mirrored.");
        }
    }

    @Override
    public void onDisable() {
        if (races != null) {
            races.shutdown();
        }
        if (boats != null) {
            boats.removeAllTaggedLoaded();
        }
    }

    public void reloadPlugin() {
        config.load();
        races.dropIdle();
        for (String skipped : arenas.reloadIdle(races::isBusy)) {
            getLogger().warning("Skipped reload of arena " + skipped + " because a race is running.");
        }
    }

    public PluginConfig config() {
        return config;
    }

    public ArenaManager arenas() {
        return arenas;
    }

    public SelectionManager selections() {
        return selections;
    }

    public RaceManager races() {
        return races;
    }

    public BoatTracker boats() {
        return boats;
    }

    public Wand wand() {
        return wand;
    }

    private void sweepLeftoverBoats() {
        int removed = boats.removeAllTaggedLoaded();
        if (removed > 0) {
            getLogger().info("Removed " + removed + " leftover race boat(s).");
        }
    }
}
