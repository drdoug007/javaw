package nebula;

/**
 * Everything the game draws or plays. {@link CanvasScreen} sends each command to the browser through Wasm imports;
 * {@link ChecksumScreen} hashes them, so the JVM and Wasm runs can be compared command for command.
 * Colors are ARGB; coordinates are canvas pixels.
 */
public interface Screen {
    /** 0 = foreground, cleared and redrawn every frame; 1 = the maze, redrawn only when it changes. */
    void layer(int index);
    void clear(int argb);
    void rect(int x, int y, int width, int height, int argb);
    void circle(int x, int y, int radius, int argb);
    void diamond(int x, int y, int radius, int argb);
    void hud(int score, int lives, int level, int state);
    /** wave: 0 sine, 1 square, 2 sawtooth, 3 triangle. */
    void tone(int hertz, int millis, int wave);
}
