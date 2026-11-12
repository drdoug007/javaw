package nebula;

/** How a hunting drone picks the tile it heads for. */
interface Behavior {
    int target(Game game, Drone self);

    /** Called when the drone is placed in the dock for a new life or sector. */
    default void reset() { }

    /** Heads for the next junction on the comet's current heading, to meet it there. */
    final class Interceptor implements Behavior {
        @Override public int target(Game game, Drone self) {
            Player comet = game.player;
            int col = comet.col(), row = comet.row(), direction = comet.dir;
            if (direction == Actor.NONE) return Maze.index(col, row);
            for (int i = 0; i < Maze.WIDTH && game.maze.openForPlayer(col + Maze.DX[direction], row + Maze.DY[direction]); i++) {
                col += Maze.DX[direction];
                row += Maze.DY[direction];
                if (game.maze.exits(col, row) > 2) break;
            }
            return Maze.index(col, row);
        }
    }

    /** Picks a fresh open tile every few seconds. */
    final class Wanderer implements Behavior {
        private int goal = -1, until;
        @Override public void reset() { goal = -1; }
        @Override public int target(Game game, Drone self) {
            if (goal < 0 || game.frame >= until || self.tile() == goal) {
                do goal = game.random(Maze.WIDTH * Maze.HEIGHT);
                while (!game.maze.openForPlayer(goal % Maze.WIDTH, goal / Maze.WIDTH));
                until = game.frame + 240;
            }
            return goal;
        }
    }

    /** Guards a post and hunts only when the comet comes within {@code range} maze steps of it. */
    final class Warden implements Behavior {
        private final int post, range;
        Warden(int col, int row, int range) { post = Maze.index(col, row); this.range = range; }
        @Override public int target(Game game, Drone self) {
            int[] distance = game.distanceFrom(post, false);
            return distance[game.player.tile()] <= range ? game.player.tile() : post;
        }
    }
}
