package mcverse.boatrace.race;

import mcverse.boatrace.arena.Pose;
import mcverse.boatrace.track.OffTrackGrace;
import mcverse.boatrace.track.Sample;
import mcverse.boatrace.track.Trail;

import java.util.UUID;

public final class Racer {
    private final UUID playerId;
    private final String name;
    private final int slotNumber;
    private final Pose start;
    private final Progress progress;
    private final Trail trail;
    private final OffTrackGrace grace;
    private UUID boatId;
    private Sample lastCheckpoint;
    private int cooldown;
    private boolean finished;
    private int place;

    public Racer(UUID playerId, String name, int slotNumber, Pose start, int checkpoints, int laps, int trailSamples, int graceTicks) {
        this.playerId = playerId;
        this.name = name;
        this.slotNumber = slotNumber;
        this.start = start;
        this.progress = new Progress(checkpoints, laps);
        this.trail = new Trail(trailSamples);
        this.grace = new OffTrackGrace(graceTicks);
        this.trail.add(Sample.fromPose(start));
    }

    public UUID playerId() {
        return playerId;
    }

    public String name() {
        return name;
    }

    public int slotNumber() {
        return slotNumber;
    }

    public Pose start() {
        return start;
    }

    public Progress progress() {
        return progress;
    }

    public Trail trail() {
        return trail;
    }

    public OffTrackGrace grace() {
        return grace;
    }

    public UUID boatId() {
        return boatId;
    }

    public void setBoatId(UUID boatId) {
        this.boatId = boatId;
    }

    public Sample lastCheckpoint() {
        return lastCheckpoint;
    }

    public void setLastCheckpoint(Sample lastCheckpoint) {
        this.lastCheckpoint = lastCheckpoint;
    }

    public int cooldown() {
        return cooldown;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public void tickCooldown() {
        if (cooldown > 0) {
            cooldown--;
        }
    }

    public boolean finished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public int place() {
        return place;
    }

    public void setPlace(int place) {
        this.place = place;
    }
}
