package java.lang.invoke;

/** Explicit byte-array view factory; lookup/runtime code generation is not supplied. */
public final class MethodHandles {
    private MethodHandles() {}
    public static VarHandle byteArrayViewVarHandle(Class<?> arrayClass,java.nio.ByteOrder order) {
        if(arrayClass==null || order==null)throw new NullPointerException();
        return new VarHandle(arrayClass,order==java.nio.ByteOrder.LITTLE_ENDIAN,false);
    }
}
