package mcverse.boatrace.arena;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CuboidTest {
    @Test
    void normalizesCornersAndIncludesTheEdges() {
        Cuboid cuboid = Cuboid.between("world", new BlockPoint(5, 10, 5), new BlockPoint(1, 2, 3));
        assertEquals(1, cuboid.minX());
        assertEquals(5, cuboid.maxX());
        assertTrue(cuboid.contains("world", 1.2, 2.0, 3.9));
        assertTrue(cuboid.contains("world", 5.9, 10.1, 5.0));
        assertFalse(cuboid.contains("world", 6.0, 10, 5));
        assertFalse(cuboid.contains("other", 1, 2, 3));
    }
}
