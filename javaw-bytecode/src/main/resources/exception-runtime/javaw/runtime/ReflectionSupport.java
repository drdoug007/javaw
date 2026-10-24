package javaw.runtime;

/** Replaced with closed-world declaration tables when method discovery is requested. */
public final class ReflectionSupport {
    private ReflectionSupport(){}
    public static java.lang.reflect.Method[] declaredMethods(Class<?> type){
        throw new UnsupportedOperationException("declared-method metadata is unavailable for this class");
    }
}
