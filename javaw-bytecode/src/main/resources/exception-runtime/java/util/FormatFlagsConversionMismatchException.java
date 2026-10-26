package java.util;
public class FormatFlagsConversionMismatchException extends IllegalFormatException {
 private final String flags;private final char conversion;
 public FormatFlagsConversionMismatchException(String f,char c){if(f==null)throw new NullPointerException();flags=f;conversion=c;}
 public String getFlags(){return flags;}public char getConversion(){return conversion;}
}
