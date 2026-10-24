package java.lang.reflect;

/** AOT-registered public interface shapes only; no class definition occurs in Wasm. */
public abstract class Proxy implements java.io.Serializable {
    protected final InvocationHandler h;
    protected Proxy(InvocationHandler handler){if(handler==null)throw new NullPointerException();h=handler;}
    public static Object newProxyInstance(ClassLoader loader,Class<?>[] interfaces,InvocationHandler handler){throw new UnsupportedOperationException();}
    public static Class<?> getProxyClass(ClassLoader loader,Class<?>... interfaces){throw new UnsupportedOperationException();}
    public static void javawValidate(Class<?>[] interfaces){for(Class<?> type:interfaces)type.getClass();}
    public static boolean isProxyClass(Class<?> type){throw new UnsupportedOperationException();}
    public static InvocationHandler getInvocationHandler(Object proxy){
        if(!isProxyClass(proxy.getClass()))throw new IllegalArgumentException();return ((Proxy)proxy).h;
    }
}
