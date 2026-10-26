package java.util;
public class IllegalFormatPrecisionException extends IllegalFormatException {private final int precision;public IllegalFormatPrecisionException(int value){precision=value;}public int getPrecision(){return precision;}}
