package java.lang;

/** One application-loader identity; class definition and resource loading remain excluded. */
public abstract class ClassLoader {
    private static final ClassLoader APPLICATION=new Closed();
    protected ClassLoader(){}
    private static final class Closed extends ClassLoader {}
    public static ClassLoader javawApplication(){return APPLICATION;}
    public static ClassLoader getSystemClassLoader(){return APPLICATION;}
    public ClassLoader getParent(){return null;}
}
