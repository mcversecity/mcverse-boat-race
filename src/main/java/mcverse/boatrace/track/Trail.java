package mcverse.boatrace.track;

import java.util.ArrayDeque;
import java.util.List;

public final class Trail {
    private final int limit;
    private final ArrayDeque<Sample> newestFirst = new ArrayDeque<>();

    public Trail(int limit) {
        this.limit = Math.max(1, limit);
    }

    public void add(Sample sample) {
        newestFirst.addFirst(sample);
        while (newestFirst.size() > limit) {
            newestFirst.removeLast();
        }
    }

    public void addIfMoved(Sample sample, double minDistance) {
        Sample newest = newestFirst.peekFirst();
        if (newest != null && newest.distanceSquared(sample) < minDistance * minDistance) {
            return;
        }
        add(sample);
    }

    public List<Sample> newestFirst() {
        return List.copyOf(newestFirst);
    }
}
