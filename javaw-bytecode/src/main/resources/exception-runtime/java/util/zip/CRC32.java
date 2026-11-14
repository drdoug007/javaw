package java.util.zip;
public class CRC32 implements Checksum {
 private static final int[] TABLE = new int[256];
 static {
  for (int n = 0; n < 256; n++) {
   int c = n;
   for (int k = 0; k < 8; k++) c = (c & 1) != 0 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
   TABLE[n] = c;
  }
 }
 private int crc;
 public CRC32(){}
 public void update(int b){add(b & 0xff);}
 public void update(byte[] b, int off, int len){
  checkRange(b, off, len);
  for (int i = off; i < off + len; i++) add(b[i] & 0xff);
 }
 private void add(int x){
  int c = ~crc;
  c = TABLE[(c ^ x) & 0xff] ^ (c >>> 8);
  crc = ~c;
 }
 public void reset(){crc = 0;}
 public long getValue(){return (long) crc & 0xffffffffL;}
 static void checkRange(byte[] b, int off, int len){
  if (b == null) throw new NullPointerException();
  if ((b.length | off | len) < 0 || len > b.length - off)
   throw new ArrayIndexOutOfBoundsException("Range [" + off + ", " + off + " + " + len + ") out of bounds for length " + b.length);
 }
}
