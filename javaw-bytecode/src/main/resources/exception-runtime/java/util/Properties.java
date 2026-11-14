package java.util;
import java.util.function.*;

/**
 * A permanent set of key/value pairs whose entries live in the inherited Hashtable. The JDK 9+
 * implementation keeps its entries in a private concurrent map; here the observable behaviour that
 * library callers depend on (defaults chain, String-only lookups, the null rules copied from that
 * map, and which overridable methods are called) is matched, while multi-entry iteration order
 * follows the Hashtable instead of the concurrent map.
 */
public class Properties extends Hashtable<Object,Object>{
    protected Properties defaults;

    public Properties(){super();}
    public Properties(int initialCapacity){
        super();
        if(initialCapacity<0)throw new IllegalArgumentException();
    }
    public Properties(Properties defaults){
        super();
        this.defaults=defaults;
    }

    public Object setProperty(String key,String value){return put(key,value);}
    public String getProperty(String key){
        Object value=super.get(key);
        String text=value instanceof String?(String)value:null;
        return text==null && defaults!=null?defaults.getProperty(key):text;
    }
    public String getProperty(String key,String defaultValue){
        String value=getProperty(key);
        return value==null?defaultValue:value;
    }

    public Enumeration<?> propertyNames(){
        Hashtable<String,Object> h=new Hashtable<String,Object>();
        enumerate(h);
        return h.keys();
    }
    public Set<String> stringPropertyNames(){
        Map<String,String> h=new HashMap<String,String>();
        enumerateStringProperties(h);
        return Collections.unmodifiableSet(h.keySet());
    }

    public void putAll(Map<?,?> t){
        t.size();
        for(Map.Entry<?,?> e:t.entrySet())super.put(e.getKey(),e.getValue());
    }
    public boolean remove(Object key,Object value){
        if(key==null)throw new NullPointerException();
        return value!=null && super.remove(key,value);
    }
    public Object merge(Object key,Object value,BiFunction<? super Object,? super Object,?> remappingFunction){
        if(key==null || value==null || remappingFunction==null)throw new NullPointerException();
        return super.merge(key,value,remappingFunction);
    }
    public void forEach(BiConsumer<? super Object,? super Object> action){
        Objects.requireNonNull(action);
        for(Object key:new ArrayList<Object>(super.keySet())){
            Object value=super.get(key);
            if(value!=null)action.accept(key,value);
        }
    }

    public String toString(){
        StringBuilder out=new StringBuilder("{");
        boolean first=true;
        for(Map.Entry<Object,Object> e:super.entrySet()){
            if(!first)out.append(", ");
            first=false;
            out.append(e.getKey()).append('=').append(e.getValue());
        }
        return out.append('}').toString();
    }
    public boolean equals(Object o){
        if(!(o instanceof Map))return false;
        Map<?,?> m=(Map<?,?>)o;
        for(Map.Entry<Object,Object> e:super.entrySet()){
            Object value=e.getValue();
            Object v=m.get(e.getKey());
            if(v==null || (v!=value && !v.equals(value)))return false;
        }
        for(Map.Entry<?,?> e:m.entrySet()){
            Object key=e.getKey();
            Object value=e.getValue();
            if(key==null || value==null)return false;
            Object mine=super.get(key);
            if(mine==null || (value!=mine && !value.equals(mine)))return false;
        }
        return true;
    }

    private void enumerate(Map<String,Object> h){
        if(defaults!=null)defaults.enumerate(h);
        for(Map.Entry<Object,Object> e:entrySet())h.put((String)e.getKey(),e.getValue());
    }
    private void enumerateStringProperties(Map<String,String> h){
        if(defaults!=null)defaults.enumerateStringProperties(h);
        for(Map.Entry<Object,Object> e:entrySet()){
            Object k=e.getKey();
            Object v=e.getValue();
            if(k instanceof String && v instanceof String)h.put((String)k,(String)v);
        }
    }
}
