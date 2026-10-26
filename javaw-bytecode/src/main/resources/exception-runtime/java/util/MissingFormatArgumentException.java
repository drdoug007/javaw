package java.util;
public class MissingFormatArgumentException extends IllegalFormatException {private final String specifier;public MissingFormatArgumentException(String value){if(value==null)throw new NullPointerException();specifier=value;}public String getFormatSpecifier(){return specifier;}}
