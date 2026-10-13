package mcverse.boatrace.race;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Each racer owns at most one boat. The boat is tagged so a reload can remove
 * leftovers, and a new boat is spawned only when that entity is already gone.
 */
public final class BoatTracker {
    private final JavaPlugin plugin;
    private final NamespacedKey managedKey;
    private final Set<UUID> allowingExit = new HashSet<>();

    public BoatTracker(JavaPlugin plugin) {
        this.plugin = plugin;
        this.managedKey = new NamespacedKey(plugin, "managed");
    }

    public NamespacedKey managedKey() {
        return managedKey;
    }

    public boolean isManaged(Entity entity) {
        return entity != null && entity.getPersistentDataContainer().has(managedKey, PersistentDataType.BYTE);
    }

    public void allowExit(UUID playerId, Runnable action) {
        allowingExit.add(playerId);
        try {
            action.run();
        } finally {
            allowingExit.remove(playerId);
        }
    }

    public boolean isExitAllowed(UUID playerId) {
        return allowingExit.contains(playerId);
    }

    public Boat live(Racer racer) {
        if (racer.boatId() == null) {
            return null;
        }
        Entity entity = Bukkit.getEntity(racer.boatId());
        if (entity instanceof Boat boat && boat.isValid() && !boat.isDead()) {
            return boat;
        }
        return null;
    }

    public Boat spawn(Racer racer, Location location, Boat.Type type) {
        Boat existing = live(racer);
        if (existing != null) {
            return existing;
        }
        Location spawnAt = location.clone();
        Boat boat = location.getWorld().spawn(spawnAt, Boat.class, created -> {
            created.setBoatType(type);
            created.setInvulnerable(true);
            created.getPersistentDataContainer().set(managedKey, PersistentDataType.BYTE, (byte) 1);
        });
        boat.setRotation(spawnAt.getYaw(), 0f);
        boat.setVelocity(new Vector());
        racer.setBoatId(boat.getUniqueId());
        return boat;
    }

    public void ensurePassenger(Racer racer, Player player, Location fallback, Boat.Type type) {
        Boat boat = live(racer);
        if (boat == null) {
            racer.setBoatId(null);
            boat = spawn(racer, fallback, type);
        }
        if (!boat.getPassengers().contains(player)) {
            Boat seated = boat;
            allowExit(player.getUniqueId(), () -> mount(seated, player));
        }
    }

    public void park(Racer racer, Location location, Player player, Boat.Type type) {
        Location target = location.clone();
        target.setPitch(0f);
        Boat boat = live(racer);
        if (boat == null) {
            racer.setBoatId(null);
            boat = spawn(racer, target, type);
        } else {
            boat.setVelocity(new Vector());
            boat.teleport(target);
            boat.setRotation(target.getYaw(), 0f);
            boat.setVelocity(new Vector());
        }
        Boat seated = boat;
        allowExit(player.getUniqueId(), () -> mount(seated, player));
        seated.setVelocity(new Vector());
    }

    public void remove(Racer racer) {
        UUID playerId = racer.playerId();
        allowExit(playerId, () -> {
            Boat boat = live(racer);
            if (boat != null) {
                boat.eject();
                boat.remove();
            }
        });
        racer.setBoatId(null);
    }

    public void removeManagedInside(World world, mcverse.boatrace.arena.Cuboid area) {
        for (Entity entity : new ArrayList<>(world.getEntities())) {
            if (!(entity instanceof Boat boat) || !isManaged(boat)) {
                continue;
            }
            if (mcverse.boatrace.arena.ArenaManager.contains(area, boat.getLocation())) {
                boat.eject();
                boat.remove();
            }
        }
    }

    public void clearAround(Location location) {
        if (location.getWorld() == null) {
            return;
        }
        List<Entity> nearby = new ArrayList<>(location.getWorld().getNearbyEntities(location, 1.5, 2.0, 1.5));
        for (Entity entity : nearby) {
            if (!(entity instanceof Boat boat)) {
                continue;
            }
            List<Entity> passengers = new ArrayList<>(boat.getPassengers());
            for (Entity passenger : passengers) {
                if (passenger instanceof Player player) {
                    allowExit(player.getUniqueId(), player::leaveVehicle);
                    player.sendMessage(mcverse.boatrace.Messages.text("The start line was cleared for the race."));
                } else {
                    passenger.leaveVehicle();
                }
            }
            boat.remove();
        }
    }

    public int removeAllTaggedLoaded() {
        int removed = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Boat boat && isManaged(boat)) {
                    boat.eject();
                    boat.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    public void removeUnownedInChunk(Entity[] entities, Predicate<UUID> owned) {
        for (Entity entity : entities) {
            if (entity instanceof Boat boat && isManaged(boat) && !owned.test(boat.getUniqueId())) {
                boat.eject();
                boat.remove();
            }
        }
    }

    private static void mount(Boat boat, Player player) {
        List<Entity> passengers = new ArrayList<>(boat.getPassengers());
        for (Entity passenger : passengers) {
            if (!passenger.getUniqueId().equals(player.getUniqueId())) {
                boat.removePassenger(passenger);
            }
        }
        if (player.getVehicle() != null && player.getVehicle() != boat) {
            player.leaveVehicle();
        }
        if (!boat.getPassengers().contains(player)) {
            boat.addPassenger(player);
        }
    }
}
