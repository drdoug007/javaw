package java.util;
public class MissingFormatWidthException extends IllegalFormatException {private final String specifier;public MissingFormatWidthException(String value){if(value==null)throw new NullPointerException();specifier=value;}public String getFormatSpecifier(){return specifier;}}
