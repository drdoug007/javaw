package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T76: java.util.concurrent.CompletableFuture single-threaded core. Expected values come from the JVM; see tasks/T76-completable-future.md. */
class BytecodeCompletableFutureTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.util.concurrent.CancellationException;
        import java.util.concurrent.CompletableFuture;
        import java.util.concurrent.CompletionException;
        import java.util.concurrent.CompletionStage;
        import java.util.concurrent.ExecutionException;
        import java.util.concurrent.Future;
        import java.util.concurrent.TimeUnit;
        import java.util.concurrent.TimeoutException;

        /** Uses the protected CompletionException constructors. */
        class MyCompletion extends CompletionException {
            MyCompletion(){super();}
            MyCompletion(String m){super(m);}
        }

        public class Fixture {
            interface Op{Object run() throws Exception;}
            /** Type, message and cause chain. NullPointerException messages (and messages quoting one) are left out: the JVM's helpful messages differ. */
            static String d(Throwable x){
                if(x==null)return "-";
                String s=x.getClass().getSimpleName();
                if(x.getMessage()!=null&&!x.getMessage().contains("NullPointerException")&&!(x instanceof NullPointerException))s+=":"+x.getMessage();
                if(x.getCause()!=null&&x.getCause()!=x)s+="<"+d(x.getCause())+">";
                return s;
            }
            static String t(Op op){
                try{return String.valueOf(op.run());}catch(Throwable e){return "!"+d(e);}
            }
            /** State of a future; get() and join() are only called when it is done (they would block on the JVM). */
            @SuppressWarnings("unchecked") static String st(CompletableFuture<?> c){
                CompletableFuture<Object> f=(CompletableFuture<Object>)c;
                String s=f.isDone()+"/"+f.isCompletedExceptionally()+"/"+f.isCancelled()+"/"+t(()->f.getNow("absent"));
                if(!f.isDone())return s+"/"+t(()->f.get(0,TimeUnit.MILLISECONDS));
                return s+"/"+t(()->f.get())+"/"+t(()->f.join())+"/"+t(()->f.get(5,TimeUnit.SECONDS));
            }
            /** The raw stored outcome as seen by handle: value or exception. */
            static Throwable raw(CompletableFuture<?> f){
                Throwable[] out=new Throwable[1];
                f.handle((v,x)->{out[0]=x;return null;});
                return out[0];
            }

            public static String states(){
                CompletableFuture<String> f=new CompletableFuture<>();
                String a=st(f)+"|"+f.complete("a")+","+f.complete("b")+","+f.completeExceptionally(new IllegalStateException("late"))+","+f.cancel(true)+","+st(f);
                CompletableFuture<String> g=new CompletableFuture<>();
                IllegalStateException boom=new IllegalStateException("boom");
                a+="|"+g.completeExceptionally(boom)+","+g.complete("x")+","+g.cancel(false)+","+st(g)+","+(raw(g)==boom);
                CompletableFuture<Object> h=CompletableFuture.completedFuture(null);
                a+="|"+st(h)+","+st(CompletableFuture.completedFuture(7));
                CompletableFuture<String> n=new CompletableFuture<>();
                a+="|"+n.complete(null)+","+st(n)+","+n.complete("y");
                a+="|"+t(()->new CompletableFuture<String>().completeExceptionally(null))+","+t(()->CompletableFuture.failedFuture(null));
                CompletableFuture<String> ff=CompletableFuture.failedFuture(new IllegalArgumentException("bad"));
                a+="|"+st(ff)+","+ff.complete("z");
                return a;
            }
            public static String wrapping(){
                IllegalStateException cause=new IllegalStateException("root");
                CompletionException ce=new CompletionException(cause);
                CompletableFuture<String> f=new CompletableFuture<>();f.completeExceptionally(ce);
                String a=st(f)+","+(raw(f)==ce);
                try{f.join();}catch(CompletionException e){a+=","+(e==ce);}
                try{f.get();}catch(Exception e){a+=","+(e.getCause()==cause);}
                CompletionException bare=new CompletionException("bare",null);
                CompletableFuture<String> b=new CompletableFuture<>();b.completeExceptionally(bare);
                a+="|"+st(b);
                try{b.get();}catch(Exception e){a+=","+(e.getCause()==bare);}
                IllegalArgumentException plain=new IllegalArgumentException("plain");
                CompletableFuture<String> p=new CompletableFuture<>();p.completeExceptionally(plain);
                try{p.join();}catch(CompletionException e){a+="|"+(e.getCause()==plain);}
                try{p.join();}catch(CompletionException e){a+=","+d(e);}
                a+="|"+d(new CompletionException(cause))+","+d(new CompletionException("m",cause))+","+d(new CompletionException((Throwable)null))+","+d(new MyCompletion())+","+d(new MyCompletion("mine"));
                a+="|"+d(new ExecutionException(cause))+","+((Object)ce instanceof RuntimeException)+","+((Object)new CancellationException() instanceof IllegalStateException);
                return a;
            }
            public static String cancellation(){
                CompletableFuture<String> c=new CompletableFuture<>();
                String a=c.cancel(false)+","+c.cancel(true)+","+c.complete("x")+","+c.completeExceptionally(new IllegalStateException())+","+st(c);
                CompletableFuture<String> m=new CompletableFuture<>();
                m.completeExceptionally(new CancellationException("mine"));
                a+="|"+st(m)+","+m.cancel(true);
                CompletableFuture<String> done=CompletableFuture.completedFuture("v");
                a+="|"+done.cancel(true)+","+st(done);
                CompletableFuture<String> failed=CompletableFuture.failedFuture(new IllegalStateException("f"));
                a+="|"+failed.cancel(true)+","+failed.isCancelled();
                CompletableFuture<String> dep=c.thenApply(s->s+"!");
                a+="|"+st(dep)+","+d(raw(dep))+","+(raw(dep).getCause()==raw(c));
                CompletableFuture<String> src=new CompletableFuture<>();
                CompletableFuture<String> later=src.thenApply(s->s+"!");
                a+="|"+src.cancel(true)+","+st(later)+","+d(raw(later));
                return a;
            }
            public static String timedGet(){
                CompletableFuture<String> p=new CompletableFuture<>();
                String a=t(()->p.get(0,TimeUnit.MILLISECONDS))+","+t(()->p.get(-5,TimeUnit.SECONDS))+","+t(()->p.get(1,TimeUnit.NANOSECONDS))+","+t(()->p.get(1,null));
                CompletableFuture<String> v=CompletableFuture.completedFuture("v");
                a+="|"+t(()->v.get(0,TimeUnit.MILLISECONDS))+","+t(()->v.get(-1,TimeUnit.DAYS))+","+t(()->v.get(1,null));
                CompletableFuture<String> f=CompletableFuture.failedFuture(new IllegalStateException("x"));
                a+="|"+t(()->f.get(0,TimeUnit.MILLISECONDS));
                Future<String> fut=v;
                a+="|"+t(()->fut.get())+","+t(()->fut.get(1,TimeUnit.SECONDS))+","+fut.isDone()+","+fut.isCancelled()+","+fut.cancel(true);
                return a;
            }
            public static String dependentsOfDone(){
                StringBuilder log=new StringBuilder();
                CompletableFuture<Integer> src=CompletableFuture.completedFuture(2);
                String a=st(src.thenApply(x->x*10))+","+st(src.thenAccept(x->log.append("acc"+x)))+","+st(src.thenRun(()->log.append(",run")))+","+log;
                RuntimeException r=new RuntimeException("f");
                CompletableFuture<Integer> thrown=src.thenApply(x->{throw r;});
                a+="|"+st(thrown)+","+(raw(thrown).getCause()==r);
                CompletionException ce=new CompletionException(r);
                CompletableFuture<Integer> thrownCe=src.thenApply(x->{throw ce;});
                a+="|"+st(thrownCe)+","+(raw(thrownCe)==ce);
                a+="|"+st(src.exceptionally(x->-1))+","+st(src.handle((v,x)->v+":"+x))+","+st(src.whenComplete((v,x)->log.append(",wc"+v+x)))+","+log;
                CompletableFuture<Integer> wcThrow=src.whenComplete((v,x)->{throw r;});
                a+="|"+st(wcThrow)+","+(raw(wcThrow).getCause()==r)+","+st(src.handle((v,x)->{throw r;}));
                CompletableFuture<Object> nil=CompletableFuture.completedFuture(null);
                a+="|"+st(nil.thenApply(x->"was "+x))+","+st(nil.exceptionally(x->"no"))+","+st(nil.whenComplete((v,x)->{}));
                a+="|"+t(()->src.thenApply(null))+","+t(()->src.thenAccept(null))+","+t(()->src.thenRun(null))+","+t(()->src.exceptionally(null))+","+t(()->src.handle(null))+","+t(()->src.whenComplete(null))+","+t(()->src.thenCompose(null));
                CompletableFuture<String> pend=new CompletableFuture<>();
                a+="|"+t(()->pend.thenApply(null))+","+t(()->pend.whenComplete(null))+","+t(()->pend.thenCompose(null));
                return a;
            }
            public static String exceptionalSources(){
                StringBuilder log=new StringBuilder();
                IllegalStateException e=new IllegalStateException("e");
                CompletableFuture<String> src=CompletableFuture.failedFuture(e);
                CompletableFuture<String> d1=src.thenApply(s->{log.append("never");return s;});
                Throwable x1=raw(d1);
                CompletableFuture<String> d2=d1.thenApply(s->s);
                String a=st(d1)+","+d(x1)+","+(x1.getCause()==e)+","+(raw(d2)==x1)+","+st(src.thenAccept(s->log.append("never")))+","+st(src.thenRun(()->log.append("never")))+","+log;
                a+="|"+st(src.exceptionally(x->"rec "+d(x)))+","+st(d1.exceptionally(x->"rec "+d(x)))+","+st(src.exceptionally(x->{throw new IllegalArgumentException("again");}));
                a+="|"+st(src.handle((v,x)->v+" "+d(x)))+","+st(d1.handle((v,x)->v+" "+d(x)));
                CompletableFuture<String> wc=src.whenComplete((v,x)->log.append("wc "+v+" "+d(x)));
                a+="|"+log+","+st(wc)+","+(raw(wc)==e)+","+(raw(wc).getCause()==e);log.setLength(0);
                CompletableFuture<String> wc2=d1.whenComplete((v,x)->{});
                a+=","+(raw(wc2)==x1);
                IllegalStateException e2=new IllegalStateException("e2");
                CompletableFuture<String> s2=CompletableFuture.failedFuture(e2);
                CompletableFuture<String> wcThrow=s2.whenComplete((v,x)->{throw new IllegalArgumentException("side");});
                a+="|"+st(wcThrow)+","+e2.getSuppressed().length+","+d(e2.getSuppressed()[0]);
                IllegalStateException e3=new IllegalStateException("e3");
                CompletableFuture<String> s3=CompletableFuture.failedFuture(e3);
                CompletableFuture<String> wcSame=s3.whenComplete((v,x)->{throw e3;});
                a+=","+st(wcSame)+","+e3.getSuppressed().length;
                CompletionException ce=new CompletionException(e);
                CompletableFuture<String> s4=CompletableFuture.failedFuture(ce);
                a+="|"+(raw(s4.thenApply(s->s))==ce)+","+(raw(s4.whenComplete((v,x)->{}))==ce)+","+(raw(s4.thenCompose(s->null))==ce)+","+st(s4.thenRun(()->{}));
                return a;
            }
            public static String compose(){
                CompletableFuture<Integer> one=CompletableFuture.completedFuture(1);
                String a=st(one.thenCompose(x->CompletableFuture.completedFuture(x+1)))+","+st(one.thenCompose(x->null));
                IllegalStateException e=new IllegalStateException("inner");
                CompletableFuture<Integer> innerFailed=one.thenCompose(x->CompletableFuture.failedFuture(e));
                a+="|"+st(innerFailed)+","+(raw(innerFailed).getCause()==e);
                CompletionException ce=new CompletionException(e);
                CompletableFuture<Integer> innerCe=one.thenCompose(x->CompletableFuture.failedFuture(ce));
                a+=","+(raw(innerCe)==ce);
                a+=","+st(one.thenCompose(x->{throw new IllegalArgumentException("fn");}));
                CompletableFuture<String> g=new CompletableFuture<>();
                CompletableFuture<String> d=one.thenCompose(x->g);
                a+="|"+st(d)+","+(d==g)+","+g.complete("late")+","+st(d);
                CompletableFuture<String> src=new CompletableFuture<>();
                CompletableFuture<String> inner=new CompletableFuture<>();
                CompletableFuture<String> chained=src.thenCompose(s->inner.thenApply(v->s+v));
                CompletableFuture<String> after=chained.thenApply(v->v+"!");
                a+="|"+st(chained)+","+src.complete("s")+","+st(chained)+","+inner.complete("i")+","+st(chained)+","+st(after);
                CompletableFuture<String> failing=new CompletableFuture<>();
                CompletableFuture<String> fc=one.thenCompose(x->failing);
                failing.completeExceptionally(e);
                a+="|"+st(fc)+","+(raw(fc).getCause()==e);
                CompletionStage<String> stage=CompletableFuture.completedFuture("st");
                CompletionStage<String> composed=stage.thenCompose(s->CompletableFuture.completedFuture(s+"2"));
                a+="|"+st(composed.toCompletableFuture());
                return a;
            }
            public static String pendingOrder(){
                StringBuilder log=new StringBuilder();
                CompletableFuture<String> src=new CompletableFuture<>();
                CompletableFuture<String> a1=src.thenApply(s->{log.append("A ");return s+"a";});
                CompletableFuture<Void> b1=src.thenAccept(s->log.append("B "));
                CompletableFuture<String> a11=a1.thenApply(s->{log.append("A1 ");return s+"1";});
                CompletableFuture<Void> a12=a1.thenRun(()->log.append("A2 "));
                CompletableFuture<String> a13=a1.whenComplete((v,x)->log.append("A3 "));
                CompletableFuture<String> c1=src.whenComplete((v,x)->log.append("C "));
                CompletableFuture<Void> b11=b1.thenRun(()->log.append("B1 "));
                CompletableFuture<Void> a111=a11.thenRun(()->log.append("A11 "));
                String a=log+"["+st(a1)+"]";
                src.complete("s");
                a+="|"+log+"|"+st(a1)+","+st(a11)+","+st(a12)+","+st(a13)+","+st(c1)+","+st(b11)+","+st(a111);
                log.setLength(0);
                src.thenRun(()->log.append("L "));
                a1.thenRun(()->log.append("L1 "));
                a+="|"+log;
                return a;
            }
            public static String pendingExceptional(){
                StringBuilder log=new StringBuilder();
                CompletableFuture<String> src=new CompletableFuture<>();
                CompletableFuture<String> m=src.thenApply(s->{log.append("M ");return s;});
                CompletableFuture<String> ex=m.exceptionally(x->{log.append("X ");return "rec:"+d(x);});
                CompletableFuture<String> h=src.handle((v,x)->{log.append("H ");return v+"/"+d(x);});
                CompletableFuture<String> w=ex.whenComplete((v,x)->log.append("W:"+v+" "));
                CompletableFuture<Void> r=src.thenRun(()->log.append("R "));
                src.completeExceptionally(new IllegalStateException("p"));
                String a=log+"|"+st(m)+","+st(ex)+","+st(h)+","+st(w)+","+st(r);
                log.setLength(0);
                CompletableFuture<String> s2=new CompletableFuture<>();
                CompletableFuture<String> d1=s2.thenApply(s->{log.append("D1 ");return s+"1";});
                CompletableFuture<String> d2=d1.thenApply(s->{log.append("D2 ");return s+"2";});
                a+="|"+d1.complete("manual")+","+log+","+st(d2);
                s2.complete("s");
                a+=","+log+","+st(d1)+","+st(d2);
                log.setLength(0);
                CompletableFuture<String> s3=new CompletableFuture<>();
                CompletableFuture<String> e1=s3.thenApply(s->{log.append("E1 ");return s;});
                CompletableFuture<String> e2=s3.exceptionally(x->{log.append("E2 ");return "c:"+d(x);});
                s3.cancel(true);
                a+="|"+log+","+st(e1)+","+st(e2);
                return a;
            }
            public static String stageInterface(){
                StringBuilder log=new StringBuilder();
                CompletableFuture<String> cf=new CompletableFuture<>();
                CompletionStage<String> stage=cf;
                Object o=cf;
                String a=(stage.toCompletableFuture()==cf)+","+(o instanceof Future)+","+(o instanceof CompletionStage);
                CompletionStage<String> w=stage.whenComplete((v,x)->log.append("w:"+v+" "));
                CompletionStage<Integer> len=stage.thenApply(String::length);
                CompletionStage<Void> acc=len.thenAccept(n->log.append("len:"+n+" "));
                CompletionStage<Void> run=acc.thenRun(()->log.append("run "));
                CompletionStage<String> hd=stage.handle((v,x)->v+"+"+x);
                CompletionStage<String> ex=stage.exceptionally(x->"none");
                cf.complete("abc");
                a+="|"+log+","+st(w.toCompletableFuture())+","+st(len.toCompletableFuture())+","+st(run.toCompletableFuture())+","+st(hd.toCompletableFuture())+","+st(ex.toCompletableFuture());
                CompletionStage<String> failed=CompletableFuture.failedFuture(new IllegalStateException("s"));
                a+="|"+st(failed.thenApply(s->s).toCompletableFuture())+","+st(failed.exceptionally(x->d(x)).toCompletableFuture());
                return a;
            }
        }
        """;

    @Test void statesAndCompletion() throws Exception {
        matchesJvm(FIXTURE, "states", "timedGet");
    }

    @Test void exceptionWrapping() throws Exception {
        matchesJvm(FIXTURE, "wrapping", "cancellation");
    }

    @Test void dependentsOfCompletedStages() throws Exception {
        matchesJvm(FIXTURE, "dependentsOfDone", "exceptionalSources");
    }

    @Test void thenCompose() throws Exception {
        matchesJvm(FIXTURE, "compose");
    }

    @Test void dependentsRunInJdkOrder() throws Exception {
        matchesJvm(FIXTURE, "pendingOrder", "pendingExceptional");
    }

    @Test void completionStageInterface() throws Exception {
        matchesJvm(FIXTURE, "stageInterface");
    }

    @Test void leavesOutAsyncAndTimeoutMembers() {
        rejectsUnusedCall("java/util/concurrent/CompletableFuture", "runAsync", "(Ljava/lang/Runnable;)Ljava/util/concurrent/CompletableFuture;", INVOKESTATIC, false);
        rejectsUnusedCall("java/util/concurrent/CompletableFuture", "supplyAsync", "(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;", INVOKESTATIC, false);
        rejectsUnusedCall("java/util/concurrent/CompletableFuture", "completeAsync", "(Ljava/util/function/Supplier;)Ljava/util/concurrent/CompletableFuture;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/CompletableFuture", "thenApplyAsync", "(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;", INVOKEVIRTUAL, false);
        rejectsUnusedCall("java/util/concurrent/CompletionStage", "whenCompleteAsync", "(Ljava/util/function/BiConsumer;)Ljava/util/concurrent/CompletionStage;", INVOKEINTERFACE, true);
        rejectsUnusedCall("java/util/concurrent/CompletableFuture", "orTimeout", "(JLjava/util/concurrent/TimeUnit;)Ljava/util/concurrent/CompletableFuture;", INVOKEVIRTUAL, false);
    }
}
