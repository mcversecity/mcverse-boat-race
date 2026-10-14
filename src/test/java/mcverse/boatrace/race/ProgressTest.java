package mcverse.boatrace.race;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressTest {
    @Test
    void checkpointsMustBePassedInOrder() {
        Progress progress = new Progress(2);
        assertFalse(progress.advanceIfExpected(1));
        assertEquals(0, progress.nextIndex());
        assertTrue(progress.advanceIfExpected(0));
        assertFalse(progress.canFinish());
        assertTrue(progress.advanceIfExpected(1));
        assertTrue(progress.canFinish());
        assertFalse(progress.advanceIfExpected(1));
    }

    @Test
    void aCourseWithNoCheckpointsCanFinish() {
        assertTrue(new Progress(0).canFinish());
    }

    @Test
    void anEarlyFinishCrossingDoesNothing() {
        Progress progress = new Progress(2, 3);
        assertEquals(Progress.Cross.NONE, progress.crossLine(true));
        assertEquals(0, progress.completedLaps());
        assertEquals(0, progress.nextIndex());
    }

    @Test
    void eachLapRequiresTheCheckpointsAgain() {
        Progress progress = new Progress(2, 3);
        completeLap(progress);
        assertEquals(Progress.Cross.LAP, progress.crossLine(true));
        assertEquals(1, progress.completedLaps());
        assertEquals(0, progress.nextIndex());
        assertEquals(2, progress.currentLap());
        assertFalse(progress.canFinish());

        assertEquals(Progress.Cross.NONE, progress.crossLine(true));
        progress.crossLine(false);
        completeLap(progress);
        assertEquals(Progress.Cross.LAP, progress.crossLine(true));
        assertEquals(2, progress.completedLaps());

        progress.crossLine(false);
        completeLap(progress);
        assertEquals(Progress.Cross.FINISHED, progress.crossLine(true));
        assertEquals(3, progress.completedLaps());
        assertEquals(1.0, progress.fraction());
    }

    @Test
    void stayingOnTheLineDoesNotAddLaps() {
        Progress progress = new Progress(1, 2);
        assertTrue(progress.advanceIfExpected(0));
        assertEquals(Progress.Cross.LAP, progress.crossLine(true));
        assertEquals(Progress.Cross.NONE, progress.crossLine(true));
        assertTrue(progress.advanceIfExpected(0));
        assertEquals(Progress.Cross.NONE, progress.crossLine(true));
        assertEquals(1, progress.completedLaps());
    }

    @Test
    void oneLapFinishesWhileAlreadyInside() {
        Progress progress = new Progress(1);
        assertEquals(Progress.Cross.NONE, progress.crossLine(true));
        assertTrue(progress.advanceIfExpected(0));
        assertEquals(Progress.Cross.FINISHED, progress.crossLine(true));
    }

    private static void completeLap(Progress progress) {
        assertTrue(progress.advanceIfExpected(0));
        assertTrue(progress.advanceIfExpected(1));
    }
}
