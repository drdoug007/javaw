package java.lang.reflect;

public interface InvocationHandler {
    Object invoke(Object proxy,Method method,Object[] arguments) throws Throwable;
}
