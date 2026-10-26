package java.util;
public class IllegalFormatCodePointException extends IllegalFormatException {private final int codePoint;public IllegalFormatCodePointException(int value){codePoint=value;}public int getCodePoint(){return codePoint;}}
