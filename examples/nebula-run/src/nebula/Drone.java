package nebula;

/** A chaser. It never reverses on its own, and takes the turn with the shortest maze path to its target. */
final class Drone extends Actor {
    static final int DOCKED = 0, LEAVING = 1, HUNTING = 2, DIMMED = 3, REFORMING = 4;
    static final int GATE_OUT = Maze.index(10, 6);
    /** Frames a caught drone spends re-forming in the dock, and frames its spark burst stays visible. */
    static final int REFORM_FRAMES = 180, BURST_FRAMES = 36;
    final String name;
    final int color, homeCol, homeRow, release, bonusSpeed;
    private final Behavior behavior;
    int mode, reform, burstX, burstY, burst;

    Drone(Game game, String name, int color, int homeCol, int homeRow, int release, int bonusSpeed, Behavior behavior) {
        super(game);
        this.name = name; this.color = color; this.homeCol = homeCol; this.homeRow = homeRow;
        this.release = release; this.bonusSpeed = bonusSpeed; this.behavior = behavior;
    }

    void reset() { place(homeCol, homeRow); mode = DOCKED; burst = 0; behavior.reset(); }

    boolean dangerous() { return mode == HUNTING || mode == LEAVING; }

    /** Caught while dimmed: bursts where it was, then re-forms in the dock. */
    void caught() {
        burstX = x; burstY = y; burst = BURST_FRAMES;
        place(homeCol, homeRow);
        mode = REFORMING; reform = REFORM_FRAMES;
    }

    /** Once per playing frame, before moving. */
    void tick() {
        if (burst > 0) burst--;
        if (mode == REFORMING && --reform <= 0) mode = LEAVING;
    }

    int speed() {
        return switch (mode) {
            case DOCKED, REFORMING -> 0;
            case DIMMED -> FULL / 2;
            default -> game.droneSpeed() + (mode == HUNTING ? bonusSpeed : 0);
        };
    }

    @Override void arrive() {
        if (mode == LEAVING && tile() == GATE_OUT) mode = game.dimmed() ? DIMMED : HUNTING;
        boolean dock = mode == LEAVING;
        int target = switch (mode) {
            case LEAVING -> GATE_OUT;
            case HUNTING -> behavior.target(game, this);
            default -> -1;
        };
        dir = choose(target, dock);
    }

    private int choose(int target, boolean dock) {
        int[] options = new int[4];
        int count = 0;
        for (int d = 0; d < 4; d++)
            if (d != opposite(dir) && game.maze.openForDrone(col() + Maze.DX[d], row() + Maze.DY[d], dock)) options[count++] = d;
        if (count == 0) return dir != NONE && game.maze.openForDrone(col() + Maze.DX[opposite(dir)], row() + Maze.DY[opposite(dir)], dock) ? opposite(dir) : NONE;
        if (target < 0) return options[game.random(count)];
        int[] distance = game.distanceFrom(target, dock);
        int best = options[0], bestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < count; i++) {
            int d = options[i], next = distance[Maze.index(col() + Maze.DX[d], row() + Maze.DY[d])];
            if (next < bestDistance) { bestDistance = next; best = d; }
        }
        return best;
    }
}
