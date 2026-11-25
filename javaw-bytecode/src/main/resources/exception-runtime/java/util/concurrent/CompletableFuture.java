package java.util.concurrent;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Single-threaded core of CompletableFuture: no executor, no blocking, no
 * time. A future holds one encoded result (null = pending, NIL = the null
 * value, an AltResult = an exception, anything else = a value) and a stack
 * of dependent actions, newest first. Completing a future runs every
 * dependent in the JDK's postComplete order. get()/join() on an incomplete
 * future would block forever on the JVM, so they throw IllegalStateException;
 * get(timeout, unit) throws TimeoutException at once.
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class CompletableFuture<T> implements Future<T>, CompletionStage<T> {

    /** Marks an exceptional result; ex is null only for NIL. */
    static final class AltResult {
        final Throwable ex;
        AltResult(Throwable ex) { this.ex = ex; }
    }

    /** The encoding of a completed null value. */
    static final AltResult NIL = new AltResult(null);

    private static final int APPLY = 0, ACCEPT = 1, RUN = 2, COMPOSE = 3,
        HANDLE = 4, WHEN = 5, EXCEPTIONALLY = 6, RELAY = 7;

    static final class Completion {
        final int kind;
        final CompletableFuture src, dep;
        final Object fn;
        Completion next;

        Completion(int kind, CompletableFuture src, CompletableFuture dep, Object fn) {
            this.kind = kind;
            this.src = src;
            this.dep = dep;
            this.fn = fn;
        }

        CompletableFuture fire() {
            CompletableFuture d = dep;
            run(kind, fn, src.result, d);
            return (d.result != null && d.stack != null) ? d : null;
        }
    }

    Object result;
    Completion stack;

    public CompletableFuture() {
    }

    CompletableFuture(Object r) {
        result = r;
    }

    public static <U> CompletableFuture<U> completedFuture(U value) {
        return new CompletableFuture<U>(encodeValue(value));
    }

    public static <U> CompletableFuture<U> failedFuture(Throwable ex) {
        if (ex == null)
            throw new NullPointerException();
        return new CompletableFuture<U>(new AltResult(ex));
    }

    public boolean complete(T value) {
        boolean c = (result == null) && set(encodeValue(value));
        postComplete();
        return c;
    }

    public boolean completeExceptionally(Throwable ex) {
        if (ex == null)
            throw new NullPointerException();
        boolean c = (result == null) && set(new AltResult(ex));
        postComplete();
        return c;
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean c = (result == null) && set(new AltResult(new CancellationException()));
        postComplete();
        return c || isCancelled();
    }

    public boolean isDone() {
        return result != null;
    }

    public boolean isCancelled() {
        return result instanceof AltResult && ((AltResult) result).ex instanceof CancellationException;
    }

    public boolean isCompletedExceptionally() {
        return result instanceof AltResult && result != NIL;
    }

    public T get() throws InterruptedException, ExecutionException {
        Object r = result;
        if (r == null)
            throw new IllegalStateException("CompletableFuture.get would block forever: not completed and no other thread");
        return reportGet(r);
    }

    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        unit.toNanos(timeout);
        Object r = result;
        if (r == null)
            throw new TimeoutException();
        return reportGet(r);
    }

    public T join() {
        Object r = result;
        if (r == null)
            throw new IllegalStateException("CompletableFuture.join would block forever: not completed and no other thread");
        return reportJoin(r);
    }

    public T getNow(T valueIfAbsent) {
        Object r = result;
        return (r == null) ? valueIfAbsent : reportJoin(r);
    }

    public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn) {
        if (fn == null)
            throw new NullPointerException();
        return stage(APPLY, fn);
    }

    public CompletableFuture<Void> thenAccept(Consumer<? super T> action) {
        if (action == null)
            throw new NullPointerException();
        return stage(ACCEPT, action);
    }

    public CompletableFuture<Void> thenRun(Runnable action) {
        if (action == null)
            throw new NullPointerException();
        return stage(RUN, action);
    }

    public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn) {
        if (fn == null)
            throw new NullPointerException();
        return stage(COMPOSE, fn);
    }

    public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn) {
        if (fn == null)
            throw new NullPointerException();
        return stage(HANDLE, fn);
    }

    public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action) {
        if (action == null)
            throw new NullPointerException();
        return stage(WHEN, action);
    }

    public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn) {
        if (fn == null)
            throw new NullPointerException();
        return stage(EXCEPTIONALLY, fn);
    }

    public CompletableFuture<T> toCompletableFuture() {
        return this;
    }

    /* ------------- encoding and plumbing -------------- */

    private static Object encodeValue(Object v) {
        return (v == null) ? NIL : v;
    }

    /** "A stage function threw x": wrap unless x already is a CompletionException. */
    private static AltResult wrap(Throwable x) {
        return new AltResult((x instanceof CompletionException) ? x : new CompletionException(x));
    }

    /** "Source r failed with x": propagate x, reusing r when it already holds the same x. */
    private static AltResult wrap(Throwable x, Object r) {
        if (!(x instanceof CompletionException))
            return new AltResult(new CompletionException(x));
        if (r instanceof AltResult && x == ((AltResult) r).ex)
            return (AltResult) r;
        return new AltResult(x);
    }

    /** Stores r only when not yet completed. */
    private boolean set(Object r) {
        if (result == null) {
            result = r;
            return true;
        }
        return false;
    }

    private void push(Completion c) {
        c.next = stack;
        stack = c;
    }

    private final void postComplete() {
        CompletableFuture f = this;
        Completion h;
        while ((h = f.stack) != null || (f != this && (h = (f = this).stack) != null)) {
            Completion t = h.next;
            f.stack = t;
            if (t != null && f != this) { // move to this.stack
                h.next = stack;
                stack = h;
                continue;
            }
            h.next = null;
            CompletableFuture d = h.fire();
            f = d == null ? this : d;
        }
    }

    /**
     * Computes dependent d from source result r, for both the "source
     * already completed" path and Completion.fire(). Skips a dependent
     * completed by hand. x is the raw stored exception, v the value.
     */
    private static void run(int kind, Object fn, Object r, CompletableFuture d) {
        if (d.result != null)
            return;
        boolean failed = (r instanceof AltResult) && (r != NIL);
        Throwable x = failed ? ((AltResult) r).ex : null;
        Object v = failed ? null : (r == NIL ? null : r);
        if (kind == APPLY) {
            if (!failed)
                try {
                    d.set(encodeValue(((Function) fn).apply(v)));
                } catch (Throwable ex) {
                    d.set(wrap(ex));
                }
            else
                d.set(wrap(x, r));
        } else if (kind == ACCEPT || kind == RUN) {
            if (!failed)
                try {
                    if (kind == ACCEPT)
                        ((Consumer) fn).accept(v);
                    else
                        ((Runnable) fn).run();
                    d.set(NIL);
                } catch (Throwable ex) {
                    d.set(wrap(ex));
                }
            else
                d.set(wrap(x, r));
        } else if (kind == COMPOSE) {
            if (!failed)
                try {
                    Object s0 = ((Function) fn).apply(v);
                    CompletableFuture g = ((CompletionStage) s0).toCompletableFuture();
                    Object s = g.result;
                    if (s == null)
                        g.push(new Completion(RELAY, g, d, null));
                    else
                        d.set((s instanceof AltResult && s != NIL) ? wrap(((AltResult) s).ex, s) : s);
                } catch (Throwable ex) {
                    d.set(wrap(ex));
                }
            else
                d.set(wrap(x, r));
        } else if (kind == HANDLE) {
            try {
                d.set(encodeValue(((BiFunction) fn).apply(failed ? null : v, failed ? x : null)));
            } catch (Throwable ex) {
                d.set(wrap(ex));
            }
        } else if (kind == WHEN) {
            if (!failed)
                try {
                    ((BiConsumer) fn).accept(v, null);
                    d.set(r);
                } catch (Throwable ex) {
                    d.set(wrap(ex));
                }
            else
                try {
                    ((BiConsumer) fn).accept(null, x);
                    d.set(wrap(x, r));
                } catch (Throwable ex) {
                    if (ex != x)
                        x.addSuppressed(ex);
                    d.set(wrap(x, r));
                }
        } else if (kind == EXCEPTIONALLY) {
            if (!failed)
                d.set(r);
            else
                try {
                    d.set(encodeValue(((Function) fn).apply(x)));
                } catch (Throwable ex) {
                    d.set(wrap(ex));
                }
        } else { // RELAY
            if (r instanceof AltResult && r != NIL)
                d.set(wrap(((AltResult) r).ex, r));
            else
                d.set(r);
        }
    }

    private <U> CompletableFuture<U> stage(int kind, Object fn) {
        CompletableFuture<U> d = new CompletableFuture<U>();
        Object r = result;
        if (r == null)
            push(new Completion(kind, this, d, fn));
        else {
            run(kind, fn, r, d);
            d.postComplete();
        }
        return d;
    }

    private T reportGet(Object r) throws ExecutionException {
        if (r instanceof AltResult) {
            Throwable x = ((AltResult) r).ex;
            if (x == null)
                return null;
            if (x instanceof CancellationException)
                throw (CancellationException) x;
            if (x instanceof CompletionException && x.getCause() != null)
                x = x.getCause();
            throw new ExecutionException(x);
        }
        return (T) r;
    }

    private T reportJoin(Object r) {
        if (r instanceof AltResult) {
            Throwable x = ((AltResult) r).ex;
            if (x == null)
                return null;
            if (x instanceof CancellationException)
                throw (CancellationException) x;
            if (x instanceof CompletionException)
                throw (CompletionException) x;
            throw new CompletionException(x);
        }
        return (T) r;
    }
}
