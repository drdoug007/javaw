package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;

/** T62: java.lang.ClassNotFoundException and the ReflectiveOperationException cause constructors. See tasks/T62-class-not-found-exception.md. */
class BytecodeClassNotFoundExceptionTest extends KitDifferential {
    private static final String FIXTURE = """
        class Lookup {
            static String find(String name) throws ClassNotFoundException {
                switch(name){
                    case "a.A": return "A";
                    case "bad": throw new ClassNotFoundException(name, new IllegalArgumentException("syntax"));
                    case "none": throw new ClassNotFoundException();
                    default: throw new ClassNotFoundException(name);
                }
            }
        }
        class Shadow extends ClassNotFoundException {
            final StringBuilder log=new StringBuilder();
            Shadow(String s,Throwable t){super(s,t);}
            public Throwable getCause(){log.append("cause;");return null;}
            public String getMessage(){log.append("message;");return "shadow:"+super.getMessage();}
        }
        public class Fixture {
            static String name(Object o){return o==null?"null":o.getClass().getName();}

            public static String constructors(){
                StringBuilder out=new StringBuilder();
                IllegalStateException root=new IllegalStateException("boom");
                ClassNotFoundException a=new ClassNotFoundException();
                ClassNotFoundException b=new ClassNotFoundException("x.Y");
                ClassNotFoundException c=new ClassNotFoundException("x.Z",root);
                ClassNotFoundException d=new ClassNotFoundException(null,null);
                out.append(a.getMessage()).append(',').append(a.getCause()).append(',').append(a.getException()).append(',').append(a).append(';');
                out.append(b.getMessage()).append(',').append(b.getCause()).append(',').append(b.getException()).append(',').append(b).append(';');
                out.append(c.getMessage()).append(',').append(c.getCause()==root).append(',').append(c.getException()==root).append(',').append(c).append(';');
                out.append(d.getMessage()).append(',').append(d.getException()).append(',').append(d.getLocalizedMessage()).append(',').append(d);
                return out.toString();
            }
            public static String hierarchy(){
                StringBuilder out=new StringBuilder();
                Object e=new ClassNotFoundException("q");
                out.append(e instanceof ReflectiveOperationException).append(',').append(e instanceof Exception).append(',');
                out.append(e instanceof RuntimeException).append(',').append(e instanceof Error).append(';');
                for(String n:new String[]{"a.A","b.B","bad","none"}){
                    try{out.append(Lookup.find(n));}
                    catch(ReflectiveOperationException r){
                        out.append(name(r)).append(':').append(r.getMessage()).append(':').append(name(r.getCause()));
                    }
                    out.append('|');
                }
                try{Lookup.find("c.C");}catch(Exception x){out.append(x instanceof ClassNotFoundException).append(',').append(((ClassNotFoundException)x).getException());}
                return out.toString();
            }
            public static String initCauseDisallowed(){
                StringBuilder out=new StringBuilder();
                ClassNotFoundException[] all={new ClassNotFoundException(),new ClassNotFoundException("m"),new ClassNotFoundException("n",null),new ClassNotFoundException("o",new Error("e"))};
                for(ClassNotFoundException e:all){
                    try{e.initCause(new RuntimeException("late"));out.append("set;");}
                    catch(IllegalStateException s){out.append(name(s)).append(',').append(s.getCause()==e).append(',').append(name(e.getCause())).append(';');}
                }
                try{all[1].initCause(null);out.append("set");}catch(IllegalStateException s){out.append("ise");}
                return out.toString();
            }
            public static String reflectiveCauses(){
                StringBuilder out=new StringBuilder();
                IllegalArgumentException root=new IllegalArgumentException("bad arg");
                ReflectiveOperationException a=new ReflectiveOperationException("wrap",root);
                ReflectiveOperationException b=new ReflectiveOperationException(root);
                ReflectiveOperationException c=new ReflectiveOperationException((Throwable)null);
                ReflectiveOperationException d=new ReflectiveOperationException("late");
                out.append(a.getMessage()).append(',').append(a.getCause()==root).append(',').append(a).append(';');
                out.append(b.getMessage()).append(',').append(b.getCause()==root).append(',').append(b).append(';');
                out.append(c.getMessage()).append(',').append(c.getCause()).append(';');
                try{c.initCause(root);out.append("set;");}catch(IllegalStateException s){out.append("ise;");}
                out.append(d.initCause(root)==d).append(',').append(d.getCause()==root).append(',');
                ReflectiveOperationException e=new ReflectiveOperationException();
                out.append(e.getMessage()).append(',').append(e.getCause()).append(',').append(e.initCause(null)==e).append(',').append(e.getCause());
                return out.toString();
            }
            public static String getExceptionUsesSuper(){
                StringBuilder out=new StringBuilder();
                Error root=new Error("deep");
                Shadow s=new Shadow("p.Q",root);
                out.append(s.getException()==root).append(s.log).append('|');s.log.setLength(0);
                out.append(s.getCause()).append(s.log).append('|');s.log.setLength(0);
                out.append(s).append(s.log).append('|');s.log.setLength(0);
                ClassNotFoundException outer=new ClassNotFoundException("outer",new ClassNotFoundException("inner"));
                Throwable t=outer;int depth=0;
                while(t!=null){out.append(t.getMessage()).append('>');t=t.getCause();depth++;}
                out.append(depth);
                return out.toString();
            }
        }
        """;

    @Test void constructorsAndHierarchy() throws Exception {
        matchesJvm(FIXTURE, "constructors", "hierarchy");
    }

    @Test void causes() throws Exception {
        matchesJvm(FIXTURE, "initCauseDisallowed", "reflectiveCauses", "getExceptionUsesSuper");
    }

    @Test void leavesOutClassLookup() {
        rejectsUnusedCall("java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", INVOKESTATIC, false);
        rejectsUnusedCall("java/lang/ClassNotFoundException", "<init>", "(Ljava/lang/Throwable;)V", INVOKESPECIAL, false);
    }
}
