package java.util;
public class IllegalFormatFlagsException extends IllegalFormatException {private final String flags;public IllegalFormatFlagsException(String value){if(value==null)throw new NullPointerException();flags=value;}public String getFlags(){return flags;}}
