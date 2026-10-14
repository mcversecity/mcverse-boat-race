package mcverse.boatrace.race;

public final class Progress {
    public enum Cross {
        NONE,
        LAP,
        FINISHED
    }

    private final int checkpoints;
    private final int laps;
    private int next;
    private int completed;
    private boolean onLine;

    public Progress(int checkpoints) {
        this(checkpoints, 1);
    }

    public Progress(int checkpoints, int laps) {
        this.checkpoints = checkpoints;
        this.laps = Math.max(1, laps);
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

    public Cross crossLine(boolean inside) {
        boolean was = onLine;
        onLine = inside;
        if (!inside || next < checkpoints) {
            return Cross.NONE;
        }
        if (was && completed > 0) {
            return Cross.NONE;
        }
        completed++;
        if (completed >= laps) {
            return Cross.FINISHED;
        }
        next = 0;
        return Cross.LAP;
    }

    public int nextIndex() {
        return next;
    }

    public int checkpoints() {
        return checkpoints;
    }

    public int laps() {
        return laps;
    }

    public int completedLaps() {
        return completed;
    }

    public int currentLap() {
        return Math.min(laps, completed + 1);
    }

    public int score() {
        if (checkpoints == 0) {
            return completed;
        }
        if (completed >= laps) {
            return checkpoints * laps + 1;
        }
        return completed * checkpoints + next;
    }

    public double fraction() {
        if (completed >= laps) {
            return 1.0;
        }
        if (checkpoints == 0) {
            return completed / (double) laps;
        }
        return (completed * (double) checkpoints + next) / (checkpoints * (double) laps);
    }
}
