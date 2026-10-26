package java.util;
public class IllegalFormatArgumentIndexException extends IllegalFormatException {private final int index;public IllegalFormatArgumentIndexException(int value){index=value;}public int getIndex(){return index;}}
