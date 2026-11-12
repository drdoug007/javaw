package nebula;

import java.util.ArrayList;
import java.util.List;

/** Nebula Run: a comet gathers stardust in a maze while four drones hunt it. One {@link #update} is one 60 Hz frame. */
public final class Game {
    public static final int READY = 0, PLAYING = 1, CAUGHT = 2, CLEARED = 3, OVER = 4, PAUSED = 5;
    static final int TILE = 24, STEP_PIXELS = TILE / Actor.STEPS;
    /** Pickups: stardust and nova cores; caught drones score {@code DRONE_POINTS * combo}. */
    static final int DUST_POINTS = 5, CORE_POINTS = 25, DRONE_POINTS = 150, CRYSTAL_POINTS = 250;
    /** The crystal appears once 30% and again once 65% of a sector's pickups are gone, for {@value #CRYSTAL_FRAMES} frames. */
    static final int CRYSTAL_FRAMES = 360;

    final Maze maze = new Maze();
    final Player player = new Player(this);
    final List<Drone> drones = new ArrayList<>();
    int score, lives, level, state, timer, frame, dimTimer, combo, collected, total, bonusTimer, bonusTile, best;
    private long seed;
    private boolean mazeDirty = true;
    private final int[] distance = new int[Maze.WIDTH * Maze.HEIGHT], queue = new int[Maze.WIDTH * Maze.HEIGHT];
    private int distanceTarget = -1, distanceKind;
    private final List<int[]> sounds = new ArrayList<>();

    public Game(int seed) {
        this.seed = seed;
        Behavior hunter = (game, self) -> game.player.tile();
        // Vex pursues directly and a little faster; Lumen meets the comet at its next junction; Drift roams;
        // Bastion guards the east nova core.
        drones.add(new Drone(this, "Vex", 0xFFE040FB, 10, 8, 0, 6, hunter));
        drones.add(new Drone(this, "Lumen", 0xFFB2FF59, 9, 8, 150, 0, new Behavior.Interceptor()));
        drones.add(new Drone(this, "Drift", 0xFFFFC94D, 11, 8, 330, 0, new Behavior.Wanderer()));
        drones.add(new Drone(this, "Bastion", 0xFF8FB8FF, 10, 9, 510, 0, new Behavior.Warden(19, 13, 6)));
        newGame();
    }

    public void newGame() {
        score = 0; lives = 3; level = 1;
        startLevel();
    }

    private void startLevel() {
        maze.reset();
        collected = 0;
        total = maze.remaining;
        mazeDirty = true;
        startLife();
    }

    private void startLife() {
        player.place(10, 15);
        for (Drone drone : drones) drone.reset();
        frame = 0; dimTimer = 0; bonusTimer = 0; combo = 0;
        state = READY; timer = 120;
    }

    int random(int bound) {
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        return (int) ((seed >>> 33) % bound);
    }

    /** Shared breadth-first distances; recomputed only when the target or the gate rule changes. */
    int[] distanceFrom(int target, boolean dock) {
        int kind = dock ? 1 : 0;
        if (target != distanceTarget || kind != distanceKind) {
            maze.distances(target, dock, distance, queue);
            distanceTarget = target; distanceKind = kind;
        }
        return distance;
    }

    int playerSpeed() { return Math.min(Actor.FULL + 4 * (level - 1), 120); }
    int droneSpeed() { return Math.min(86 + 5 * (level - 1), 116); }
    boolean dimmed() { return dimTimer > 0; }

    /** Ignored while paused or between lives, so a pause cannot be used to turn around. */
    public void steer(int direction) {
        if (direction >= 0 && direction < 4 && (state == PLAYING || state == READY)) player.steer(direction);
    }

    /** Enter/space: start again after a game over, otherwise pause or resume. */
    public void press() {
        if (state == OVER) newGame();
        else if (state == PLAYING) state = PAUSED;
        else if (state == PAUSED) state = PLAYING;
    }

    public void update() {
        switch (state) {
            case READY -> { if (--timer <= 0) state = PLAYING; }
            case PLAYING -> play();
            case CAUGHT -> {
                if (--timer > 0) return;
                if (lives <= 0) { state = OVER; best = Math.max(best, score); }
                else startLife();
            }
            case CLEARED -> {
                if (timer % 20 == 0) mazeDirty = true;
                if (--timer > 0) return;
                level++;
                startLevel();
            }
            default -> { }
        }
    }

