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
}
