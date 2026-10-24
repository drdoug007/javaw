package java.lang.invoke;

/** Closed-world caller lookups and byte-array views; runtime class definition is excluded. */
public final class MethodHandles {
    private MethodHandles() {}
    public static Lookup lookup(){throw new UnsupportedOperationException();}
    public static Lookup privateLookupIn(Class<?> target,Lookup lookup) throws IllegalAccessException {throw new UnsupportedOperationException();}
    public static abstract class Lookup {
        private final Class<?> caller;
        protected Lookup(Class<?> caller){this.caller=caller;}
        public Class<?> lookupClass(){return caller;}
        public boolean hasFullPrivilegeAccess(){return true;}
        protected abstract Lookup javawSpecialLookup(Class<?> specialCaller) throws IllegalAccessException;
        protected abstract MethodHandle javawFind(Class<?> owner,String name,MethodType type,int kind) throws NoSuchMethodException,IllegalAccessException;
        public MethodHandle findStatic(Class<?> owner,String name,MethodType type) throws NoSuchMethodException,IllegalAccessException {return javawFind(owner,name,type,6);}
        public MethodHandle findVirtual(Class<?> owner,String name,MethodType type) throws NoSuchMethodException,IllegalAccessException {return javawFind(owner,name,type,5);}
        public MethodHandle findConstructor(Class<?> owner,MethodType type) throws NoSuchMethodException,IllegalAccessException {return javawFind(owner,"<init>",type,8);}
        public MethodHandle findSpecial(Class<?> owner,String name,MethodType type,Class<?> specialCaller) throws NoSuchMethodException,IllegalAccessException {
            specialCaller.getClass();return javawSpecialLookup(specialCaller).javawFind(owner,name,type,7);
        }
    }
    public static VarHandle byteArrayViewVarHandle(Class<?> arrayClass,java.nio.ByteOrder order) {
        if(arrayClass==null || order==null)throw new NullPointerException();
        return new VarHandle(arrayClass,order==java.nio.ByteOrder.LITTLE_ENDIAN,false);
    }
}
