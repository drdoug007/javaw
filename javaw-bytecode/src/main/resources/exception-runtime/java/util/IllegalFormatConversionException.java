package java.util;
public class IllegalFormatConversionException extends IllegalFormatException {
 private final char conversion;private final Class<?> argumentClass;
 public IllegalFormatConversionException(char c,Class<?> type){if(type==null)throw new NullPointerException();conversion=c;argumentClass=type;}
 public char getConversion(){return conversion;}public Class<?> getArgumentClass(){return argumentClass;}
}
