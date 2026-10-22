package com.example.javaw.examples.threads;

import com.example.javaw.concurrent.AtomicInteger;
import com.example.javaw.concurrent.Atomics;
import com.example.javaw.concurrent.WasmThread;

public class WorkerAtomicsDemo {

    public static AtomicInteger globalCounter = new AtomicInteger(0);
    public static int[] sharedArray = new int[16];

    public static int incrementShared() {
        return globalCounter.incrementAndGet();
    }

    public static int getCounterValue() {
        return globalCounter.get();
    }

    public static boolean compareAndSwap(int expected, int newValue) {
        return globalCounter.compareAndSet(expected, newValue);
    }

    public static int atomicArrayAdd(int index, int delta) {
        return Atomics.add(sharedArray, index, delta);
    }

    public static int readSharedArray(int index) {
        return Atomics.load(sharedArray, index);
    }

    public static int getAvailableCores() {
        return WasmThread.hardwareConcurrency();
    }
}
