package nebula;

/** The maze: 21 by 21 tiles, mirrored left to right, with one vertical tunnel that wraps top to bottom. */
final class Maze {
    static final int WIDTH = 21, HEIGHT = 21;
    static final int WALL = 0, DUST = 1, CORE = 2, EMPTY = 3, GATE = 4, DOCK = 5;
    /** '#' wall, '.' stardust, 'o' nova core, 'T' tunnel, '-' dock gate, 'D' dock. */
    private static final String[] LAYOUT = {
            "##########T##########",
            "#o.......#.#.......o#",
            "#.##.###.#.#.###.##.#",
            "#.##.#.........#.##.#",
            "#....#.#######.#....#",
            "####.#..#####..#.####",
            "#....##.......##....#",
            "#.##....##-##....##.#",
            "#.##.##.#DDD#.##.##.#",
            "#....#..#DDD#..#....#",
            "###.##.#######.##.###",
            "#...................#",
            "#.#.#.#.##.##.#.#.#.#",
            "#o..#...#...#...#..o#",
            "##.##.#.#.#.#.#.##.##",
            "#..#.............#..#",
            "#.##.#.#.#.#.#.#.##.#",
            "#......#.....#......#",
            "#.####.###.###.####.#",
            "#...................#",
            "##########T##########",
    };
    static final int[] DX = {0, -1, 0, 1}, DY = {-1, 0, 1, 0};

    private final int[] cells = new int[WIDTH * HEIGHT];
    int remaining;

    Maze() { reset(); }

    void reset() {
        remaining = 0;
        for (int row = 0; row < HEIGHT; row++)
            for (int col = 0; col < WIDTH; col++) {
                int kind = switch (LAYOUT[row].charAt(col)) {
                    case '#' -> WALL;
                    case '.' -> DUST;
                    case 'o' -> CORE;
                    case '-' -> GATE;
                    case 'D' -> DOCK;
                    default -> EMPTY;
                };
                cells[row * WIDTH + col] = kind;
                if (kind == DUST || kind == CORE) remaining++;
            }
    }

    static int wrapCol(int col) { return ((col % WIDTH) + WIDTH) % WIDTH; }
    static int wrapRow(int row) { return ((row % HEIGHT) + HEIGHT) % HEIGHT; }
    static int index(int col, int row) { return wrapRow(row) * WIDTH + wrapCol(col); }

    int at(int col, int row) { return cells[index(col, row)]; }
    void clear(int col, int row) { cells[index(col, row)] = EMPTY; }

    boolean openForPlayer(int col, int row) {
        int kind = at(col, row);
        return kind == DUST || kind == CORE || kind == EMPTY;
    }

    /** The number of open neighbors for the comet: more than two makes a junction. */
    int exits(int col, int row) {
        int count = 0;
        for (int d = 0; d < 4; d++) if (openForPlayer(col + DX[d], row + DY[d])) count++;
        return count;
    }

    /** Drones cross the gate and the dock only while leaving it. */
    boolean openForDrone(int col, int row, boolean dock) {
        int kind = at(col, row);
        return kind != WALL && (dock || kind != GATE && kind != DOCK);
    }

    /** Breadth-first distances from {@code target} into {@code distance}; unreachable tiles stay at MAX_VALUE. */
    void distances(int target, boolean dock, int[] distance, int[] queue) {
        java.util.Arrays.fill(distance, Integer.MAX_VALUE);
        int head = 0, tail = 0;
        distance[target] = 0;
        queue[tail++] = target;
        while (head < tail) {
            int cell = queue[head++], col = cell % WIDTH, row = cell / WIDTH;
            for (int d = 0; d < 4; d++) {
                int next = index(col + DX[d], row + DY[d]);
                if (distance[next] == Integer.MAX_VALUE && openForDrone(next % WIDTH, next / WIDTH, dock)) {
                    distance[next] = distance[cell] + 1;
                    queue[tail++] = next;
                }
            }
        }
    }
}
