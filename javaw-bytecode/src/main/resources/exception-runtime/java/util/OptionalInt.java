package java.util;import java.util.function.*;
 public final class OptionalInt{
 private static final OptionalInt EMPTY=new OptionalInt();private final boolean present;private final int value;
 private OptionalInt(){present=false;value=0;}private OptionalInt(int value){this.value=value;present=true;}
 public static OptionalInt empty(){return EMPTY;}public static OptionalInt of(int value){return new OptionalInt(value);}
 public boolean isPresent(){return present;}public boolean isEmpty(){return !present;}public int getAsInt(){if(!present)throw new NoSuchElementException("No value present");return value;}
 public void ifPresent(IntConsumer action){if(present)action.accept(value);}public void ifPresentOrElse(IntConsumer action,Runnable empty){if(present)action.accept(value);else empty.run();}
 public int orElse(int fallback){return present?value:fallback;}public int orElseGet(IntSupplier supplier){return present?value:supplier.getAsInt();}
 public int orElseThrow(){return getAsInt();}public <X extends Throwable>int orElseThrow(Supplier<? extends X> supplier)throws X{if(!present)throw supplier.get();return value;}
 public boolean equals(Object other){if(this==other)return true;if(!(other instanceof OptionalInt))return false;OptionalInt that=(OptionalInt)other;return present==that.present && (!present || Integer.compare(value,that.value)==0);}
 public int hashCode(){return present?Integer.hashCode(value):0;}public String toString(){return present?"OptionalInt[".concat(Integer.toString(value)).concat("]"):"OptionalInt.empty";}
 }
