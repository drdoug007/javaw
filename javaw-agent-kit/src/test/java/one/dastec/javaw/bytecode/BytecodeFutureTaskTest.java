package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T75: Callable, Future, RunnableFuture, FutureTask and the concurrent exceptions (single-threaded). Expected values come from the JVM; see tasks/T75-future-task.md. */
class BytecodeFutureTaskTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.Callable;
        import java.util.concurrent.CancellationException;
        import java.util.concurrent.ExecutionException;
        import java.util.concurrent.Future;
        import java.util.concurrent.FutureTask;
        import java.util.concurrent.RunnableFuture;
        import java.util.concurrent.TimeUnit;
        import java.util.concurrent.TimeoutException;
        class Bad extends Exception { Bad(String m){super(m);} }
        class Named implements Callable<String> {
            final String value; int calls;
            Named(String v){value=v;}
            public String call(){calls++;return value;}
            public String toString(){return "Named:"+value;}
        }
        class Failing implements Callable<String> {
            final Throwable t; int calls;
            Failing(Throwable t){this.t=t;}
            public String call() throws Exception{calls++;if(t instanceof Exception)throw (Exception)t;throw (Error)t;}
            public String toString(){return "Failing";}
        }
        class Counter implements Runnable {
            int runs; final RuntimeException fail;
            Counter(RuntimeException f){fail=f;}
            public void run(){runs++;if(fail!=null)throw fail;}
        }
        class Spy extends FutureTask<String> {
            final StringBuilder log=new StringBuilder();
            Spy(Callable<String> c){super(c);}
            Spy(Runnable r,String v){super(r,v);}
            protected void done(){log.append("done(").append(isDone()).append(isCancelled()).append(");");}
            protected void set(String v){log.append("set:").append(v).append(';');super.set(v);}
            protected void setException(Throwable t){log.append("setEx:").append(t).append(';');super.setException(t);}
            public boolean cancel(boolean b){log.append("cancel:").append(b).append(';');return super.cancel(b);}
            public boolean isDone(){log.append("isDone;");return super.isDone();}
            public String get() throws InterruptedException, ExecutionException{log.append("get;");return super.get();}
            void publish(String v){set(v);}
            void fail(Throwable t){setException(t);}
        }
        class Peek extends FutureTask<String> {
            final StringBuilder log=new StringBuilder();
            Peek(Callable<String> c){super(c);}
            protected void done(){
                try{log.append("get=").append(get());}
                catch(ExecutionException e){log.append("ee=").append(e.getCause());}
                catch(CancellationException e){log.append("cancelled");}
                catch(InterruptedException e){log.append("int");}
            }
        }
        class Throwing extends FutureTask<String> {
            Throwing(Callable<String> c){super(c);}
            protected void done(){throw new IllegalArgumentException("done failed");}
        }
        class MyEE extends ExecutionException {
            MyEE(){super();}
            MyEE(String m){super(m);}
        }
        class Fixed implements Future<String> {
            boolean cancelled;
            public boolean cancel(boolean b){cancelled=b;return b;}
            public boolean isCancelled(){return cancelled;}
            public boolean isDone(){return true;}
            public String get(){return "fixed";}
            public String get(long t,TimeUnit u) throws TimeoutException{if(t<0)throw new TimeoutException("neg");return "fixed"+t;}
        }
        public class Fixture {
            static String name(Throwable t){return t.getClass().getName();}
            static String tail(Object o){String s=String.valueOf(o);int i=s.indexOf('[');return i<0?s:s.substring(i);}
            static String get(Future<?> f){
                try{return String.valueOf(f.get());}
                catch(ExecutionException e){return "EE("+e.getMessage()+"|"+e.getCause()+")";}
                catch(CancellationException e){return "CE("+e.getMessage()+")";}
                catch(InterruptedException e){return "int";}
            }
            static String get(Future<?> f,long t,TimeUnit u){
                try{return String.valueOf(f.get(t,u));}
                catch(ExecutionException e){return "EE("+e.getMessage()+"|"+e.getCause()+")";}
                catch(CancellationException e){return "CE("+e.getMessage()+")";}
                catch(TimeoutException e){return "TE("+e.getMessage()+")";}
                catch(NullPointerException e){return "NPE";}
                catch(InterruptedException e){return "int";}
            }

            public static String callableTask(){
                StringBuilder out=new StringBuilder();
                Named c=new Named("a");
                FutureTask<String> f=new FutureTask<>(c);
                out.append(f.isDone()).append(f.isCancelled()).append(tail(f)).append(',');
                f.run();
                out.append(c.calls).append(f.isDone()).append(f.isCancelled()).append(',').append(get(f)).append(',');
                out.append(get(f,0,TimeUnit.SECONDS)).append(',').append(get(f,-1,TimeUnit.DAYS)).append(',').append(tail(f)).append(',');
                f.run();f.run();
                out.append(c.calls).append(',').append(get(f,5,null)).append(',');
                out.append(f.cancel(false)).append(f.cancel(true)).append(f.isCancelled()).append(get(f)).append(';');
                FutureTask<String> n=new FutureTask<>(new Named(null));
                n.run();
                out.append(get(n)).append(',').append(tail(n)).append(';');
                try{new FutureTask<String>((Callable<String>)null);out.append("no");}catch(NullPointerException e){out.append(name(e)).append(':').append(e.getMessage());}
                out.append(',');
                try{new FutureTask<String>((Runnable)null,"x");out.append("no");}catch(NullPointerException e){out.append(name(e));}
                return out.toString();
            }
            public static String runnableTask(){
                StringBuilder out=new StringBuilder();
                Counter r=new Counter(null);
                FutureTask<String> f=new FutureTask<>(r,"result");
                out.append(tail(f).startsWith("[Not completed, task = ")).append(',');
                f.run();f.run();
                out.append(r.runs).append(get(f)).append(',').append(tail(f)).append(',');
                FutureTask<Integer> v=new FutureTask<>(new Counter(null),null);
                v.run();
                out.append(get(v)).append(',');
                Counter bad=new Counter(new IllegalStateException("boom"));
                FutureTask<String> b=new FutureTask<>(bad,"never");
                b.run();b.run();
                out.append(bad.runs).append(b.isDone()).append(',').append(get(b)).append(',').append(tail(b)).append(';');
                RunnableFuture<String> rf=new FutureTask<>(new Named("rf"));
                Runnable asRunnable=rf;
                asRunnable.run();
                Future<String> fu=rf;
                out.append(fu.isDone()).append(fu.isCancelled()).append(get(fu)).append(get(fu,1,TimeUnit.NANOSECONDS)).append(',');
                Callable<Integer> lambda=()->6*7;
                FutureTask<Integer> lt=new FutureTask<>(lambda);
                lt.run();
                out.append(get(lt)).append(',');
                try{out.append(lambda.call());}catch(Exception e){out.append(name(e));}
                Future<String> fixed=new Fixed();
                out.append(',').append(get(fixed)).append(get(fixed,3,TimeUnit.SECONDS)).append(get(fixed,-3,TimeUnit.SECONDS));
                out.append(fixed.cancel(true)).append(fixed.isCancelled()).append(fixed.isDone());
                return out.toString();
            }
            public static String exceptionalTask(){
                StringBuilder out=new StringBuilder();
                Bad bad=new Bad("bad");
                Failing c=new Failing(bad);
                FutureTask<String> f=new FutureTask<>(c);
                f.run();f.run();
                out.append(c.calls).append(f.isDone()).append(f.isCancelled()).append(',').append(get(f)).append(',');
                out.append(get(f,0,TimeUnit.SECONDS)).append(',').append(tail(f)).append(',');
                try{f.get();}catch(ExecutionException e1){
                    try{f.get();}catch(ExecutionException e2){out.append(e1!=e2).append(e1.getCause()==bad).append(e2.getCause()==bad);}
                    catch(InterruptedException e){out.append("int");}
                }catch(InterruptedException e){out.append("int");}
                out.append(';');
                FutureTask<String> err=new FutureTask<>(new Failing(new AssertionError("assert")));
                err.run();
                out.append(get(err)).append(',').append(tail(err)).append(';');
                FutureTask<String> rt=new FutureTask<>(new Failing(new CancellationException("inner")));
                rt.run();
                out.append(rt.isCancelled()).append(',').append(get(rt)).append(';');
                FutureTask<String> te=new FutureTask<>(new Failing(new TimeoutException("late")));
                te.run();
                out.append(get(te,0,TimeUnit.SECONDS));
                return out.toString();
            }
            public static String exceptionTypes(){
                StringBuilder out=new StringBuilder();
                Throwable cause=new IllegalArgumentException("why");
                ExecutionException a=new ExecutionException(cause);
                out.append(a.getMessage()).append('|').append(a.getCause()==cause).append('|').append(a).append(',');
                ExecutionException b=new ExecutionException("msg",cause);
                out.append(b.getMessage()).append('|').append(b.getCause()==cause).append(',');
                ExecutionException c=new ExecutionException((Throwable)null);
                out.append(c.getMessage()).append('|').append(c.getCause()).append(',');
                ExecutionException d=new ExecutionException("only",null);
                out.append(d.getMessage()).append('|').append(d.getCause()).append(',');
                MyEE e=new MyEE();MyEE m=new MyEE("mine");
                out.append(e.getMessage()).append('|').append(e.getCause()).append('|').append(m.getMessage()).append(',');
                m.initCause(cause);
                out.append(m.getCause()==cause).append(',');
                Object ee=a;
                out.append(ee instanceof Exception).append(ee instanceof RuntimeException).append(';');
                CancellationException ce=new CancellationException(),cm=new CancellationException("stop");
                out.append(ce.getMessage()).append('|').append(cm.getMessage()).append('|').append(cm).append('|');
                Object co=ce;
                out.append(co instanceof IllegalStateException).append(co instanceof RuntimeException).append(';');
                TimeoutException t=new TimeoutException(),tm=new TimeoutException("slow");
                out.append(t.getMessage()).append('|').append(tm.getMessage()).append('|').append(tm).append('|');
                Throwable r=t.initCause(cause);
                out.append(r==t).append(t.getCause()==cause).append('|');
                Object to=t;
                out.append(to instanceof Exception).append(to instanceof RuntimeException);
                try{throw new ExecutionException("thrown",tm);}catch(ExecutionException x){out.append('|').append(x.getCause().getMessage());}
                return out.toString();
            }
            public static String cancellation(){
                StringBuilder out=new StringBuilder();
                Named c=new Named("c");
                Spy s=new Spy(c);
                out.append(s.cancel(false)).append(s.isCancelled()).append(s.isDone()).append(',').append(s.log).append(',');
                s.log.setLength(0);
                out.append(s.cancel(false)).append(s.cancel(true)).append(s.log).append(',');
                s.log.setLength(0);
                s.run();
                out.append(c.calls).append(s.log).append(',');
                s.log.setLength(0);
                out.append(get(s)).append(get(s,0,TimeUnit.SECONDS)).append(get(s,1,null)).append(',').append(tail(s)).append(s.log).append(';');
                Spy i=new Spy(new Named("i"));
                out.append(i.cancel(true)).append(i.isCancelled()).append(i.log).append(',').append(tail(i)).append(get(i)).append(';');
                final Spy[] self=new Spy[1];
                self[0]=new Spy(()->{boolean r=self[0].cancel(false);return "late"+r;});
                self[0].run();
                out.append(self[0].log).append(',').append(self[0].isCancelled()).append(get(self[0])).append(';');
                Spy d=new Spy(new Named("d"));
                d.run();d.log.setLength(0);
                out.append(d.cancel(false)).append(d.isCancelled()).append(d.log).append(get(d));
                return out.toString();
            }
            public static String notDone(){
                StringBuilder out=new StringBuilder();
                Named c=new Named("n");
                FutureTask<String> f=new FutureTask<>(c);
                out.append(get(f,0,TimeUnit.SECONDS)).append(get(f,-5,TimeUnit.DAYS)).append(get(f,1,TimeUnit.NANOSECONDS)).append(get(f,Long.MIN_VALUE,TimeUnit.MILLISECONDS));
                out.append(get(f,3,null)).append(',').append(f.isDone()).append(c.calls).append(tail(f)).append(',');
                f.run();
                out.append(get(f,0,TimeUnit.SECONDS)).append(';');
                final StringBuilder inner=new StringBuilder();
                final Spy[] self=new Spy[1];
                final int[] calls={0};
                self[0]=new Spy(()->{
                    calls[0]++;
                    inner.append(self[0].isCancelled()).append(get(self[0],0,TimeUnit.SECONDS));
                    self[0].run();
                    inner.append(calls[0]);
                    return "outer";
                });
                self[0].run();
                out.append(inner).append(',').append(calls[0]).append(self[0].log).append(get(self[0]));
                return out.toString();
            }
            public static String hooks(){
                StringBuilder out=new StringBuilder();
                Spy a=new Spy(new Named("a"));
                a.run();
                out.append(a.log).append(',');
                a.log.setLength(0);
                a.publish("again");a.fail(new Bad("late"));a.run();
                out.append(a.log).append(get(a)).append(';');
                Named never=new Named("never");
                Spy p=new Spy(never);
                p.publish("early");
                out.append(p.log).append(',');
                p.log.setLength(0);
                p.run();
                out.append(never.calls).append(p.log).append(get(p)).append(',').append(tail(p)).append(';');
                Spy x=new Spy(new Named("x"));
                x.fail(new Bad("direct"));
                out.append(x.log).append(get(x)).append(',').append(tail(x)).append(',');
                Spy z=new Spy(new Named("z"));
                z.fail(null);
                out.append(z.log).append(get(z)).append(',').append(tail(z)).append(';');
                Spy e=new Spy(new Failing(new Bad("e")));
                e.run();
                out.append(e.log).append(',');
                Spy r=new Spy(new Counter(null),"rv");
                r.run();
                out.append(r.log).append(';');
                Peek pk=new Peek(new Named("pk"));pk.run();
                Peek pe=new Peek(new Failing(new Bad("pe")));pe.run();
                Peek pc=new Peek(new Named("pc"));pc.cancel(false);
                out.append(pk.log).append(',').append(pe.log).append(',').append(pc.log).append(';');
                Throwing t=new Throwing(new Named("t"));
                try{t.run();out.append("ran");}catch(IllegalArgumentException ex){out.append(ex.getMessage());}
                out.append(',').append(t.isDone()).append(get(t)).append(tail(t)).append(',');
                Throwing tc=new Throwing(new Named("tc"));
                try{out.append(tc.cancel(false));}catch(IllegalArgumentException ex){out.append(ex.getMessage());}
                out.append(tc.isCancelled()).append(tail(tc));
                return out.toString();
            }
        }
        """;

    @Test void completesWithResult() throws Exception {
        matchesJvm(FIXTURE, "callableTask", "runnableTask");
    }

    @Test void completesExceptionally() throws Exception {
        matchesJvm(FIXTURE, "exceptionalTask", "exceptionTypes");
    }

    @Test void cancelsAndTimesOut() throws Exception {
        matchesJvm(FIXTURE, "cancellation", "notDone");
    }

    @Test void callsProtectedHooks() throws Exception {
        matchesJvm(FIXTURE, "hooks");
    }

    @Test void leavesOutExecutorsAndJdk19Members() {
        rejectsUnusedCall("java/util/concurrent/FutureTask", "runAndReset", "()Z", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/FutureTask", "resultNow", "()Ljava/lang/Object;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/Future", "exceptionNow", "()Ljava/lang/Throwable;", INVOKEINTERFACE, true);
        rejectsUnusedCall("java/util/concurrent/Executors", "callable", "(Ljava/lang/Runnable;Ljava/lang/Object;)Ljava/util/concurrent/Callable;", INVOKESTATIC, false);
    }
}
