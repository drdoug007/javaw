package com.example.javaw;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

/** Host-independent byte streams. Files and sockets require separate host adapters. */
final class IoSupport {
    static void addTypes(CompilationUnit cu) {
        if (cu.findAll(ClassOrInterfaceType.class).stream().noneMatch(t -> java.util.Set.of(
                "InputStream", "OutputStream", "ByteArrayInputStream", "ByteArrayOutputStream", "IOException").contains(t.getNameAsString()))) return;
        LibrarySupport.add(cu, "java.io", """
            class IOException extends Exception {
                public IOException() { super(); }
                public IOException(String message) { super(message); }
            }
            abstract class InputStream implements AutoCloseable {
                public abstract int read();
                public int read(byte[] bytes) { if(bytes==null) throw new NullPointerException(); return read(bytes,0,bytes.length); }
                public int read(byte[] bytes,int offset,int length) {
                    if(bytes==null) throw new NullPointerException();
                    if(offset<0 || length<0 || offset>bytes.length-length) throw new IndexOutOfBoundsException();
                    if(length==0) return 0;
                    int value=read(); if(value<0) return -1; bytes[offset]=(byte)value;
                    int count=1; while(count<length) { value=read(); if(value<0) break; bytes[offset+count]=(byte)value; count++; }
                    return count;
                }
                public long skip(long count) { long skipped=0; while(skipped<count && read()>=0) skipped++; return skipped; }
                public int available() { return 0; }
                public boolean markSupported() { return false; }
                public void mark(int limit) {}
                public void reset() { throw new IOException("mark/reset not supported"); }
                public void close() {}
            }
            abstract class OutputStream implements AutoCloseable {
                public abstract void write(int value);
                public void write(byte[] bytes) { if(bytes==null) throw new NullPointerException(); write(bytes,0,bytes.length); }
                public void write(byte[] bytes,int offset,int length) {
                    if(bytes==null) throw new NullPointerException();
                    if(offset<0 || length<0 || offset>bytes.length-length) throw new IndexOutOfBoundsException();
                    for(int i=0;i<length;i++) write(bytes[offset+i]);
                }
                public void flush() {}
                public void close() {}
            }
            class ByteArrayInputStream extends InputStream {
                private byte[] buffer; private int position; private int limit; private int marked;
                public ByteArrayInputStream(byte[] bytes) { this(bytes,0,__javaw_input_length(bytes)); }
                private static int __javaw_input_length(byte[] bytes) { if(bytes==null) throw new NullPointerException(); return bytes.length; }
                public ByteArrayInputStream(byte[] bytes,int offset,int length) {
                    if(bytes==null) throw new NullPointerException();
                    if(offset<0 || length<0 || offset>bytes.length) throw new IndexOutOfBoundsException();
                    buffer=bytes; position=offset; marked=offset;
                    limit=length>bytes.length-offset ? bytes.length : offset+length;
                }
                public int read() { if(position>=limit) return -1; return buffer[position++] & 255; }
                public int read(byte[] bytes,int offset,int length) {
                    if(bytes==null) throw new NullPointerException();
                    if(offset<0 || length<0 || offset>bytes.length-length) throw new IndexOutOfBoundsException();
                    if(position>=limit) return -1;
                    int count=length<limit-position ? length : limit-position;
                    for(int i=0;i<count;i++) bytes[offset+i]=buffer[position+i]; position+=count; return count;
                }
                public long skip(long count) {
                    if(count<=0) return 0L; int remaining=limit-position;
                    int skipped=count>remaining ? remaining : (int)count; position+=skipped; return (long)skipped;
                }
                public int available() { return limit-position; }
                public boolean markSupported() { return true; }
                public void mark(int readLimit) { marked=position; }
                public void reset() { position=marked; }
            }
            class ByteArrayOutputStream extends OutputStream {
                private byte[] buffer; private int count;
                public ByteArrayOutputStream() { this(32); }
                public ByteArrayOutputStream(int size) {
                    if(size<0) throw new IllegalArgumentException(); buffer=new byte[size];
                }
                public void write(int value) {
                    if(count==buffer.length) {
                        int capacity=buffer.length==0 ? 1 : buffer.length*2;
                        byte[] grown=new byte[capacity]; for(int i=0;i<count;i++) grown[i]=buffer[i]; buffer=grown;
                    }
                    buffer[count++]=(byte)value;
                }
                public int size() { return count; }
                public void reset() { count=0; }
                public byte[] toByteArray() { byte[] copy=new byte[count]; for(int i=0;i<count;i++) copy[i]=buffer[i]; return copy; }
                public void writeTo(OutputStream target) { target.write(buffer,0,count); }
            }
            """);
    }
}
