package mcverse.boatrace.track;

public final class OffTrackGrace {
    private final int limit;
    private int ticks;

    public OffTrackGrace(int limit) {
        this.limit = Math.max(1, limit);
    }

    public boolean shouldRecover(boolean offTrack) {
        if (!offTrack) {
            ticks = 0;
            return false;
        }
        ticks++;
        return ticks >= limit;
    }

    public void reset() {
        ticks = 0;
    }
}
