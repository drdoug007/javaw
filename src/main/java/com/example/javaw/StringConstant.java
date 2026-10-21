package com.example.javaw;

public class StringConstant {
    public final int offset;
    public final int length;
    public final byte[] bytes;

    public StringConstant(int offset, int length, byte[] bytes) {
        this.offset = offset;
        this.length = length;
        this.bytes = bytes;
    }
}
