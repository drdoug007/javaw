package com.example.javaw.lang;

import com.example.javaw.concurrent.WasmThread;

/**
 * Standard Thread facade wrapping WebAssembly concurrency primitives.
 */
public class Thread implements Runnable {

    private Runnable target;
    private String name;
    private int id;
    private boolean alive;

    public Thread() {
        this(null, "Thread");
    }

    public Thread(Runnable target) {
        this(target, "Thread");
    }

    public Thread(String name) {
        this(null, name);
    }

    public Thread(Runnable target, String name) {
        this.target = target;
        this.name = name;
        this.id = WasmThread.currentThreadId();
        this.alive = false;
    }

    public void start() {
        this.alive = true;
        if (target != null) {
            target.run();
        } else {
            run();
        }
    }

    @Override
    public void run() {
        if (target != null) {
            target.run();
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAlive() {
        return alive;
    }

    public static void sleep(long millis) {
        WasmThread.sleep((int) millis);
    }

    public static void sleep(int millis) {
        WasmThread.sleep(millis);
    }

    public static Thread currentThread() {
        Thread t = new Thread(null, "main");
        t.id = WasmThread.currentThreadId();
        t.alive = true;
        return t;
    }

    public static int activeCount() {
        return WasmThread.hardwareConcurrency();
    }

    public static void yield() {
    }

    public static void onSpinWait() {
    }
}
