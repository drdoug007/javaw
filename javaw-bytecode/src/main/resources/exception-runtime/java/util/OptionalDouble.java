package java.util;import java.util.function.*;
 public final class OptionalDouble{
 private static final OptionalDouble EMPTY=new OptionalDouble();private final boolean present;private final double value;
 private OptionalDouble(){present=false;value=0;}private OptionalDouble(double value){this.value=value;present=true;}
 public static OptionalDouble empty(){return EMPTY;}public static OptionalDouble of(double value){return new OptionalDouble(value);}
 public boolean isPresent(){return present;}public boolean isEmpty(){return !present;}public double getAsDouble(){if(!present)throw new NoSuchElementException("No value present");return value;}
 public void ifPresent(DoubleConsumer action){if(present)action.accept(value);}public void ifPresentOrElse(DoubleConsumer action,Runnable empty){if(present)action.accept(value);else empty.run();}
 public double orElse(double fallback){return present?value:fallback;}public double orElseGet(DoubleSupplier supplier){return present?value:supplier.getAsDouble();}
 public double orElseThrow(){return getAsDouble();}public <X extends Throwable>double orElseThrow(Supplier<? extends X> supplier)throws X{if(!present)throw supplier.get();return value;}
 public boolean equals(Object other){if(this==other)return true;if(!(other instanceof OptionalDouble))return false;OptionalDouble that=(OptionalDouble)other;return present==that.present && (!present || Double.compare(value,that.value)==0);}
 public int hashCode(){return present?Double.hashCode(value):0;}public String toString(){return present?"OptionalDouble[".concat(Double.toString(value)).concat("]"):"OptionalDouble.empty";}
 }