    private void play() {
        frame++;
        for (Drone drone : drones) {
            drone.tick();
            if (drone.mode == Drone.DOCKED && frame >= drone.release - 30 * (level - 1)) drone.mode = Drone.LEAVING;
        }
        player.move(playerSpeed());
        touch();
        for (Drone drone : drones) drone.move(drone.speed());
        touch();
        if (dimTimer > 0 && --dimTimer == 0)
            for (Drone drone : drones) if (drone.mode == Drone.DIMMED) drone.mode = Drone.HUNTING;
        if (bonusTimer > 0) bonusTimer--;
        if (state == PLAYING && maze.remaining == 0) {
            state = CLEARED; timer = 150; mazeDirty = true;
            sound(523, 120, 3); sound(659, 120, 3); sound(784, 240, 3);
        }
    }

    /** Collection at a tile center. */
    void collect(int col, int row) {
        int kind = maze.at(col, row);
        if (kind == Maze.DUST || kind == Maze.CORE) {
            maze.clear(col, row);
            maze.remaining--;
            collected++;
            if (collected == total * 30 / 100 || collected == total * 65 / 100) placeCrystal();
        }
        // Pickups climb a pentatonic scale, rising an octave at each half of the sector.
        if (kind == Maze.DUST) { score += DUST_POINTS; sound(PENTATONIC[collected % 5] << (2 * collected / total), 45, 3); }
        if (kind == Maze.CORE) {
            score += CORE_POINTS; combo = 0;
            dimTimer = Math.max(150, 420 - 45 * (level - 1));
            for (Drone drone : drones) if (drone.mode == Drone.HUNTING) drone.mode = Drone.DIMMED;
            sound(220, 300, 2);
        }
        if (bonusTimer > 0 && Maze.index(col, row) == bonusTile) {
            score += CRYSTAL_POINTS * level; bonusTimer = 0;
            sound(1320, 160, 1);
        }
    }

    private static final int[] PENTATONIC = {262, 294, 330, 392, 440};

    /** On a random open tile away from the comet. */
    private void placeCrystal() {
        int tile;
        do tile = random(Maze.WIDTH * Maze.HEIGHT);
        while (!maze.openForPlayer(tile % Maze.WIDTH, tile / Maze.WIDTH) || tile == player.tile());
        bonusTile = tile;
        bonusTimer = CRYSTAL_FRAMES;
    }

    private void touch() {
        if (state != PLAYING) return;
        for (Drone drone : drones) {
            if (drone.tile() != player.tile()) continue;
            if (drone.mode == Drone.DIMMED) {
                combo++;
                score += DRONE_POINTS * combo;
                drone.caught();
                sound(330 + 110 * combo, 180, 1);
            } else if (drone.dangerous()) {
                lives--;
                state = CAUGHT; timer = 100;
                sound(392, 150, 2); sound(262, 150, 2); sound(131, 400, 2);
                return;
            }
        }
    }

    private void sound(int hertz, int millis, int wave) { sounds.add(new int[]{hertz, millis, wave}); }

    public void render(Screen screen) {
        if (mazeDirty) { drawMaze(screen); mazeDirty = false; }
        screen.layer(0);
        screen.clear(0);
        int pulse = (frame / 8) % 2;
        for (int row = 0; row < Maze.HEIGHT; row++)
            for (int col = 0; col < Maze.WIDTH; col++) {
                int kind = maze.at(col, row), cx = col * TILE + TILE / 2, cy = row * TILE + TILE / 2;
                if (kind == Maze.DUST) screen.rect(cx - 2, cy - 2, 4, 4, 0xFFFFE3A3);
                else if (kind == Maze.CORE) screen.circle(cx, cy, 6 + pulse, 0xFF5CF2FF);
            }
        if (bonusTimer > 0)
            screen.diamond(bonusTile % Maze.WIDTH * TILE + TILE / 2, bonusTile / Maze.WIDTH * TILE + TILE / 2, 8 + pulse, 0xFF5CFFA0);
        drawComet(screen);
        if (state != CAUGHT && state != CLEARED) for (Drone drone : drones) drawDrone(screen, drone);
        screen.hud(score, lives, level, state);
        for (int[] sound : sounds) screen.tone(sound[0], sound[1], sound[2]);
        sounds.clear();
    }

