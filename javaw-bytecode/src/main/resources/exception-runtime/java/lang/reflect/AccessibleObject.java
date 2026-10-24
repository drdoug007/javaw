package java.lang.reflect;

/** Access override for the selected single unnamed-module domain. */
public class AccessibleObject {
    private boolean override;
    protected AccessibleObject(){}
    public void setAccessible(boolean flag){override=flag;}
    public boolean trySetAccessible(){override=true;return true;}
    public boolean canAccess(Object receiver){return javawCanAccess(receiver,AccessibleObject.class);}
    public boolean javawCanAccess(Object receiver,Class<?> caller){throw new UnsupportedOperationException();}
    protected final boolean javawAccess(Class<?> owner,int modifiers,Class<?> caller,Object receiver,boolean constructor){
        if(constructor || (modifiers&8)!=0){if(receiver!=null)throw new IllegalArgumentException();}else if(receiver==null || !owner.isInstance(receiver))throw new IllegalArgumentException();
        try{javawCheck(owner,modifiers,caller,receiver);return true;}catch(IllegalAccessException denied){return false;}
    }
    public boolean isAccessible(){return override;}
    public static void setAccessible(AccessibleObject[] values,boolean flag){for(AccessibleObject value:values)value.setAccessible(flag);}
    protected final void javawCheck(Class<?> owner,int modifiers,Class<?> caller,Object receiver) throws IllegalAccessException {
        if(override)return;
        boolean samePackage=packageEquals(owner,caller);
        if((owner.getModifiers()&1)==0 && !samePackage)throw new IllegalAccessException();
        if((modifiers&1)!=0)return;
        if((modifiers&2)!=0){if(javaw.runtime.ReflectionSupport.nestHost(owner)==javaw.runtime.ReflectionSupport.nestHost(caller))return;}
        else if(samePackage)return;
        else if((modifiers&4)!=0 && owner.isAssignableFrom(caller) && ((modifiers&8)!=0 || receiver!=null && caller.isInstance(receiver)))return;
        throw new IllegalAccessException();
    }
    private static boolean packageEquals(Class<?> left,Class<?> right){
        String a=left.getName(),b=right.getName();int x=-1,y=-1;
        for(int i=0;i<a.length();i++)if(a.charAt(i)=='.')x=i;
        for(int i=0;i<b.length();i++)if(b.charAt(i)=='.')y=i;
        if(x!=y)return false;for(int i=0;i<x;i++)if(a.charAt(i)!=b.charAt(i))return false;return true;
    }
}
