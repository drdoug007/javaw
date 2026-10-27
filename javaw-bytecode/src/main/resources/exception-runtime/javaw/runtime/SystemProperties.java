package javaw.runtime;

/** Explicit per-instance string properties; no implicit host environment or JVM defaults. */
public final class SystemProperties {
    private SystemProperties() {}
    private static Entry first;
    private static final class Entry {
        final String key;String value;Entry next;
        Entry(String key,String value,Entry next){this.key=key;this.value=value;this.next=next;}
    }
    private static void checkKey(String key){
        if(key==null)throw new NullPointerException();
        if(key.isEmpty())throw new IllegalArgumentException();
    }
    public static String getProperty(String key){
        checkKey(key);
        for(Entry entry=first;entry!=null;entry=entry.next)if(key.equals(entry.key))return entry.value;
        return null;
    }
    public static String getProperty(String key,String fallback){String value=getProperty(key);return value==null?fallback:value;}
    public static String setProperty(String key,String value){
        checkKey(key);if(value==null)throw new NullPointerException();
        for(Entry entry=first;entry!=null;entry=entry.next)if(key.equals(entry.key)){String previous=entry.value;entry.value=value;return previous;}
        first=new Entry(key,value,first);return null;
    }
    public static String clearProperty(String key){
        checkKey(key);Entry previous=null;
        for(Entry entry=first;entry!=null;entry=entry.next){
            if(key.equals(entry.key)){if(previous==null)first=entry.next;else previous.next=entry.next;return entry.value;}
            previous=entry;
        }
        return null;
    }
}
