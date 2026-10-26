package java.util;
public class UnknownFormatConversionException extends IllegalFormatException {private final String conversion;public UnknownFormatConversionException(String value){if(value==null)throw new NullPointerException();conversion=value;}public String getConversion(){return conversion;}}
