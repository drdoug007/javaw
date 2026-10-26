package java.util;
public class UnknownFormatFlagsException extends IllegalFormatException {private final String flags;public UnknownFormatFlagsException(String value){if(value==null)throw new NullPointerException();flags=value;}public String getFlags(){return flags;}}
