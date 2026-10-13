package mcverse.boatrace.race;

public final class Progress {
    private final int checkpoints;
    private int next;

    public Progress(int checkpoints) {
        this.checkpoints = checkpoints;
    }

    public boolean advanceIfExpected(int index) {
        if (index != next) {
            return false;
        }
        next++;
        return true;
    }

    public boolean canFinish() {
        return next >= checkpoints;
    }

    public int nextIndex() {
        return next;
    }

    public int checkpoints() {
        return checkpoints;
    }
}
