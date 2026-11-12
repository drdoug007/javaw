package nebula;

/** Something that moves through the maze. Positions are in steps: a tile is {@value #STEPS} steps. */
abstract class Actor {
    static final int STEPS = 8, NONE = -1;
    /** Speeds are in hundredths of a step per frame. */
    static final int FULL = 100;
    final Game game;
    int x, y, dir = NONE, wanted = NONE;
    private int progress;

    Actor(Game game) { this.game = game; }

    void place(int col, int row) { x = col * STEPS; y = row * STEPS; dir = NONE; wanted = NONE; progress = 0; }
    int col() { return Maze.wrapCol((x + STEPS / 2) / STEPS); }
    int row() { return Maze.wrapRow((y + STEPS / 2) / STEPS); }
    int tile() { return Maze.index(col(), row()); }
    boolean centered() { return x % STEPS == 0 && y % STEPS == 0; }
    static int opposite(int direction) { return direction == NONE ? NONE : (direction + 2) & 3; }

    /** Advances by {@code speed} hundredths of a step, deciding the direction at each tile center. */
    void move(int speed) {
        progress += speed;
        while (progress >= FULL) {
            progress -= FULL;
            if (centered()) arrive();
            if (dir == NONE) continue;
            x += Maze.DX[dir];
            y += Maze.DY[dir];
            int width = Maze.WIDTH * STEPS, height = Maze.HEIGHT * STEPS;
            x = (x + width) % width;
            y = (y + height) % height;
        }
    }

    /** Called on a tile center; sets {@link #dir}. */
    abstract void arrive();
}
