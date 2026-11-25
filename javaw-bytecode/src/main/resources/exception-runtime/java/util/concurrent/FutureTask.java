package java.util.concurrent;

/** Single-threaded FutureTask: no waiters, no runner thread, no interrupts; get on a task
 *  that is not done throws instead of blocking. */
public class FutureTask<V> implements RunnableFuture<V> {
    private static final int NEW = 0;
    private static final int NORMAL = 1;
    private static final int EXCEPTIONAL = 2;
    private static final int CANCELLED = 3;
    private static final int INTERRUPTED = 4;

    private int state;
    private Callable<V> callable;
    private Object outcome;
    private boolean running;

    private static class Adapter<T> implements Callable<T> {
        private final Runnable runnable;
        private final T result;
        Adapter(Runnable runnable, T result) { this.runnable = runnable; this.result = result; }
        public T call() { runnable.run(); return result; }
    }

    public FutureTask(Callable<V> callable) {
        if (callable == null) throw new NullPointerException();
        this.callable = callable; state = NEW;
    }

    public FutureTask(Runnable runnable, V result) {
        if (runnable == null) throw new NullPointerException();
        this.callable = new Adapter<V>(runnable, result); state = NEW;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        if (state != NEW) return false;
        state = mayInterruptIfRunning ? INTERRUPTED : CANCELLED;
        finishCompletion();
        return true;
    }

    public boolean isCancelled() { return state >= CANCELLED; }

    public boolean isDone() { return state != NEW; }

    public V get() throws InterruptedException, ExecutionException {
        int s = state;
        if (s == NEW) throw new IllegalStateException("FutureTask.get would block forever: task not completed and no other thread");
        return report(s);
    }

    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        if (unit == null) throw new NullPointerException();
        int s = state;
        if (s == NEW) {
            unit.toNanos(timeout);
            throw new TimeoutException();
        }
        return report(s);
    }

    protected void done() {}

    protected void set(V v) {
        if (state != NEW) return;
        outcome = v; state = NORMAL;
        finishCompletion();
    }

    protected void setException(Throwable t) {
        if (state != NEW) return;
        outcome = t; state = EXCEPTIONAL;
        finishCompletion();
    }

    private void finishCompletion() {
        done();
        callable = null;
    }

    public void run() {
        if (state != NEW || running) return;
        running = true;
        try {
            Callable<V> c = callable;
            if (c != null && state == NEW) {
                V result;
                boolean ran;
                try {
                    result = c.call();
                    ran = true;
                } catch (Throwable ex) {
                    result = null;
                    ran = false;
                    setException(ex);
                }
                if (ran) set(result);
            }
        } finally {
            running = false;
        }
    }

    private V report(int s) throws InterruptedException, ExecutionException {
        if (s == NORMAL) return (V) outcome;
        if (s >= CANCELLED) throw new CancellationException();
        throw new ExecutionException((Throwable) outcome);
    }

    public String toString() {
        int s = state;
        String t;
        if (s == NORMAL) t = "[Completed normally]";
        else if (s == EXCEPTIONAL) t = "[Completed exceptionally: " + outcome + "]";
        else if (s >= CANCELLED) t = "[Cancelled]";
        else if (callable != null) t = "[Not completed, task = " + callable + "]";
        else t = "[Not completed]";
        return super.toString() + t;
    }
}
