package one.dastec.javaw.wasi;

/**
 * Strongly-typed Java facade for interactive WASI Terminal and REPL operations in WebAssembly.
 * Supports standard ANSI colors, terminal control, cursor manipulation, and interactive line-by-line reading.
 */
public class WASITerminal {

    // Standard ANSI Colors
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bright ANSI Colors
    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    // Styles & Attributes
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";
    public static final String REVERSE = "\u001B[7m";

    /**
     * Reads a line of text from standard input (WASI stdin).
     */
    public static native String readLine();

    /**
     * Prints a prompt and reads a line of text from standard input (WASI stdin).
     */
    public static native String readLine(String prompt);

    /**
     * Reads a single character (ASCII code) from standard input.
     */
    public static native int readChar();

    /**
     * Writes text to standard output (WASI stdout).
     */
    public static native void print(String message);

    /**
     * Writes text followed by a newline to standard output (WASI stdout).
     */
    public static native void println(String message);

    /**
     * Writes an integer to standard output.
     */
    public static native void print(int val);

    /**
     * Writes an integer followed by a newline to standard output.
     */
    public static native void println(int val);

    /**
     * Writes a double to standard output.
     */
    public static native void print(double val);

    /**
     * Writes a double followed by a newline to standard output.
     */
    public static native void println(double val);

    /**
     * Writes a newline to standard output.
     */
    public static native void println();

    /**
     * Clears the terminal screen and resets cursor position to home.
     */
    public static native void clear();

    /**
     * Sets the active ANSI color or styling escape sequence.
     */
    public static native void setColor(String ansiCode);

    /**
     * Resets the active ANSI color and styling to default.
     */
    public static native void resetColor();

    /**
     * Positions the cursor at the specified row and column coordinates (1-based).
     */
    public static native void setCursor(int row, int col);

    /**
     * Returns the terminal column count (width).
     */
    public static native int getColumns();

    /**
     * Returns the terminal row count (height).
     */
    public static native int getRows();
}
