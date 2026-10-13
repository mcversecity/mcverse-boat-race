package mcverse.boatrace.track;

import java.util.function.Predicate;

public final class OffTrack {
    private OffTrack() {
    }

    public static Sample firstSafe(Iterable<Sample> newestFirst, Predicate<Sample> safe) {
        for (Sample sample : newestFirst) {
            if (safe.test(sample)) {
                return sample;
            }
        }
        return null;
    }
}
