package java.util;
public class DuplicateFormatFlagsException extends IllegalFormatException {private final String flags;public DuplicateFormatFlagsException(String value){if(value==null)throw new NullPointerException();flags=value;}public String getFlags(){return flags;}}
