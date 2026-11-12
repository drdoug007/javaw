package nebula;

/**
 * A headless game driven by {@link Autopilot}. The same code runs on the JVM (jvm/nebula/SimulationMain.java) and in
 * Wasm (the export {@code nebula.Simulation.run}), and both must produce the same draw-command hash.
 */
public final class Simulation {
    private Simulation() {}

    /** Plays {@code frames} frames from {@code seed}, rendering every other frame; returns the hash and final score. */
    public static String run(int seed, int frames) {
        ChecksumScreen screen = new ChecksumScreen();
        Game game = new Game(seed);
        Autopilot pilot = new Autopilot();
        int games = 1, levels = 0, lastLevel = 1;
        for (int i = 0; i < frames; i++) {
            if (game.state == Game.OVER) { game.press(); games++; lastLevel = 1; }
            // Steering only at tile centers: a plan made mid-tile would flip as the rounded tile changes.
            if (game.state == Game.PLAYING && (game.player.centered() || game.player.dir == Actor.NONE)) game.steer(pilot.choose(game));
            game.update();
            if (game.level != lastLevel) { levels++; lastLevel = game.level; }
            if (i % 2 == 0) game.render(screen);
        }
        return "hash=" + Long.toHexString(screen.hash) + " commands=" + screen.commands + " score=" + game.score
                + " level=" + game.level + " lives=" + game.lives + " games=" + games + " levelsCleared=" + levels
                + " best=" + game.best;
    }
}
