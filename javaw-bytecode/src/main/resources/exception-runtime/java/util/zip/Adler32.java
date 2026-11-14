package java.util.zip;
public class Adler32 implements Checksum {
 private int adler = 1;
 public Adler32(){}
 public void update(int b){add(b & 0xff);}
 public void update(byte[] b, int off, int len){
  CRC32.checkRange(b, off, len);
  for (int i = off; i < off + len; i++) add(b[i] & 0xff);
 }
 private void add(int x){
  int a = ((adler & 0xffff) + x) % 65521;
  int b = ((adler >>> 16) + a) % 65521;
  adler = (b << 16) | a;
 }
 public void reset(){adler = 1;}
 public long getValue(){return (long) adler & 0xffffffffL;}
}
