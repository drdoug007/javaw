package java.util;
public class IllegalFormatWidthException extends IllegalFormatException {private final int width;public IllegalFormatWidthException(int value){width=value;}public int getWidth(){return width;}}
