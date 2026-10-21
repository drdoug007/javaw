package com.example.javaw.wasi;

/**
 * Strongly-typed Java facade for WebAssembly System Interface (WASI).
 */
public class WASI {

    public static void print(String message) {
        System.out.print(message);
    }

    public static void println(String message) {
        System.out.println(message);
    }

    public static void print(int value) {
        System.out.print(value);
    }

    public static void println(int value) {
        System.out.println(value);
    }

    public static void print(double value) {
        System.out.print(value);
    }

    public static void println(double value) {
        System.out.println(value);
    }

    public static void print(boolean value) {
        System.out.print(value);
    }

    public static void println(boolean value) {
        System.out.println(value);
    }

    public static void print(char value) {
        System.out.print(value);
    }

    public static void println(char value) {
        System.out.println(value);
    }

    public static void println() {
        System.out.println();
    }

    public static void eprint(String message) {
        System.err.print(message);
    }

    public static void eprintln(String message) {
        System.err.println(message);
    }

    public static void eprint(int value) {
        System.err.print(value);
    }

    public static void eprintln(int value) {
        System.err.println(value);
    }

    public static void exit(int code) {
        System.exit(code);
    }

    public static int fdWrite(int fd, String content) {
        return 0;
    }

    public static void procExit(int code) {
        System.exit(code);
    }
}
