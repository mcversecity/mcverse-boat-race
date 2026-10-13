package mcverse.boatrace.arena;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SelectionManager {
    private final Map<UUID, Selection> selections = new HashMap<>();

    public Selection get(UUID playerId) {
        return selections.computeIfAbsent(playerId, ignored -> new Selection());
    }
}