    private void drawMaze(Screen screen) {
        screen.layer(1);
        screen.clear(0xFF070B1A);
        boolean flash = state == CLEARED && (timer / 20) % 2 == 0;
        int fill = flash ? 0xFF3B2F8F : 0xFF1A1446, edge = flash ? 0xFFFFFFFF : 0xFF7C5CFF;
        for (int row = 0; row < Maze.HEIGHT; row++)
            for (int col = 0; col < Maze.WIDTH; col++) {
                int kind = maze.at(col, row), x = col * TILE, y = row * TILE;
                if (kind == Maze.GATE) { screen.rect(x, y + TILE / 2 - 2, TILE, 4, 0xFFFF6AD5); continue; }
                if (kind != Maze.WALL) continue;
                screen.rect(x, y, TILE, TILE, fill);
                // A glowing edge on every side that faces a corridor.
                if (row > 0 && maze.at(col, row - 1) != Maze.WALL) screen.rect(x, y, TILE, 3, edge);
                if (row < Maze.HEIGHT - 1 && maze.at(col, row + 1) != Maze.WALL) screen.rect(x, y + TILE - 3, TILE, 3, edge);
                if (col > 0 && maze.at(col - 1, row) != Maze.WALL) screen.rect(x, y, 3, TILE, edge);
                if (col < Maze.WIDTH - 1 && maze.at(col + 1, row) != Maze.WALL) screen.rect(x + TILE - 3, y, 3, TILE, edge);
            }
    }

    private void drawComet(Screen screen) {
        int cx = player.x * STEP_PIXELS + TILE / 2, cy = player.y * STEP_PIXELS + TILE / 2;
        if (state == CAUGHT) {
            // The comet bursts into shrinking sparks.
            int radius = Math.max(1, timer / 10);
            for (int d = 0; d < 4; d++)
                screen.circle(cx + Maze.DX[d] * (100 - timer) / 4, cy + Maze.DY[d] * (100 - timer) / 4, radius, 0xFFFF9A3C);
            return;
        }
        if (player.dir != Actor.NONE) {
            int[] alpha = {0xAA, 0x77, 0x44};
            for (int i = 0; i < 3; i++)
                screen.circle(cx - Maze.DX[player.dir] * 7 * (i + 1), cy - Maze.DY[player.dir] * 7 * (i + 1), 7 - 2 * i,
                        alpha[i] << 24 | 0xFF9A3C);
        }
        screen.circle(cx, cy, 9, 0xFFFFD36B);
        screen.circle(cx, cy, 5, 0xFFFFFFFF);
    }

    private void drawDrone(Screen screen, Drone drone) {
        if (drone.burst > 0) {
            // A caught drone bursts into sparks of its own color, which fly apart and shrink.
            int spread = Drone.BURST_FRAMES - drone.burst, bx = drone.burstX * STEP_PIXELS + TILE / 2, by = drone.burstY * STEP_PIXELS + TILE / 2;
            for (int d = 0; d < 4; d++) {
                int diagonal = (d + 1) % 4;
                screen.circle(bx + (Maze.DX[d] + Maze.DX[diagonal]) * spread, by + (Maze.DY[d] + Maze.DY[diagonal]) * spread,
                        Math.max(1, drone.burst / 9), drone.color);
            }
        }
        int cx = drone.x * STEP_PIXELS + TILE / 2, cy = drone.y * STEP_PIXELS + TILE / 2;
        if (drone.mode == Drone.REFORMING) {
            // Re-forming: the outline grows back in the dock.
            int radius = 3 + 8 * (Drone.REFORM_FRAMES - drone.reform) / Drone.REFORM_FRAMES;
            screen.diamond(cx, cy, radius, drone.color);
            screen.diamond(cx, cy, Math.max(0, radius - 3), 0xFF070B1A);
            return;
        }
        if (drone.mode == Drone.DIMMED) {
            // Dimmed: a hollow outline of its own color, pulsing faster as the effect runs out.
            int radius = dimTimer < 120 ? 9 + (dimTimer / 6) % 3 : 11;
            screen.diamond(cx, cy, radius, drone.color);
            screen.diamond(cx, cy, radius - 3, 0xFF070B1A);
            return;
        }
        screen.diamond(cx, cy, 11, drone.color);
        // One lens, looking where the drone is heading.
        int lookX = drone.dir == Actor.NONE ? 0 : Maze.DX[drone.dir] * 2, lookY = drone.dir == Actor.NONE ? 0 : Maze.DY[drone.dir] * 2;
        screen.circle(cx, cy, 5, 0xFFFFFFFF);
        screen.circle(cx + lookX, cy + lookY, 2, 0xFF101020);
    }
}
