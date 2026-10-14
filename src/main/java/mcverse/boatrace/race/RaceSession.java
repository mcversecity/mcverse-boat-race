package mcverse.boatrace.race;

import mcverse.boatrace.Messages;
import mcverse.boatrace.arena.Arena;
import mcverse.boatrace.arena.ArenaManager;
import mcverse.boatrace.arena.ArenaValidator;
import mcverse.boatrace.arena.Cuboid;
import mcverse.boatrace.arena.Pose;
import mcverse.boatrace.config.PluginConfig;
import mcverse.boatrace.track.OffTrack;
import mcverse.boatrace.track.Sample;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public final class RaceSession {
    private final Arena arena;
    private final BoatTracker boats;
    private final PluginConfig config;
    private final List<UUID> queue = new ArrayList<>();
    private final List<UUID> nextQueue = new ArrayList<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, Racer> racers = new LinkedHashMap<>();
    private BossBar bar;
    private RaceState state = RaceState.IDLE;
    private int tickClock;
    private int secondsLeft;
    private int countNumber;
    private int countTicks;
    private int raceTicks;
    private int elapsedSeconds;
    private int resultTicks;
    private int placeCounter;

    public RaceSession(Arena arena, BoatTracker boats, PluginConfig config) {
        this.arena = arena;
        this.boats = boats;
        this.config = config;
    }

    public Arena arena() {
        return arena;
    }

    public RaceState state() {
        return state;
    }

    public boolean busy() {
        return state != RaceState.IDLE;
    }

    public boolean holds(UUID playerId) {
        if (queue.contains(playerId) || nextQueue.contains(playerId)) {
            return true;
        }
        Racer racer = racers.get(playerId);
        return racer != null && !racer.finished();
    }

    public boolean isFrozen(Player player) {
        return isLockedInBoat(player.getUniqueId()) && state == RaceState.COUNTDOWN;
    }

    public boolean isLockedInBoat(UUID playerId) {
        Racer racer = racers.get(playerId);
        return racer != null
                && !racer.finished()
                && (state == RaceState.COUNTDOWN || state == RaceState.RACING);
    }

    public boolean locks(Vehicle vehicle) {
        if (state != RaceState.COUNTDOWN || !(vehicle instanceof Boat boat)) {
            return false;
        }
        return racerByBoat(boat.getUniqueId()) != null;
    }

    public UUID boatOwner(UUID boatId) {
        Racer racer = racerByBoat(boatId);
        if (racer == null || racer.finished()) {
            return null;
        }
        return racer.playerId();
    }

    public boolean ownsBoat(UUID boatId) {
        return racerByBoat(boatId) != null;
    }

    public void join(Player player) {
        UUID playerId = player.getUniqueId();
        names.put(playerId, player.getName());
        if (queue.contains(playerId) || nextQueue.contains(playerId)) {
            player.sendMessage(Messages.error("You are already queued."));
            return;
        }
        Racer existing = racers.get(playerId);
        if (existing != null && !existing.finished()) {
            player.sendMessage(Messages.error("You are already in this race."));
            return;
        }
        if (arena.spectateArea() == null || !ArenaManager.contains(arena.spectateArea(), player.getLocation())) {
            player.sendMessage(Messages.error("Join from inside the spectator area."));
            return;
        }
        boolean raceBusy = state == RaceState.COUNTDOWN || state == RaceState.RACING || state == RaceState.RESULTS;
        List<UUID> target = raceBusy || queue.size() >= arena.slotCount() ? nextQueue : queue;
        if (target.size() >= arena.slotCount()) {
            player.sendMessage(Messages.error("That race is full."));
            return;
        }
        target.add(playerId);
        if (target == queue) {
            player.sendMessage(Messages.ok("You joined the race (" + queue.size() + "/" + arena.slotCount() + ")."));
            tellExcept(player.getName() + " joined (" + queue.size() + "/" + arena.slotCount() + ").", playerId);
            if (state == RaceState.IDLE) {
                startLobby();
            }
            return;
        }
        player.sendMessage(Messages.ok("You are queued for the next race (" + nextQueue.size() + "/" + arena.slotCount() + ")."));
    }

    public void leave(Player player) {
        UUID playerId = player.getUniqueId();
        if (queue.remove(playerId)) {
            pullNextIntoQueue();
            player.sendMessage(Messages.text("You left the queue."));
            if (queue.isEmpty() && state == RaceState.LOBBY) {
                becomeIdle();
            }
            return;
        }
        if (nextQueue.remove(playerId)) {
            player.sendMessage(Messages.text("You left the next race."));
            return;
        }
        Racer racer = racers.get(playerId);
        if (racer != null && !racer.finished()) {
            markDnf(racer, true);
            checkEnd();
            return;
        }
        player.sendMessage(Messages.error("You are not in a race."));
    }

    public boolean forceStart() {
        if (state == RaceState.COUNTDOWN || state == RaceState.RACING || state == RaceState.RESULTS) {
            return false;
        }
        evict(queue);
        if (queue.isEmpty()) {
            return false;
        }
        beginCountdown();
        return state == RaceState.COUNTDOWN;
    }

    public boolean hasWaitingPlayers() {
        return !queue.isEmpty();
    }

    public void stop(String reason) {
        List<Player> people = new ArrayList<>(audience());
        for (Racer racer : new ArrayList<>(racers.values())) {
            boats.remove(racer);
            Player player = Bukkit.getPlayer(racer.playerId());
            if (player != null && !racer.finished()) {
                teleportSpectator(player);
            }
        }
        queue.clear();
        nextQueue.clear();
        racers.clear();
        state = RaceState.IDLE;
        hideBar();
        for (Player player : people) {
            player.sendMessage(Messages.text(reason));
        }
    }

    public void shutdown() {
        for (Racer racer : racers.values()) {
            boats.remove(racer);
        }
        racers.clear();
        queue.clear();
        nextQueue.clear();
        state = RaceState.IDLE;
        hideBar();
    }

    public void tick() {
        tickClock++;
        switch (state) {
            case LOBBY -> {
                if (tickClock % 20 == 0) {
                    tickLobbySecond();
                }
            }
            case COUNTDOWN -> tickCountdown();
            case RACING -> tickRacing();
            case RESULTS -> {
                if (tickClock % 20 == 0) {
                    tickResultsSecond();
                }
            }
            default -> {
                return;
            }
        }
        if (state != RaceState.IDLE && tickClock % 20 == 0) {
            refreshBar();
        }
    }

    public void onVehicleMove(Vehicle vehicle, Location to) {
        if (state != RaceState.RACING || !(vehicle instanceof Boat boat)) {
            return;
        }
        Racer racer = racerByBoat(boat.getUniqueId());
        if (racer == null || racer.finished()) {
            return;
        }
        List<Cuboid> checkpoints = arena.checkpoints();
        for (int index = 0; index < checkpoints.size(); index++) {
            if (!ArenaManager.contains(checkpoints.get(index), to)) {
                continue;
            }
            if (!racer.progress().advanceIfExpected(index)) {
                continue;
            }
            Sample sample = Locations.sample(to);
            boolean legal = arena.track() != null
                    && ArenaManager.contains(arena.track(), to)
                    && ArenaValidator.Ice.under(to, config.iceBlocks());
            if (legal) {
                racer.setLastCheckpoint(sample);
                racer.trail().add(sample);
            }
            Player player = Bukkit.getPlayer(racer.playerId());
            if (player != null) {
                String message = "Checkpoint " + (index + 1) + "/" + checkpoints.size();
                player.sendMessage(Messages.ok(message));
                Titles.action(player, message);
            }
        }
        if (arena.finish() != null && ArenaManager.contains(arena.finish(), to) && racer.progress().canFinish()) {
            finish(racer);
        }
    }

    public Location onDeath(Player player) {
        Racer racer = racers.get(player.getUniqueId());
        if (racer == null || racer.finished()) {
            return null;
        }
        Location respawn = spectatorLocation();
        markDnf(racer, true);
        checkEnd();
        return respawn;
    }

    public void onWorldChange(Player player) {
        UUID playerId = player.getUniqueId();
        if (queue.remove(playerId) || nextQueue.remove(playerId)) {
            player.sendMessage(Messages.error("You left the spectator area and the queue."));
            if (state == RaceState.LOBBY && queue.isEmpty()) {
                becomeIdle();
            }
            return;
        }
        Racer racer = racers.get(playerId);
        if (racer != null && !racer.finished()) {
            markDnf(racer, true);
            checkEnd();
        }
    }

    private void tickLobbySecond() {
        evict(queue);
        evict(nextQueue);
        if (queue.isEmpty()) {
            pullNextIntoQueue();
        }
        if (queue.isEmpty()) {
            becomeIdle();
            return;
        }
        if (queue.size() < arena.minPlayers()) {
            return;
        }
        if (queue.size() >= arena.slotCount() && secondsLeft > config.fullLobbySeconds()) {
            secondsLeft = config.fullLobbySeconds();
        }
        secondsLeft--;
        if (secondsLeft <= 0) {
            beginCountdown();
        }
    }

    private void tickCountdown() {
        lockBoats();
        countTicks++;
        if (countTicks < 20) {
            return;
        }
        countTicks = 0;
        countNumber--;
        if (countNumber > 0) {
            showCountdown(countNumber);
            return;
        }
        showStart();
        state = RaceState.RACING;
        raceTicks = 0;
        elapsedSeconds = 0;
        for (Racer racer : racers.values()) {
            racer.grace().reset();
            racer.setCooldown(config.offTrackGraceTicks());
        }
        refreshBar();
    }

    private void tickRacing() {
        World world = world();
        if (world == null) {
            stop("The race world unloaded, so the race was stopped.");
            return;
        }
        for (Racer racer : new ArrayList<>(racers.values())) {
            if (racer.finished() || state != RaceState.RACING) {
                continue;
            }
            Player player = Bukkit.getPlayer(racer.playerId());
            if (player == null || !player.isOnline()) {
                markDnf(racer, false);
                continue;
            }
            Location fallback = Locations.of(world, racer.start());
            if (!racer.trail().newestFirst().isEmpty()) {
                fallback = Locations.of(world, racer.trail().newestFirst().getFirst());
            }
            boats.ensurePassenger(racer, player, fallback, config.boatType());
            if (racer.cooldown() > 0) {
                racer.tickCooldown();
                continue;
            }
            Boat boat = boats.live(racer);
            if (boat == null) {
                continue;
            }
            Location location = boat.getLocation();
            boolean onTrack = arena.track() != null
                    && ArenaManager.contains(arena.track(), location)
                    && ArenaValidator.Ice.under(location, config.iceBlocks());
            if (onTrack && tickClock % config.trailIntervalTicks() == 0) {
                racer.trail().addIfMoved(Locations.sample(location), 0.5);
            }
            if (racer.grace().shouldRecover(!onTrack)) {
                recover(racer, player, world);
            }
        }
        checkEnd();
        if (state != RaceState.RACING) {
            return;
        }
        raceTicks++;
        if (raceTicks % 20 == 0) {
            elapsedSeconds++;
            evict(nextQueue);
            if (elapsedSeconds >= arena.maxRaceSeconds()) {
                dnfRemaining();
                beginResults();
            }
        }
    }

    private void tickResultsSecond() {
        resultTicks--;
        if (resultTicks > 0) {
            return;
        }
        racers.clear();
        queue.clear();
        while (!nextQueue.isEmpty() && queue.size() < arena.slotCount()) {
            queue.add(nextQueue.removeFirst());
        }
        if (queue.isEmpty()) {
            becomeIdle();
        } else {
            startLobby();
        }
    }

    private void startLobby() {
        state = RaceState.LOBBY;
        secondsLeft = arena.lobbySeconds();
        if (queue.size() >= arena.slotCount() && secondsLeft > config.fullLobbySeconds()) {
            secondsLeft = config.fullLobbySeconds();
        }
        refreshBar();
    }

    private void beginCountdown() {
        evict(queue);
        if (queue.isEmpty()) {
            becomeIdle();
            return;
        }
        World world = world();
        if (world == null) {
            stop("The race world is not loaded.");
            return;
        }
        if (arena.track() != null) {
            boats.removeManagedInside(world, arena.track());
        }
        List<UUID> starting = new ArrayList<>(queue);
        List<Integer> slotNumbers = new ArrayList<>(arena.slots().keySet());
        for (int index = 0; index < starting.size() && index < slotNumbers.size(); index++) {
            Pose pose = arena.slots().get(slotNumbers.get(index));
            boats.clearAround(Locations.of(world, pose));
        }
        racers.clear();
        placeCounter = 0;
        List<UUID> overflow = new ArrayList<>();
        int index = 0;
        try {
            for (UUID playerId : starting) {
                if (index >= slotNumbers.size()) {
                    overflow.add(playerId);
                    continue;
                }
                Player player = Bukkit.getPlayer(playerId);
                if (player == null) {
                    continue;
                }
                int slotNumber = slotNumbers.get(index++);
                Pose pose = arena.slots().get(slotNumber);
                Racer racer = new Racer(
                        playerId,
                        names.getOrDefault(playerId, player.getName()),
                        slotNumber,
                        pose,
                        arena.checkpoints().size(),
                        config.trailSamples(),
                        config.offTrackGraceTicks());
                racers.put(playerId, racer);
                boats.park(racer, Locations.of(world, pose), player, config.boatType());
            }
        } catch (RuntimeException ex) {
            for (Racer racer : new ArrayList<>(racers.values())) {
                boats.remove(racer);
            }
            racers.clear();
            Bukkit.getLogger().log(Level.SEVERE, "Could not spawn a race boat in " + arena.name(), ex);
            tell("The race could not start because a boat failed to spawn.");
            return;
        }
        queue.clear();
        for (int overflowIndex = overflow.size() - 1; overflowIndex >= 0; overflowIndex--) {
            nextQueue.addFirst(overflow.get(overflowIndex));
        }
        if (racers.isEmpty()) {
            becomeIdle();
            return;
        }
        state = RaceState.COUNTDOWN;
        countNumber = 3;
        countTicks = 0;
        showCountdown(3);
        refreshBar();
    }

    private void lockBoats() {
        World world = world();
        if (world == null) {
            return;
        }
        for (Racer racer : racers.values()) {
            Player player = Bukkit.getPlayer(racer.playerId());
            if (player == null) {
                continue;
            }
            boats.park(racer, Locations.of(world, racer.start()), player, config.boatType());
        }
    }

    private void recover(Racer racer, Player player, World world) {
        Sample safe = OffTrack.firstSafe(racer.trail().newestFirst(), sample -> {
            Location location = Locations.of(world, sample);
            return arena.track() != null
                    && ArenaManager.contains(arena.track(), location)
                    && ArenaValidator.Ice.under(location, config.iceBlocks())
                    && ArenaValidator.Ice.boatSpace(location);
        });
        if (safe == null) {
            safe = racer.lastCheckpoint();
        }
        if (safe == null) {
            safe = Sample.fromPose(racer.start());
        }
        boats.park(racer, Locations.of(world, safe), player, config.boatType());
        racer.grace().reset();
        racer.setCooldown(config.recoveryCooldownTicks());
        player.sendMessage(Messages.text("Back on the ice."));
        Titles.action(player, "Back on the ice");
    }

    private void finish(Racer racer) {
        if (racer.finished()) {
            return;
        }
        racer.setFinished(true);
        racer.setPlace(++placeCounter);
        boats.remove(racer);
        Player player = Bukkit.getPlayer(racer.playerId());
        if (player != null) {
            teleportSpectator(player);
            Titles.place(player, racer.place());
            player.sendMessage(Messages.ok("You finished " + Messages.place(racer.place()) + "."));
        }
        tell(racer.name() + " finished " + Messages.place(racer.place()) + ".");
        checkEnd();
    }

    private void markDnf(Racer racer, boolean tellPlayer) {
        if (racer.finished()) {
            return;
        }
        racer.setFinished(true);
        boats.remove(racer);
        Player player = Bukkit.getPlayer(racer.playerId());
        if (player != null) {
            teleportSpectator(player);
            if (tellPlayer) {
                player.sendMessage(Messages.error("You are out of the race."));
            }
        }
        tell(racer.name() + " is out.");
    }

    private void dnfRemaining() {
        for (Racer racer : new ArrayList<>(racers.values())) {
            if (!racer.finished()) {
                markDnf(racer, true);
            }
        }
    }

    private void checkEnd() {
        if (state != RaceState.RACING) {
            return;
        }
        for (Racer racer : racers.values()) {
            if (!racer.finished()) {
                return;
            }
        }
        beginResults();
    }

    private void beginResults() {
        if (state == RaceState.RESULTS || state == RaceState.IDLE) {
            return;
        }
        state = RaceState.RESULTS;
        resultTicks = config.resultsSeconds();
        for (Racer racer : racers.values()) {
            boats.remove(racer);
        }
        List<Racer> finished = new ArrayList<>();
        List<Racer> dnf = new ArrayList<>();
        for (Racer racer : racers.values()) {
            if (racer.place() > 0) {
                finished.add(racer);
            } else {
                dnf.add(racer);
            }
        }
        finished.sort((left, right) -> Integer.compare(left.place(), right.place()));
        tell("Results:");
        if (finished.isEmpty()) {
            tell("Nobody finished.");
        }
        for (Racer racer : finished) {
            tell(Messages.place(racer.place()) + " " + racer.name());
        }
        for (Racer racer : dnf) {
            tell("DNF " + racer.name());
        }
        refreshBar();
    }

    private void becomeIdle() {
        state = RaceState.IDLE;
        racers.clear();
        hideBar();
        if (!nextQueue.isEmpty() && queue.isEmpty()) {
            pullNextIntoQueue();
            if (!queue.isEmpty()) {
                startLobby();
            }
        }
    }

    private void pullNextIntoQueue() {
        while (!nextQueue.isEmpty() && queue.size() < arena.slotCount()) {
            UUID playerId = nextQueue.removeFirst();
            queue.add(playerId);
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                player.sendMessage(Messages.ok("You joined this race."));
            }
        }
    }

    private void evict(List<UUID> ids) {
        World world = world();
        Iterator<UUID> iterator = ids.iterator();
        while (iterator.hasNext()) {
            UUID playerId = iterator.next();
            Player player = Bukkit.getPlayer(playerId);
            if (player == null || world == null || player.getWorld() != world
                    || arena.spectateArea() == null
                    || !ArenaManager.contains(arena.spectateArea(), player.getLocation())) {
                iterator.remove();
                if (player != null) {
                    player.sendMessage(Messages.error("You left the spectator area and the queue."));
                }
            }
        }
    }

    private void showCountdown(int number) {
        for (Player player : audience()) {
            Titles.countdown(player, number);
        }
    }

    private void showStart() {
        for (Player player : audience()) {
            Titles.start(player);
        }
    }

    private void tell(String message) {
        tellExcept(message, null);
    }

    private void tellExcept(String message, UUID except) {
        String line = Messages.text(message);
        for (Player player : audience()) {
            if (except == null || !except.equals(player.getUniqueId())) {
                player.sendMessage(line);
            }
        }
    }

    private Set<Player> audience() {
        Set<Player> players = new HashSet<>();
        addOnline(players, queue);
        addOnline(players, nextQueue);
        addOnline(players, racers.keySet());
        World world = world();
        if (world != null && arena.spectateArea() != null) {
            for (Player player : world.getPlayers()) {
                if (ArenaManager.contains(arena.spectateArea(), player.getLocation())) {
                    players.add(player);
                }
            }
        }
        return players;
    }

    private static void addOnline(Set<Player> players, Iterable<UUID> ids) {
        for (UUID id : ids) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                players.add(player);
            }
        }
    }

    private void refreshBar() {
        if (state == RaceState.IDLE) {
            hideBar();
            return;
        }
        if (bar == null) {
            bar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SOLID);
        }
        bar.setVisible(true);
        switch (state) {
            case LOBBY -> {
                bar.setColor(BarColor.YELLOW);
                if (queue.size() < arena.minPlayers()) {
                    bar.setTitle("Waiting for racers (" + queue.size() + "/" + arena.minPlayers() + ")");
                    bar.setProgress(1.0);
                } else {
                    int shown = Math.max(secondsLeft, 0);
                    bar.setTitle("Race starts in " + shown + "s (" + racersLabel(queue.size()) + ")");
                    bar.setProgress(Math.max(0.0, Math.min(1.0, shown / (double) arena.lobbySeconds())));
                }
            }
            case COUNTDOWN -> {
                bar.setColor(BarColor.RED);
                bar.setTitle(countNumber > 0 ? Integer.toString(countNumber) : "START");
                bar.setProgress(Math.max(0.0, Math.min(1.0, countNumber / 3.0)));
            }
            case RACING -> {
                bar.setColor(BarColor.GREEN);
                int best = 0;
                for (Racer racer : racers.values()) {
                    best = Math.max(best, racer.progress().nextIndex());
                }
                int total = Math.max(1, arena.checkpoints().size());
                bar.setTitle("Racing");
                bar.setProgress(Math.max(0.0, Math.min(1.0, best / (double) total)));
            }
            case RESULTS -> {
                bar.setColor(BarColor.WHITE);
                bar.setTitle("Race finished");
                bar.setProgress(1.0);
            }
            default -> hideBar();
        }
        Set<Player> watching = audience();
        for (Player player : new ArrayList<>(bar.getPlayers())) {
            if (!watching.contains(player)) {
                bar.removePlayer(player);
            }
        }
        for (Player player : watching) {
            bar.addPlayer(player);
        }
    }

    private void hideBar() {
        if (bar != null) {
            bar.removeAll();
            bar.setVisible(false);
        }
    }

    private void teleportSpectator(Player player) {
        Location location = spectatorLocation();
        if (location != null) {
            player.teleport(location);
            player.setFallDistance(0f);
        }
    }

    private Location spectatorLocation() {
        World world = world();
        if (world == null || arena.spectatorSpawn() == null) {
            return null;
        }
        return Locations.of(world, arena.spectatorSpawn());
    }

    private World world() {
        if (arena.world() == null) {
            return null;
        }
        return Bukkit.getWorld(arena.world());
    }

    private Racer racerByBoat(UUID boatId) {
        for (Racer racer : racers.values()) {
            if (boatId.equals(racer.boatId())) {
                return racer;
            }
        }
        return null;
    }

    private static String racersLabel(int count) {
        return count == 1 ? "1 racer" : count + " racers";
    }
}
