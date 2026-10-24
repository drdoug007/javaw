package java.util;import java.util.function.*;
 public final class OptionalLong{
 private static final OptionalLong EMPTY=new OptionalLong();private final boolean present;private final long value;
 private OptionalLong(){present=false;value=0;}private OptionalLong(long value){this.value=value;present=true;}
 public static OptionalLong empty(){return EMPTY;}public static OptionalLong of(long value){return new OptionalLong(value);}
 public boolean isPresent(){return present;}public boolean isEmpty(){return !present;}public long getAsLong(){if(!present)throw new NoSuchElementException("No value present");return value;}
 public void ifPresent(LongConsumer action){if(present)action.accept(value);}public void ifPresentOrElse(LongConsumer action,Runnable empty){if(present)action.accept(value);else empty.run();}
 public long orElse(long fallback){return present?value:fallback;}public long orElseGet(LongSupplier supplier){return present?value:supplier.getAsLong();}
 public long orElseThrow(){return getAsLong();}public <X extends Throwable>long orElseThrow(Supplier<? extends X> supplier)throws X{if(!present)throw supplier.get();return value;}
 public boolean equals(Object other){if(this==other)return true;if(!(other instanceof OptionalLong))return false;OptionalLong that=(OptionalLong)other;return present==that.present && (!present || Long.compare(value,that.value)==0);}
 public int hashCode(){return present?Long.hashCode(value):0;}public String toString(){return present?"OptionalLong[".concat(Long.toString(value)).concat("]"):"OptionalLong.empty";}
 }
