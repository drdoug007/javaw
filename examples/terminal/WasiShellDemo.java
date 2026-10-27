package one.dastec.javaw.examples.terminal;

import one.dastec.javaw.terminal.Terminal;
import one.dastec.javaw.wasi.WASIFile;

public class WasiShellDemo {

    private static boolean initialized = false;

    public static void main(String[] args) {
        runInteractiveShell();
    }

    public static void runInteractiveShell() {
        if (!initialized) {
            initialized = true;
            Terminal.clear();
            Terminal.println(Terminal.BRIGHT_CYAN + Terminal.BOLD + "=============================================" + Terminal.RESET);
            Terminal.println(Terminal.BRIGHT_CYAN + Terminal.BOLD + "   Javaw Interactive WASI Browser Terminal   " + Terminal.RESET);
            Terminal.println(Terminal.BRIGHT_CYAN + Terminal.BOLD + "=============================================" + Terminal.RESET);
            Terminal.println(Terminal.DIM + "Type " + Terminal.BRIGHT_YELLOW + "help" + Terminal.DIM + " for available shell commands." + Terminal.RESET);
            Terminal.println("");

            // Initialize virtual file system with a welcome file
            WASIFile welcome = new WASIFile("welcome.txt");
            if (!welcome.exists()) {
                welcome.writeString("Welcome to the WebAssembly Virtual File System in Java!");
            }
        }

        while (true) {
            String input = Terminal.readLine(Terminal.BRIGHT_GREEN + "wasi-sh> " + Terminal.RESET);
            if (input == null) break;
            executeCommand(input);
        }
    }

    public static void executeCommand(String input) {
        if (input == null || input.isEmpty()) return;

        if (input.equals("exit") || input.equals("quit")) {
            Terminal.println(Terminal.YELLOW + "Shell terminated. Goodbye!" + Terminal.RESET);
        } else if (input.equals("help")) {
            printHelp();
        } else if (input.equals("clear") || input.equals("cls")) {
            Terminal.clear();
        } else if (input.equals("info")) {
            Terminal.println(Terminal.BRIGHT_BLUE + "Terminal Dimensions: " + Terminal.RESET +
                    Terminal.getColumns() + " columns x " + Terminal.getRows() + " rows");
        } else if (input.equals("cat") || input.equals("cat welcome.txt")) {
            WASIFile f = new WASIFile("welcome.txt");
            if (f.exists()) {
                Terminal.println(Terminal.BRIGHT_WHITE + f.readString() + Terminal.RESET);
            } else {
                Terminal.println(Terminal.RED + "Error: File not found." + Terminal.RESET);
            }
        } else if (input.startsWith("echo ")) {
            String msg = input.substring(5, input.length());
            Terminal.println(msg);
        } else if (input.equals("color") || input.equals("colors")) {
            printColorPalette();
        } else if (!input.isEmpty()) {
            Terminal.println(Terminal.RED + "Command not recognized: '" + input + "'. Type 'help' for command list." + Terminal.RESET);
        }
    }

    public static void printHelp() {
        Terminal.println(Terminal.BOLD + "Available Built-in Commands:" + Terminal.RESET);
        Terminal.println("  " + Terminal.CYAN + "help" + Terminal.RESET + "          - Display this help message");
        Terminal.println("  " + Terminal.CYAN + "clear" + Terminal.RESET + "         - Clear terminal screen");
        Terminal.println("  " + Terminal.CYAN + "info" + Terminal.RESET + "          - Show terminal size and configuration");
        Terminal.println("  " + Terminal.CYAN + "cat <file>" + Terminal.RESET + "    - Display contents of virtual file");
        Terminal.println("  " + Terminal.CYAN + "echo <msg>" + Terminal.RESET + "    - Print text to standard output");
        Terminal.println("  " + Terminal.CYAN + "colors" + Terminal.RESET + "        - Display ANSI 16-color test palette");
        Terminal.println("  " + Terminal.CYAN + "exit" + Terminal.RESET + "          - Terminate the shell session");
    }

    public static void printColorPalette() {
        Terminal.println(Terminal.BOLD + "ANSI Color Test Palette:" + Terminal.RESET);
        Terminal.println(Terminal.RED + "■ Red   " +
                Terminal.GREEN + "■ Green   " +
                Terminal.YELLOW + "■ Yellow   " +
                Terminal.BLUE + "■ Blue   " +
                Terminal.MAGENTA + "■ Magenta   " +
                Terminal.CYAN + "■ Cyan" + Terminal.RESET);
        Terminal.println(Terminal.BRIGHT_RED + "■ B.Red " +
                Terminal.BRIGHT_GREEN + "■ B.Green " +
                Terminal.BRIGHT_YELLOW + "■ B.Yellow " +
                Terminal.BRIGHT_BLUE + "■ B.Blue " +
                Terminal.BRIGHT_MAGENTA + "■ B.Mag   " +
                Terminal.BRIGHT_CYAN + "■ B.Cyan" + Terminal.RESET);
    }
}
