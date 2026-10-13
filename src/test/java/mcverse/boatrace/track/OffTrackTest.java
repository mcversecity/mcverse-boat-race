package mcverse.boatrace.track;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OffTrackTest {
    @Test
    void recoversOnceTheGraceRunsOut() {
        OffTrackGrace grace = new OffTrackGrace(10);
        for (int tick = 0; tick < 9; tick++) {
            assertFalse(grace.shouldRecover(true));
        }
        assertTrue(grace.shouldRecover(true));
    }

    @Test
    void beingOnTrackResetsTheGrace() {
        OffTrackGrace grace = new OffTrackGrace(2);
        assertFalse(grace.shouldRecover(true));
        assertFalse(grace.shouldRecover(false));
        assertFalse(grace.shouldRecover(true));
        assertTrue(grace.shouldRecover(true));
    }

    @Test
    void picksTheNewestSafeSample() {
        Sample newest = new Sample(3, 64, 0, 90);
        Sample older = new Sample(1, 64, 0, 90);
        Sample safe = OffTrack.firstSafe(List.of(newest, older), sample -> sample.x() < 2);
        assertEquals(older, safe);
    }

    @Test
    void returnsNullWhenNothingIsSafe() {
        assertNull(OffTrack.firstSafe(List.of(new Sample(0, 0, 0, 0)), sample -> false));
    }

    @Test
    void trailKeepsTheNewestPoints() {
        Trail trail = new Trail(2);
        trail.add(new Sample(1, 0, 0, 0));
        trail.add(new Sample(2, 0, 0, 0));
        trail.add(new Sample(3, 0, 0, 0));
        assertEquals(List.of(new Sample(3, 0, 0, 0), new Sample(2, 0, 0, 0)), trail.newestFirst());
    }

    @Test
    void trailSkipsASampleThatBarelyMoved() {
        Trail trail = new Trail(5);
        trail.add(new Sample(0, 64, 0, 0));
        trail.addIfMoved(new Sample(0.1, 64, 0, 10), 0.5);
        assertEquals(1, trail.newestFirst().size());
    }
}
