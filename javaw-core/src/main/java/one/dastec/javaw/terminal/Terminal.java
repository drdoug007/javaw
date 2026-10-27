package one.dastec.javaw.terminal;

import one.dastec.javaw.wasi.WASITerminal;

/**
 * Convenient facade for interactive Terminal and REPL operations in WebAssembly.
 */
public class Terminal {

    public static final String RESET = WASITerminal.RESET;
    public static final String BLACK = WASITerminal.BLACK;
    public static final String RED = WASITerminal.RED;
    public static final String GREEN = WASITerminal.GREEN;
    public static final String YELLOW = WASITerminal.YELLOW;
    public static final String BLUE = WASITerminal.BLUE;
    public static final String MAGENTA = WASITerminal.MAGENTA;
    public static final String CYAN = WASITerminal.CYAN;
    public static final String WHITE = WASITerminal.WHITE;

    public static final String BRIGHT_BLACK = WASITerminal.BRIGHT_BLACK;
    public static final String BRIGHT_RED = WASITerminal.BRIGHT_RED;
    public static final String BRIGHT_GREEN = WASITerminal.BRIGHT_GREEN;
    public static final String BRIGHT_YELLOW = WASITerminal.BRIGHT_YELLOW;
    public static final String BRIGHT_BLUE = WASITerminal.BRIGHT_BLUE;
    public static final String BRIGHT_MAGENTA = WASITerminal.BRIGHT_MAGENTA;
    public static final String BRIGHT_CYAN = WASITerminal.BRIGHT_CYAN;
    public static final String BRIGHT_WHITE = WASITerminal.BRIGHT_WHITE;

    public static final String BOLD = WASITerminal.BOLD;
    public static final String DIM = WASITerminal.DIM;
    public static final String ITALIC = WASITerminal.ITALIC;
    public static final String UNDERLINE = WASITerminal.UNDERLINE;
    public static final String REVERSE = WASITerminal.REVERSE;

    public static String readLine() {
        return WASITerminal.readLine();
    }

    public static String readLine(String prompt) {
        return WASITerminal.readLine(prompt);
    }

    public static int readChar() {
        return WASITerminal.readChar();
    }

    public static void print(String message) {
        WASITerminal.print(message);
    }

    public static void println(String message) {
        WASITerminal.println(message);
    }

    public static void print(int val) {
        WASITerminal.print(val);
    }

    public static void println(int val) {
        WASITerminal.println(val);
    }

    public static void print(double val) {
        WASITerminal.print(val);
    }

    public static void println(double val) {
        WASITerminal.println(val);
    }

    public static void println() {
        WASITerminal.println();
    }

    public static void clear() {
        WASITerminal.clear();
    }

    public static void setColor(String ansiCode) {
        WASITerminal.setColor(ansiCode);
    }

    public static void resetColor() {
        WASITerminal.resetColor();
    }

    public static void setCursor(int row, int col) {
        WASITerminal.setCursor(row, col);
    }

    public static int getColumns() {
        return WASITerminal.getColumns();
    }

    public static int getRows() {
        return WASITerminal.getRows();
    }
}
