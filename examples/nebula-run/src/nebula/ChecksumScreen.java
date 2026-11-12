package nebula;

/** Hashes every draw and sound command, so two runs can be compared command for command. */
final class ChecksumScreen implements Screen {
    long hash = 17, commands;

    private void mix(int kind, int... values) {
        commands++;
        hash = hash * 31 + kind;
        for (int value : values) hash = hash * 1_000_003L + value;
    }

    @Override public void layer(int index) { mix(1, index); }
    @Override public void clear(int argb) { mix(2, argb); }
    @Override public void rect(int x, int y, int width, int height, int argb) { mix(3, x, y, width, height, argb); }
    @Override public void circle(int x, int y, int radius, int argb) { mix(4, x, y, radius, argb); }
    @Override public void diamond(int x, int y, int radius, int argb) { mix(5, x, y, radius, argb); }
    @Override public void hud(int score, int lives, int level, int state) { mix(6, score, lives, level, state); }
    @Override public void tone(int hertz, int millis, int wave) { mix(7, hertz, millis, wave); }
}
