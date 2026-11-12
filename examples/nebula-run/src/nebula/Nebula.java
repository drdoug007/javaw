package nebula;

/** The browser entry points, exported as nebula.Nebula.*; the page calls them from requestAnimationFrame and key events. */
public final class Nebula {
    private static Game game;
    private static final Screen SCREEN = new CanvasScreen();

    private Nebula() {}

    public static void start(int seed) { game = new Game(seed); }
    /** 0 up, 1 left, 2 down, 3 right. */
    public static void steer(int direction) { game.steer(direction); }
    public static void press() { game.press(); }
    public static void update() { game.update(); }
    public static void render() { game.render(SCREEN); }
    public static int state() { return game.state; }
    public static int score() { return game.score; }
}
