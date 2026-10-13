package mcverse.boatrace.race;

import mcverse.boatrace.Messages;
import mcverse.boatrace.arena.Arena;
import mcverse.boatrace.arena.ArenaManager;
import mcverse.boatrace.config.PluginConfig;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class RaceManager {
    private final ArenaManager arenas;
    private final BoatTracker boats;
    private final PluginConfig config;
    private final Map<String, RaceSession> sessions = new HashMap<>();
    private final Map<UUID, Location> respawns = new HashMap<>();

    public RaceManager(ArenaManager arenas, BoatTracker boats, PluginConfig config) {
        this.arenas = arenas;
        this.boats = boats;
        this.config = config;
    }

    public void tick() {
        for (RaceSession session : sessions.values()) {
            if (session.busy()) {
                session.tick();
            }
        }
    }

    public void shutdown() {
        for (RaceSession session : sessions.values()) {
            session.shutdown();
        }
        sessions.clear();
        respawns.clear();
    }

    public void dropIdle() {
        sessions.entrySet().removeIf(entry -> {
            if (!entry.getValue().busy()) {
                entry.getValue().shutdown();
                return true;
            }
            return false;
        });
    }

    public boolean isBusy(String arena) {
        RaceSession session = sessions.get(arena);
        return session != null && session.busy();
    }

    public String stateName(String arena) {
        RaceSession session = sessions.get(arena);
        if (session == null) {
            return "idle";
        }
        return session.state().name().toLowerCase(Locale.ROOT);
    }

    public void join(Player player, Arena arena) {
        var problems = arenas.problems(arena);
        if (!problems.isEmpty()) {
            player.sendMessage(Messages.error("That arena is not ready: " + String.join(", ", problems)));
            return;
        }
        for (RaceSession session : sessions.values()) {
            if (!session.arena().name().equals(arena.name()) && session.holds(player.getUniqueId())) {
                player.sendMessage(Messages.error("Leave " + session.arena().name() + " before joining another race."));
                return;
            }
        }
        session(arena).join(player);
    }

    public void leave(Player player) {
        RaceSession session = holding(player.getUniqueId());
        if (session == null) {
            player.sendMessage(Messages.error("You are not in a race."));
            return;
        }
        session.leave(player);
    }

    public boolean forceStart(Arena arena) {
        var problems = arenas.problems(arena);
        if (!problems.isEmpty()) {
            return false;
        }
        return session(arena).forceStart();
    }

    public void stop(Arena arena, String reason) {
        RaceSession session = sessions.get(arena.name());
        if (session == null || !session.busy()) {
            return;
        }
        session.stop(reason);
    }

    public boolean running(Arena arena) {
        return isBusy(arena.name());
    }

    public void deleteArena(Arena arena) {
        RaceSession session = sessions.remove(arena.name());
        if (session != null) {
            session.stop("The arena was deleted.");
        }
        arenas.delete(arena);
    }

    public boolean isFrozen(Player player) {
        RaceSession session = holding(player.getUniqueId());
        return session != null && session.isFrozen(player);
    }

    public boolean shouldCancelExit(Player player) {
        if (boats.isExitAllowed(player.getUniqueId())) {
            return false;
        }
        for (RaceSession session : sessions.values()) {
            if (session.isLockedInBoat(player.getUniqueId())) {
                return true;
            }
        }
        return false;
    }

    public boolean shouldLock(Vehicle vehicle) {
        for (RaceSession session : sessions.values()) {
            if (session.locks(vehicle)) {
                return true;
            }
        }
        return false;
    }

    public boolean ownsBoat(UUID boatId) {
        for (RaceSession session : sessions.values()) {
            if (session.ownsBoat(boatId)) {
                return true;
            }
        }
        return false;
    }

    public boolean canEnter(Entity vehicle, Entity entering) {
        if (!boats.isManaged(vehicle)) {
            return true;
        }
        for (RaceSession session : sessions.values()) {
            UUID owner = session.boatOwner(vehicle.getUniqueId());
            if (owner != null) {
                return owner.equals(entering.getUniqueId());
            }
        }
        return false;
    }

    public void onVehicleMove(Vehicle vehicle, Location to) {
        for (RaceSession session : sessions.values()) {
            if (session.ownsBoat(vehicle.getUniqueId())) {
                session.onVehicleMove(vehicle, to);
                return;
            }
        }
    }

    public void onQuit(Player player) {
        RaceSession session = holding(player.getUniqueId());
        if (session != null) {
            session.leave(player);
        }
        respawns.remove(player.getUniqueId());
    }

    public void onDeath(Player player) {
        for (RaceSession session : sessions.values()) {
            Location respawn = session.onDeath(player);
            if (respawn != null) {
                respawns.put(player.getUniqueId(), respawn);
                return;
            }
        }
    }

    public void onRespawn(PlayerRespawnEvent event) {
        Location location = respawns.remove(event.getPlayer().getUniqueId());
        if (location != null) {
            event.setRespawnLocation(location);
        }
    }

    public void onWorldChange(Player player) {
        RaceSession session = holding(player.getUniqueId());
        if (session != null) {
            session.onWorldChange(player);
        }
    }

    private RaceSession holding(UUID playerId) {
        for (RaceSession session : sessions.values()) {
            if (session.holds(playerId)) {
                return session;
            }
        }
        return null;
    }

    private RaceSession session(Arena arena) {
        return sessions.computeIfAbsent(arena.name(), name -> new RaceSession(arena, boats, config));
    }
}
