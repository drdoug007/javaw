package nebula;

/**
 * Draws through Wasm imports. Each native method is bound with javaw-bytecode's --native-import; the browser page
 * supplies the imports as small canvas and WebAudio functions. Only numbers cross the boundary.
 */
final class CanvasScreen implements Screen {
    private static native void selectLayer(int index);
    private static native void fill(int argb);
    private static native void fillRect(int x, int y, int width, int height, int argb);
    private static native void fillCircle(int x, int y, int radius, int argb);
    private static native void fillDiamond(int x, int y, int radius, int argb);
    private static native void showHud(int score, int lives, int level, int state);
    private static native void playTone(int hertz, int millis, int wave);

    @Override public void layer(int index) { selectLayer(index); }
    @Override public void clear(int argb) { fill(argb); }
    @Override public void rect(int x, int y, int width, int height, int argb) { fillRect(x, y, width, height, argb); }
    @Override public void circle(int x, int y, int radius, int argb) { fillCircle(x, y, radius, argb); }
    @Override public void diamond(int x, int y, int radius, int argb) { fillDiamond(x, y, radius, argb); }
    @Override public void hud(int score, int lives, int level, int state) { showHud(score, lives, level, state); }
    @Override public void tone(int hertz, int millis, int wave) { playTone(hertz, millis, wave); }
}
