package mcverse.boatrace.listener;

import mcverse.boatrace.BoatRacePlugin;
import mcverse.boatrace.Messages;
import mcverse.boatrace.arena.Arena;
import mcverse.boatrace.arena.BlockPoint;
import mcverse.boatrace.arena.Selection;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;

public final class RaceListener implements Listener {
    private final BoatRacePlugin plugin;

    public RaceListener(BoatRacePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleMove(VehicleMoveEvent event) {
        if (plugin.races().shouldLock(event.getVehicle())) {
            event.getVehicle().setVelocity(new Vector());
            return;
        }
        plugin.races().onVehicleMove(event.getVehicle(), event.getTo());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getExited() instanceof Player player && plugin.races().shouldCancelExit(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player && plugin.races().shouldCancelExit(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!plugin.races().canEnter(event.getVehicle(), event.getEntered())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        if (event.getVehicle() instanceof Boat boat
                && plugin.boats().isManaged(boat)
                && plugin.races().ownsBoat(boat.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.races().onQuit(event.getPlayer());
    }

    @EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent event) {
        plugin.races().onDeath(event.getEntity());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        plugin.races().onRespawn(event);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        plugin.races().onWorldChange(event.getPlayer());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        plugin.boats().removeUnownedInChunk(event.getChunk().getEntities(), plugin.races()::ownsBoat);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != null && event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        if (plugin.wand().isWand(event.getItem())) {
            if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }
            Block block = event.getClickedBlock();
            if (block == null) {
                return;
            }
            event.setCancelled(true);
            Selection selection = plugin.selections().get(player.getUniqueId());
            BlockPoint point = new BlockPoint(block.getX(), block.getY(), block.getZ());
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                selection.setPos1(block.getWorld().getName(), point);
                player.sendMessage(Messages.ok("Position 1 set to " + point + "."));
            } else {
                selection.setPos2(block.getWorld().getName(), point);
                player.sendMessage(Messages.ok("Position 2 set to " + point + "."));
            }
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (!(event.getClickedBlock().getState() instanceof Sign sign)) {
            return;
        }
        String header = plain(sign.getSide(Side.FRONT).getLine(0));
        if (!header.equalsIgnoreCase("[mcvboatrace]")) {
            return;
        }
        event.setCancelled(true);
        if (!player.hasPermission("mcvboatrace.join")) {
            player.sendMessage(Messages.error("You cannot join a race."));
            return;
        }
        String requested = plain(sign.getSide(Side.FRONT).getLine(1));
        Arena arena = requested.isBlank()
                ? onlyArenaAt(player)
                : plugin.arenas().get(requested);
        if (arena == null) {
            if (!requested.isBlank()) {
                player.sendMessage(Messages.error("Unknown arena '" + requested + "'."));
            }
            return;
        }
        plugin.races().join(player, arena);
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String header = plain(event.getLine(0));
        if (!header.equalsIgnoreCase("[mcvboatrace]")) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.hasPermission("mcvboatrace.admin")) {
            event.setCancelled(true);
            player.sendMessage(Messages.error("You need the mcvboatrace.admin permission to create a join sign."));
            return;
        }
        String requested = plain(event.getLine(1)).toLowerCase(Locale.ROOT);
        if (!requested.isBlank() && plugin.arenas().get(requested) == null) {
            event.setCancelled(true);
            player.sendMessage(Messages.error("Unknown arena '" + requested + "'."));
            return;
        }
        event.setLine(0, ChatColor.AQUA + "[mcvboatrace]");
        if (!requested.isBlank()) {
            event.setLine(1, requested);
        }
        Block block = event.getBlock();
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (block.getState() instanceof Sign sign) {
                sign.setWaxed(true);
                sign.update(false, false);
            }
        });
        player.sendMessage(Messages.ok("Join sign created. Right click it to join."));
    }

    private Arena onlyArenaAt(Player player) {
        List<Arena> inside = plugin.arenas().containing(player.getLocation());
        if (inside.size() == 1) {
            return inside.get(0);
        }
        if (inside.isEmpty()) {
            player.sendMessage(Messages.error("Stand in a spectator area, or put the arena name on the sign."));
        } else {
            player.sendMessage(Messages.error("You are in more than one spectator area. Put the arena name on the sign."));
        }
        return null;
    }

    private static String plain(String text) {
        if (text == null) {
            return "";
        }
        return ChatColor.stripColor(text).trim();
    }
}
