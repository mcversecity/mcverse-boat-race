package mcverse.boatrace.command;

import mcverse.boatrace.BoatRacePlugin;
import mcverse.boatrace.Messages;
import mcverse.boatrace.arena.Arena;
import mcverse.boatrace.arena.ArenaManager;
import mcverse.boatrace.arena.BlockPoint;
import mcverse.boatrace.arena.Cuboid;
import mcverse.boatrace.arena.Pose;
import mcverse.boatrace.arena.Selection;
import mcverse.boatrace.integration.WorldGuardArenas;
import mcverse.boatrace.integration.WorldGuardSync;
import mcverse.boatrace.race.Locations;
import org.bukkit.FluidCollisionMode;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BoatRaceCommand implements CommandExecutor, TabCompleter {
    private static final List<String> PLAY = List.of("help", "join", "leave");
    private static final List<String> ADMIN = List.of(
            "create", "delete", "list", "info", "select", "wand", "pos1", "pos2",
            "setspectator", "setspectatearea", "settrack", "setfinish",
            "addcheckpoint", "removecheckpoint", "clearcheckpoints", "listcheckpoints",
            "setslot", "delslot", "listslots",
            "setminplayers", "setlobby", "setmaxtime",
            "start", "stop", "reload");

    private final BoatRacePlugin plugin;

    public BoatRaceCommand(BoatRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            help(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (!PLAY.contains(sub) && !admin(sender)) {
            return true;
        }
        switch (sub) {
            case "join" -> join(sender, args);
            case "leave" -> leave(sender);
            case "create" -> create(sender, args);
            case "delete" -> delete(sender, args);
            case "list" -> list(sender);
            case "info" -> info(sender, args);
            case "select" -> select(sender, args);
            case "wand" -> wand(sender);
            case "pos1" -> position(sender, true);
            case "pos2" -> position(sender, false);
            case "setspectator" -> setSpectator(sender);
            case "setspectatearea" -> setCuboid(sender, "Spectator area", "spectate", CuboidTarget.SPECTATE);
            case "settrack" -> setCuboid(sender, "Track", "track", CuboidTarget.TRACK);
            case "setfinish" -> setCuboid(sender, "Finish", "finish", CuboidTarget.FINISH);
            case "addcheckpoint" -> addCheckpoint(sender);
            case "removecheckpoint" -> removeCheckpoint(sender, args);
            case "clearcheckpoints" -> clearCheckpoints(sender);
            case "listcheckpoints" -> listCheckpoints(sender);
            case "setslot" -> setSlot(sender, args);
            case "delslot" -> deleteSlot(sender, args);
            case "listslots" -> listSlots(sender);
            case "setminplayers" -> setNumber(sender, args, 1, Arena.MAX_SLOTS, "Min players", (arena, value) -> plugin.arenas().setMinPlayers(arena, value));
            case "setlobby" -> setNumber(sender, args, 5, 300, "Lobby seconds", (arena, value) -> plugin.arenas().setLobbySeconds(arena, value));
            case "setmaxtime" -> setNumber(sender, args, 30, 3600, "Max race seconds", (arena, value) -> plugin.arenas().setMaxRaceSeconds(arena, value));
            case "start" -> start(sender, args);
            case "stop" -> stop(sender, args);
            case "reload" -> reload(sender);
            default -> sender.sendMessage(Messages.error("Unknown command. Use /mcvboatrace help."));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(PLAY);
            if (sender.hasPermission("mcvboatrace.admin")) {
                options.addAll(ADMIN);
            }
            return filter(options, args[0]);
        }
        if (!sender.hasPermission("mcvboatrace.admin") && !"join".equalsIgnoreCase(args[0])) {
            return List.of();
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && List.of("join", "delete", "info", "select", "start", "stop").contains(sub)) {
            return filter(plugin.arenas().names(), args[1]);
        }
        if (args.length == 2 && sub.equals("removecheckpoint")) {
            Arena arena = sender instanceof Player player ? plugin.arenas().selectedOrOnly(player.getUniqueId()) : null;
            if (arena == null) {
                return List.of();
            }
            List<String> indexes = new ArrayList<>();
            for (int i = 1; i <= arena.checkpoints().size(); i++) {
                indexes.add(Integer.toString(i));
            }
            return filter(indexes, args[1]);
        }
        if (args.length == 2 && (sub.equals("setslot") || sub.equals("delslot"))) {
            return filter(List.of("1", "2", "3", "4", "5", "6", "7", "8"), args[1]);
        }
        return List.of();
    }

    private void help(CommandSender sender) {
        sender.sendMessage(Messages.text("Play"));
        sender.sendMessage(Messages.text("/mcvboatrace join [arena] - queue from the spectator area"));
        sender.sendMessage(Messages.text("/mcvboatrace leave - leave the queue or race"));
        if (!sender.hasPermission("mcvboatrace.admin")) {
            return;
        }
        sender.sendMessage(Messages.text("Arena setup"));
        sender.sendMessage(Messages.text("/mcvboatrace create <arena>"));
        sender.sendMessage(Messages.text("/mcvboatrace select <arena>"));
        sender.sendMessage(Messages.text("/mcvboatrace delete <arena>"));
        sender.sendMessage(Messages.text("/mcvboatrace list"));
        sender.sendMessage(Messages.text("/mcvboatrace info [arena]"));
        sender.sendMessage(Messages.text("/mcvboatrace wand - left click pos1, right click pos2"));
        sender.sendMessage(Messages.text("/mcvboatrace pos1"));
        sender.sendMessage(Messages.text("/mcvboatrace pos2"));
        sender.sendMessage(Messages.text("/mcvboatrace setspectator - stand where finishers return"));
        sender.sendMessage(Messages.text("/mcvboatrace setspectatearea - box where players watch and join"));
        sender.sendMessage(Messages.text("/mcvboatrace settrack - box around the ice, including the air above it"));
        sender.sendMessage(Messages.text("/mcvboatrace setfinish"));
        sender.sendMessage(Messages.text("/mcvboatrace addcheckpoint - add boxes in driving order"));
        sender.sendMessage(Messages.text("/mcvboatrace removecheckpoint <n>"));
        sender.sendMessage(Messages.text("/mcvboatrace clearcheckpoints"));
        sender.sendMessage(Messages.text("/mcvboatrace listcheckpoints"));
        sender.sendMessage(Messages.text("/mcvboatrace setslot <n> - stand on the ice and look down the track"));
        sender.sendMessage(Messages.text("/mcvboatrace delslot <n>"));
        sender.sendMessage(Messages.text("/mcvboatrace listslots"));
        sender.sendMessage(Messages.text("/mcvboatrace setminplayers <n>"));
        sender.sendMessage(Messages.text("/mcvboatrace setlobby <seconds>"));
        sender.sendMessage(Messages.text("/mcvboatrace setmaxtime <seconds>"));
        sender.sendMessage(Messages.text("/mcvboatrace start [arena] - skip the lobby"));
        sender.sendMessage(Messages.text("/mcvboatrace stop [arena]"));
        sender.sendMessage(Messages.text("/mcvboatrace reload"));
        sender.sendMessage(Messages.text("A sign with [mcvboatrace] on line 1 joins the race. Line 2 can name the arena."));
        sender.sendMessage(Messages.text("WorldGuard regions are saved as mcvbr_<arena>_spectate, _track, _finish, and _cp1."));
    }

    private void join(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        if (!player.hasPermission("mcvboatrace.join")) {
            player.sendMessage(Messages.error("You cannot join a race."));
            return;
        }
        Arena arena;
        if (args.length >= 2) {
            arena = plugin.arenas().get(args[1]);
            if (arena == null) {
                player.sendMessage(Messages.error("Unknown arena '" + args[1] + "'."));
                return;
            }
        } else {
            var inside = plugin.arenas().containing(player.getLocation());
            if (inside.size() != 1) {
                player.sendMessage(inside.isEmpty()
                        ? Messages.error("Stand in a spectator area, or use /mcvboatrace join <arena>.")
                        : Messages.error("You are in more than one spectator area. Name the arena."));
                return;
            }
            arena = inside.getFirst();
        }
        plugin.races().join(player, arena);
    }

    private void leave(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        plugin.races().leave(player);
    }

    private void create(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Messages.error("Usage: /mcvboatrace create <arena>"));
            return;
        }
        String name = arenaName(sender, args[1]);
        if (name == null) {
            return;
        }
        if (plugin.arenas().get(name) != null) {
            sender.sendMessage(Messages.error("That arena already exists. Select it with /mcvboatrace select " + name + "."));
            return;
        }
        Arena arena = plugin.arenas().create(name);
        if (sender instanceof Player player) {
            plugin.arenas().select(player.getUniqueId(), arena);
        }
        sender.sendMessage(Messages.ok("Created arena " + name + ". Select corners with the wand, then set the track."));
    }

    private void delete(CommandSender sender, String[] args) {
        Arena arena = namedArena(sender, args);
        if (arena == null) {
            return;
        }
        String name = arena.name();
        plugin.races().deleteArena(arena);
        sender.sendMessage(Messages.ok("Deleted arena " + name + "."));
    }

    private void list(CommandSender sender) {
        if (plugin.arenas().names().isEmpty()) {
            sender.sendMessage(Messages.text("No arenas yet. /mcvboatrace create <arena>"));
            return;
        }
        for (String name : plugin.arenas().names()) {
            sender.sendMessage(Messages.text(name + " - " + plugin.races().stateName(name)));
        }
    }

    private void info(CommandSender sender, String[] args) {
        Arena arena = resolve(sender, args, 1);
        if (arena == null) {
            return;
        }
        sender.sendMessage(Messages.text("Arena " + arena.name()
                + " in " + (arena.world() == null ? "no world yet" : arena.world())
                + ", race " + plugin.races().stateName(arena.name())));
        sender.sendMessage(Messages.text("Spectator spawn: " + (arena.spectatorSpawn() == null ? "missing" : arena.spectatorSpawn().format())));
        sender.sendMessage(Messages.text("Spectate area: " + format(arena.spectateArea())));
        sender.sendMessage(Messages.text("Track: " + format(arena.track())));
        sender.sendMessage(Messages.text("Finish: " + format(arena.finish())));
        sender.sendMessage(Messages.text("Checkpoints: " + arena.checkpoints().size()));
        sender.sendMessage(Messages.text("Start slots: " + arena.slotCount()));
        sender.sendMessage(Messages.text("Min players " + arena.minPlayers()
                + ", lobby " + arena.lobbySeconds() + "s, max time " + arena.maxRaceSeconds() + "s"));
        if (arena.spectateArea() != null) {
            sender.sendMessage(Messages.text("WorldGuard " + WorldGuardArenas.id(arena.name(), "spectate")));
        }
        if (arena.track() != null) {
            sender.sendMessage(Messages.text("WorldGuard " + WorldGuardArenas.id(arena.name(), "track")));
        }
        if (arena.finish() != null) {
            sender.sendMessage(Messages.text("WorldGuard " + WorldGuardArenas.id(arena.name(), "finish")));
        }
        var problems = plugin.arenas().problems(arena);
        if (problems.isEmpty()) {
            sender.sendMessage(Messages.ok("Setup complete."));
        } else {
            sender.sendMessage(Messages.error("Still needed: " + String.join(", ", problems)));
        }
    }

    private void select(CommandSender sender, String[] args) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        Arena arena = namedArena(sender, args);
        if (arena == null) {
            return;
        }
        plugin.arenas().select(player.getUniqueId(), arena);
        player.sendMessage(Messages.ok("Selected arena " + arena.name() + "."));
    }

    private void wand(CommandSender sender) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        ItemStack wand = plugin.wand().item();
        var leftover = player.getInventory().addItem(wand);
        for (ItemStack extra : leftover.values()) {
            player.getWorld().dropItem(player.getLocation(), extra);
        }
        player.sendMessage(Messages.ok("Left click sets position 1. Right click sets position 2."));
    }

    private void position(CommandSender sender, boolean first) {
        Player player = player(sender);
        if (player == null) {
            return;
        }
        Block block = player.getTargetBlockExact(80, FluidCollisionMode.NEVER);
        if (block == null) {
            player.sendMessage(Messages.error("Look at a block."));
            return;
        }
        Selection selection = plugin.selections().get(player.getUniqueId());
        BlockPoint point = new BlockPoint(block.getX(), block.getY(), block.getZ());
        if (first) {
            selection.setPos1(block.getWorld().getName(), point);
            player.sendMessage(Messages.ok("Position 1 set to " + point + "."));
        } else {
            selection.setPos2(block.getWorld().getName(), point);
            player.sendMessage(Messages.ok("Position 2 set to " + point + "."));
        }
    }

    private void setSpectator(CommandSender sender) {
        Player player = player(sender);
        Arena arena = editable(player);
        if (player == null || arena == null || !sameWorld(player, arena)) {
            return;
        }
        plugin.arenas().setSpectatorSpawn(arena, player.getWorld().getName(), Locations.pose(player.getLocation()));
        player.sendMessage(Messages.ok("Spectator spawn saved at " + arena.spectatorSpawn().format() + "."));
    }

    private void setCuboid(CommandSender sender, String label, String regionPart, CuboidTarget target) {
        Player player = player(sender);
        Arena arena = editable(player);
        if (player == null || arena == null) {
            return;
        }
        Cuboid cuboid = selection(player, arena);
        if (cuboid == null) {
            return;
        }
        WorldGuardSync.Result result = switch (target) {
            case SPECTATE -> plugin.arenas().setSpectateArea(arena, cuboid);
            case TRACK -> plugin.arenas().setTrack(arena, cuboid);
            case FINISH -> plugin.arenas().setFinish(arena, cuboid);
        };
        player.sendMessage(Messages.ok(label + " saved " + cuboid.format() + "."
                + WorldGuardSync.describe(result, WorldGuardArenas.id(arena.name(), regionPart))));
        if (target == CuboidTarget.TRACK) {
            player.sendMessage(Messages.text("Include the air above the ice, or boats will count as off the track."));
        }
    }

    private void addCheckpoint(CommandSender sender) {
        Player player = player(sender);
        Arena arena = editable(player);
        if (player == null || arena == null) {
            return;
        }
        if (arena.checkpoints().size() >= Arena.MAX_CHECKPOINTS) {
            player.sendMessage(Messages.error("This arena already has " + Arena.MAX_CHECKPOINTS + " checkpoints."));
            return;
        }
        Cuboid cuboid = selection(player, arena);
        if (cuboid == null) {
            return;
        }
        WorldGuardSync.Result result = plugin.arenas().addCheckpoint(arena, cuboid);
        int number = arena.checkpoints().size();
        player.sendMessage(Messages.ok("Checkpoint " + number + " saved " + cuboid.format() + "."
                + WorldGuardSync.describe(result, WorldGuardArenas.id(arena.name(), "cp" + number))));
    }

    private void removeCheckpoint(CommandSender sender, String[] args) {
        Player player = player(sender);
        Arena arena = editable(player);
        if (player == null || arena == null) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Messages.error("Usage: /mcvboatrace removecheckpoint <n>"));
            return;
        }
        if (arena.checkpoints().isEmpty()) {
            player.sendMessage(Messages.error("There are no checkpoints to remove."));
            return;
        }
        Integer number = parse(player, args[1], 1, arena.checkpoints().size(), "Checkpoint");
        if (number == null) {
            return;
        }
        WorldGuardSync.Result result = plugin.arenas().removeCheckpoint(arena, number - 1);
        player.sendMessage(Messages.ok("Removed checkpoint " + number + "."
                + WorldGuardSync.describe(result, WorldGuardArenas.id(arena.name(), "cp"))));
    }

    private void clearCheckpoints(CommandSender sender) {
        Player player = player(sender);
        Arena arena = editable(player);
        if (player == null || arena == null) {
            return;
        }
        WorldGuardSync.Result result = plugin.arenas().clearCheckpoints(arena);
        player.sendMessage(Messages.ok("Cleared checkpoints."
                + WorldGuardSync.describe(result, WorldGuardArenas.id(arena.name(), "cp"))));
    }

    private void listCheckpoints(CommandSender sender) {
        Arena arena = resolve(sender, new String[0], 1);
        if (arena == null) {
            return;
        }
        if (arena.checkpoints().isEmpty()) {
            sender.sendMessage(Messages.text("No checkpoints yet."));
            return;
        }
        int number = 1;
        for (Cuboid cuboid : arena.checkpoints()) {
            sender.sendMessage(Messages.text(number + ": " + cuboid.format()));
            number++;
        }
    }

    private void setSlot(CommandSender sender, String[] args) {
        Player player = player(sender);
        Arena arena = editable(player);
        if (player == null || arena == null || !sameWorld(player, arena)) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Messages.error("Usage: /mcvboatrace setslot <n>"));
            return;
        }
        Integer number = parse(player, args[1], 1, Arena.MAX_SLOTS, "Slot");
        if (number == null) {
            return;
        }
        Pose pose = Locations.pose(player.getLocation());
        plugin.arenas().setSlot(arena, number, player.getWorld().getName(), pose);
        player.sendMessage(Messages.ok("Start slot " + number + " saved at " + pose.format() + "."));
        if (arena.track() != null && !arena.track().contains(player.getWorld().getName(), pose.x(), pose.y(), pose.z())) {
            player.sendMessage(Messages.error("That slot is outside the track. The race cannot start until you move the slot or expand the track."));
        }
    }

    private void deleteSlot(CommandSender sender, String[] args) {
        Player player = player(sender);
        Arena arena = editable(player);
        if (player == null || arena == null) {
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Messages.error("Usage: /mcvboatrace delslot <n>"));
            return;
        }
        Integer number = parse(player, args[1], 1, Arena.MAX_SLOTS, "Slot");
        if (number == null) {
            return;
        }
        if (!plugin.arenas().removeSlot(arena, number)) {
            player.sendMessage(Messages.error("Slot " + number + " is not set."));
            return;
        }
        player.sendMessage(Messages.ok("Removed start slot " + number + "."));
    }

    private void listSlots(CommandSender sender) {
        Arena arena = resolve(sender, new String[0], 1);
        if (arena == null) {
            return;
        }
        if (arena.slots().isEmpty()) {
            sender.sendMessage(Messages.text("No start slots yet."));
            return;
        }
        for (Map.Entry<Integer, Pose> entry : arena.slots().entrySet()) {
            sender.sendMessage(Messages.text(entry.getKey() + ": " + entry.getValue().format()));
        }
    }

    private void setNumber(CommandSender sender, String[] args, int min, int max, String label, IntSetter setter) {
        Arena arena = editable(sender instanceof Player player ? player : null);
        if (!(sender instanceof Player player) || arena == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(Messages.error("Only a player can do that."));
            }
            return;
        }
        if (args.length < 2) {
            player.sendMessage(Messages.error("Usage: /mcvboatrace " + args[0].toLowerCase(Locale.ROOT) + " <number>"));
            return;
        }
        Integer value = parse(player, args[1], min, max, label);
        if (value == null) {
            return;
        }
        setter.set(arena, value);
        player.sendMessage(Messages.ok(label + " set to " + value + " for " + arena.name() + "."));
    }

    private void start(CommandSender sender, String[] args) {
        Arena arena = resolve(sender, args, 1);
        if (arena == null) {
            return;
        }
        var problems = plugin.arenas().problems(arena);
        if (!problems.isEmpty()) {
            sender.sendMessage(Messages.error("That arena is not ready: " + String.join(", ", problems)));
            return;
        }
        String state = plugin.races().stateName(arena.name());
        if (state.equals("countdown") || state.equals("racing") || state.equals("results")) {
            sender.sendMessage(Messages.error("A race is already running in " + arena.name() + "."));
            return;
        }
        if (!plugin.races().forceStart(arena)) {
            sender.sendMessage(Messages.error("Nobody is queued. Players join from the spectator area."));
            return;
        }
        sender.sendMessage(Messages.ok("Started the countdown in " + arena.name() + "."));
    }

    private void stop(CommandSender sender, String[] args) {
        Arena arena = resolve(sender, args, 1);
        if (arena == null) {
            return;
        }
        if (!plugin.races().running(arena)) {
            sender.sendMessage(Messages.error("No race is running in " + arena.name() + "."));
            return;
        }
        plugin.races().stop(arena, "The race was stopped.");
        sender.sendMessage(Messages.ok("Stopped the race in " + arena.name() + "."));
    }

    private void reload(CommandSender sender) {
        plugin.reloadPlugin();
        sender.sendMessage(Messages.ok("Reloaded boat race config and idle arenas."));
    }

    private Arena editable(Player player) {
        if (player == null) {
            return null;
        }
        Arena arena = plugin.arenas().selectedOrOnly(player.getUniqueId());
        if (arena == null) {
            player.sendMessage(Messages.error("Select an arena with /mcvboatrace select <arena>."));
            return null;
        }
        if (plugin.races().isBusy(arena.name())) {
            player.sendMessage(Messages.error("Stop the race before changing " + arena.name() + "."));
            return null;
        }
        return arena;
    }

    private Arena namedArena(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Messages.error("Name the arena. See /mcvboatrace list."));
            return null;
        }
        Arena arena = plugin.arenas().get(args[1]);
        if (arena == null) {
            sender.sendMessage(Messages.error("Unknown arena '" + args[1] + "'."));
        }
        return arena;
    }

    private Arena resolve(CommandSender sender, String[] args, int index) {
        if (args.length > index) {
            Arena arena = plugin.arenas().get(args[index]);
            if (arena == null) {
                sender.sendMessage(Messages.error("Unknown arena '" + args[index] + "'."));
            }
            return arena;
        }
        if (sender instanceof Player player) {
            Arena arena = plugin.arenas().selectedOrOnly(player.getUniqueId());
            if (arena == null) {
                player.sendMessage(Messages.error("Select an arena with /mcvboatrace select <arena>."));
            }
            return arena;
        }
        sender.sendMessage(Messages.error("Name the arena. See /mcvboatrace list."));
        return null;
    }

    private Cuboid selection(Player player, Arena arena) {
        Selection selection = plugin.selections().get(player.getUniqueId());
        if (!selection.complete()) {
            player.sendMessage(Messages.error("Set both corners with the wand, or /mcvboatrace pos1 and pos2."));
            return null;
        }
        if (arena.world() != null && !arena.world().equals(selection.world())) {
            player.sendMessage(Messages.error("That selection is in " + selection.world() + ", but the arena is in " + arena.world() + "."));
            return null;
        }
        return selection.toCuboid();
    }

    private boolean sameWorld(Player player, Arena arena) {
        if (arena.world() != null && !arena.world().equals(player.getWorld().getName())) {
            player.sendMessage(Messages.error("This arena is in " + arena.world() + "."));
            return false;
        }
        return true;
    }

    private Player player(CommandSender sender) {
        if (sender instanceof Player player) {
            return player;
        }
        sender.sendMessage(Messages.error("Only a player can do that."));
        return null;
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("mcvboatrace.admin")) {
            return true;
        }
        sender.sendMessage(Messages.error("You need the mcvboatrace.admin permission."));
        return false;
    }

    private String arenaName(CommandSender sender, String raw) {
        String name = raw.toLowerCase(Locale.ROOT);
        if (!ArenaManager.NAME.matcher(name).matches()) {
            sender.sendMessage(Messages.error("Use 1-24 letters, numbers, underscores, or hyphens."));
            return null;
        }
        return name;
    }

    private Integer parse(CommandSender sender, String raw, int min, int max, String label) {
        try {
            int value = Integer.parseInt(raw);
            if (value < min || value > max) {
                sender.sendMessage(Messages.error(label + " must be " + min + "-" + max + "."));
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            sender.sendMessage(Messages.error(label + " must be a number."));
            return null;
        }
    }

    private static String format(Cuboid cuboid) {
        return cuboid == null ? "missing" : cuboid.format();
    }

    private static List<String> filter(Iterable<String> options, String prefix) {
        String needle = prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(needle)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private enum CuboidTarget {
        SPECTATE,
        TRACK,
        FINISH
    }

    @FunctionalInterface
    private interface IntSetter {
        void set(Arena arena, int value);
    }
}
