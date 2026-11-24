package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T65: java.lang.Throwable implements java.io.Serializable. See tasks/T65-throwable-serializable.md. */
class BytecodeThrowableSerializableTest extends KitDifferential {
    private static final String FIXTURE = """
        import java.io.Serializable;
        class AppException extends Exception {
            private static final long serialVersionUID = 7L;
            final int code;
            AppException(String m,int code){super(m);this.code=code;}
        }
        class AppFailure extends RuntimeException implements Serializable {
            AppFailure(String m,Throwable c){super(m,c);}
        }
        interface Tagged extends Serializable { String tag(); }
        class TaggedError extends Error implements Tagged {
            TaggedError(){super("tagged");}
            public String tag(){return "T:"+getMessage();}
        }
        class Plain {}
        public class Fixture {
            static boolean ser(Object o){return o instanceof Serializable;}
            static <T extends Serializable> String bound(T value){return value.getClass().getName();}
            static String keep(Serializable s){return s==null?"null":s.toString();}

            public static String instanceChecks(){
                Object[] all={new Throwable("t"),new Exception("e"),new RuntimeException("r"),new Error("x"),
                    new IllegalStateException("s"),new AppException("a",3),new AppFailure("f",null),new TaggedError(),
                    new Plain(),"text",Integer.valueOf(4),new int[1]};
                StringBuilder out=new StringBuilder();
                for(Object o:all)out.append(ser(o)?'Y':'n');
                out.append(',').append(ser(new Object())).append(',').append(ser(null));
                return out.toString();
            }
            public static String assignments(){
                StringBuilder out=new StringBuilder();
                Serializable a=new Throwable("one");
                Serializable b=new RuntimeException("two",new Error("cause"));
                Serializable c=new AppException("three",9);
                out.append(keep(a)).append('|').append(keep(b)).append('|').append(keep(c)).append('|').append(keep(null)).append('|');
                Serializable[] array={new Exception("x"),new AppFailure("y",null),"z",new TaggedError()};
                for(Serializable s:array)out.append(s instanceof Throwable?((Throwable)s).getMessage():s).append(',');
                out.append(bound(new Error())).append(',').append(bound(new AppException("b",1))).append(',').append(bound(new TaggedError()));
                return out.toString();
            }
            public static String casts(){
                StringBuilder out=new StringBuilder();
                Object o=new IllegalArgumentException("bad");
                Serializable s=(Serializable)o;
                out.append(s.equals(o)).append(',').append(s.hashCode()==o.hashCode()).append(',');
                Throwable back=(Throwable)s;
                out.append(back.getMessage()).append(',');
                try{throw (RuntimeException)s;}catch(IllegalArgumentException e){out.append("caught:").append(e==o).append(',');}
                try{Serializable p=(Serializable)(Object)new Plain();out.append("no").append(p!=null);}catch(ClassCastException e){out.append("cce,");}
                Tagged t=new TaggedError();Serializable ts=t;
                out.append(t.tag()).append(',').append(ts instanceof Error).append(',').append(ts instanceof Tagged);
                return out.toString();
            }
            public static String unchanged(){
                StringBuilder out=new StringBuilder();
                Exception e=new Exception("outer",new AppException("inner",5));
                out.append(e).append('|').append(e.getCause()).append('|').append(((AppException)e.getCause()).code).append('|');
                Throwable t=new Throwable();
                out.append(t.getMessage()).append(',').append(t.getCause()).append(',').append(t).append('|');
                RuntimeException r=new RuntimeException(new Error("deep"));
                out.append(r.getMessage()).append('|');
                r.addSuppressed(new AppFailure("s1",null));r.addSuppressed(new TaggedError());
                out.append(r.getSuppressed().length).append(',').append(r.getSuppressed()[1].getMessage()).append('|');
                Throwable init=new Throwable("i");init.initCause(new Exception("c"));
                out.append(init.getCause().getMessage()).append('|');
                try{init.initCause(null);out.append("no");}catch(IllegalStateException x){out.append("ise");}
                try{throw new AppException("thrown",2);}catch(Exception x){out.append('|').append(x.getClass().getName()).append(':').append(x.getMessage());}
                return out.toString();
            }
        }
        """;

    @Test void serializableTypes() throws Exception {
        matchesJvm(FIXTURE, "instanceChecks", "assignments", "casts");
    }

    @Test void behaviourUnchanged() throws Exception {
        matchesJvm(FIXTURE, "unchanged");
    }

    @Test void leavesOutSerializationHooks() {
        rejectsUnusedCall("java/lang/Throwable", "writeObject", "(Ljava/io/ObjectOutputStream;)V", INVOKESPECIAL, false);
        rejectsUnusedCall("java/lang/Throwable", "readObject", "(Ljava/io/ObjectInputStream;)V", INVOKESPECIAL, false);
    }
}
