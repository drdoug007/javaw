package nebula;

/** The comet. It reverses at once, turns at tile centers and remembers the last steering input. */
final class Player extends Actor {
    Player(Game game) { super(game); }

    void steer(int direction) {
        wanted = direction;
        if (dir != NONE && direction == opposite(dir)) dir = direction;
    }

    private boolean open(int direction) {
        return game.maze.openForPlayer(col() + Maze.DX[direction], row() + Maze.DY[direction]);
    }

    @Override void arrive() {
        game.collect(col(), row());
        if (wanted != NONE && open(wanted)) dir = wanted;
        else if (dir != NONE && !open(dir)) dir = NONE;
    }
}
