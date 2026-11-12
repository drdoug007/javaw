package nebula;

/** Steers toward the nearest stardust, avoiding tiles next to a dangerous drone when it can. Deterministic. */
final class Autopilot {
    private final int[] previous = new int[Maze.WIDTH * Maze.HEIGHT], queue = new int[Maze.WIDTH * Maze.HEIGHT];

    int choose(Game game) {
        int direction = search(game, true);
        return direction != Actor.NONE ? direction : search(game, false);
    }

    private int search(Game game, boolean careful) {
        java.util.Arrays.fill(previous, -1);
        int start = game.player.tile(), head = 0, tail = 0;
        previous[start] = start;
        queue[tail++] = start;
        while (head < tail) {
            int cell = queue[head++];
            int kind = game.maze.at(cell % Maze.WIDTH, cell / Maze.WIDTH);
            if (cell != start && (kind == Maze.DUST || kind == Maze.CORE)) return firstStep(start, cell);
            for (int d = 0; d < 4; d++) {
                int col = cell % Maze.WIDTH + Maze.DX[d], row = cell / Maze.WIDTH + Maze.DY[d], next = Maze.index(col, row);
                if (previous[next] >= 0 || !game.maze.openForPlayer(col, row) || careful && threatened(game, next)) continue;
                previous[next] = cell;
                queue[tail++] = next;
            }
        }
        return Actor.NONE;
    }

    private static boolean threatened(Game game, int cell) {
        for (Drone drone : game.drones) {
            if (!drone.dangerous()) continue;
            int dx = Math.abs(drone.col() - cell % Maze.WIDTH), dy = Math.abs(drone.row() - cell / Maze.WIDTH);
            if (dx + dy <= 2) return true;
        }
        return false;
    }

    private int firstStep(int start, int goal) {
        int cell = goal;
        while (previous[cell] != start) cell = previous[cell];
        for (int d = 0; d < 4; d++)
            if (Maze.index(start % Maze.WIDTH + Maze.DX[d], start / Maze.WIDTH + Maze.DY[d]) == cell) return d;
        return Actor.NONE;
    }
}
