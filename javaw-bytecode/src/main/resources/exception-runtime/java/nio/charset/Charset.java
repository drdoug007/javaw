package java.nio.charset;

/** Six standard built-in charsets; streaming codecs and providers are not bundled. */
public abstract class Charset implements Comparable<Charset> {
    private final String name;
    private final java.util.Set<String> aliases;
    protected Charset(String canonicalName,String[] aliases){
        if(!javaw.runtime.TextCharsets.validName(canonicalName))throw new IllegalCharsetNameException(canonicalName);
        this.name=canonicalName;
        java.util.Set<String> names=new java.util.LinkedHashSet<>();
        if(aliases!=null)for(String alias:aliases){
            if(!javaw.runtime.TextCharsets.validName(alias))throw new IllegalCharsetNameException(alias);names.add(alias);}
        this.aliases=java.util.Collections.unmodifiableSet(names);
    }
    public final String name(){return name;}
    public final java.util.Set<String> aliases(){return aliases;}
    public String displayName(){return name;}
    public final boolean isRegistered(){return !name.regionMatches(true,0,"x-",0,2);}
    public boolean canEncode(){return true;}
    public abstract boolean contains(Charset other);
    public final int compareTo(Charset other){return name.compareToIgnoreCase(other.name);}
    public final int hashCode(){return name.hashCode();}
    public final boolean equals(Object other){return other instanceof Charset && name.equals(((Charset)other).name);}
    public final String toString(){return name;}
    public static Charset defaultCharset(){return StandardCharsets.UTF_8;}
    public static boolean isSupported(String name){try{forName(name);return true;}catch(UnsupportedCharsetException e){return false;}}
    public static Charset forName(String name){
        if(name==null)throw new IllegalArgumentException("Null charset name");
        if(!javaw.runtime.TextCharsets.validName(name))throw new IllegalCharsetNameException(name);
        int id;try{id=javaw.runtime.TextCharsets.lookup(name);}catch(java.io.UnsupportedEncodingException e){throw new UnsupportedCharsetException(name);}
        switch(id){case 0:return StandardCharsets.UTF_8;case 1:return StandardCharsets.ISO_8859_1;
            case 2:return StandardCharsets.US_ASCII;case 3:return StandardCharsets.UTF_16BE;
            case 4:return StandardCharsets.UTF_16LE;default:return StandardCharsets.UTF_16;}
    }
    static final class Builtin extends Charset {
        private final int id;
        Builtin(int id){super(javaw.runtime.TextCharsets.canonical(id),javaw.runtime.TextCharsets.aliases(id));this.id=id;}
        public boolean contains(Charset other){
            if(other==null && id!=1 && id!=2)throw new NullPointerException();
            if(!(other instanceof Builtin))return false;int target=((Builtin)other).id;
            return id==2?target==2:id==1?target==1 || target==2:true;
        }
    }
}
