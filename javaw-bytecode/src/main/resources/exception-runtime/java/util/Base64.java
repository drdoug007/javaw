package java.util;

public class Base64 {
    private static final String BASIC_ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    private static final String URL_ALPHABET="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    private static final byte[] BASIC_TABLE=encodeTable(BASIC_ALPHABET);
    private static final byte[] URL_TABLE=encodeTable(URL_ALPHABET);
    private static final int[] BASIC_DECODE=decodeTable(BASIC_ALPHABET);
    private static final int[] URL_DECODE=decodeTable(URL_ALPHABET);

    private Base64(){}

    public static Encoder getEncoder(){return Encoder.BASIC;}
    public static Encoder getUrlEncoder(){return Encoder.URL;}
    public static Encoder getMimeEncoder(){return Encoder.MIME;}
    public static Encoder getMimeEncoder(int lineLength,byte[] lineSeparator){
        lineSeparator=Objects.requireNonNull(lineSeparator);
        for(byte b:lineSeparator)
            if(BASIC_DECODE[b&0xff]!=-1)
                throw new IllegalArgumentException("Illegal base64 line separator character 0x"+Integer.toString(b,16));
        lineLength&=~3;
        if(lineLength<=0)return Encoder.BASIC;
        return new Encoder(false,lineSeparator,lineLength,true);
    }
    public static Decoder getDecoder(){return Decoder.BASIC;}
    public static Decoder getUrlDecoder(){return Decoder.URL;}
    public static Decoder getMimeDecoder(){return Decoder.MIME;}

    private static byte[] encodeTable(String alphabet){
        byte[] table=new byte[alphabet.length()];
        for(int i=0;i<table.length;i++)table[i]=(byte)alphabet.charAt(i);
        return table;
    }
    private static int[] decodeTable(String alphabet){
        int[] table=new int[256];
        Arrays.fill(table,-1);
        for(int i=0;i<alphabet.length();i++)table[alphabet.charAt(i)]=i;
        table['=']=-2;
        return table;
    }

    public static class Encoder{
        private final boolean isURL;
        private final byte[] newline;
        private final int linemax;
        private final boolean doPadding;

        private static final Encoder BASIC=new Encoder(false,null,-1,true);
        private static final Encoder URL=new Encoder(true,null,-1,true);
        private static final Encoder MIME=new Encoder(false,new byte[]{'\r','\n'},76,true);

        private Encoder(boolean isURL,byte[] newline,int linemax,boolean doPadding){
            this.isURL=isURL;this.newline=newline;this.linemax=linemax;this.doPadding=doPadding;
        }

        public byte[] encode(byte[] src){
            byte[] out=new byte[encodedOutLength(src.length)];
            int n=encode0(src,out,0);
            if(n!=out.length)out=Arrays.copyOf(out,n);
            return out;
        }

        public int encode(byte[] src,byte[] dst){
            int len=encodedOutLength(src.length);
            if(dst.length<len)throw new IllegalArgumentException("Output byte array is too small for encoding all input bytes");
            return encode0(src,dst,0);
        }

        public String encodeToString(byte[] src){
            byte[] encoded=encode(src);
            return new String(encoded,0,0,encoded.length);
        }

        public Encoder withoutPadding(){
            if(!doPadding)return this;
            return new Encoder(isURL,newline,linemax,false);
        }

        private int encodedOutLength(int n){
            int len=doPadding?4*((n+2)/3):4*(n/3)+(n%3==0?0:n%3+1);
            if(linemax>0)len+=(len-1)/linemax*newline.length;
            return len;
        }

        private int encode0(byte[] src,byte[] dst,int dp){
            byte[] table=isURL?URL_TABLE:BASIC_TABLE;
            int sp=0,length=src.length,linepos=0;
            while(sp+3<=length){
                int i0=src[sp]&0xff,i1=src[sp+1]&0xff,i2=src[sp+2]&0xff;
                dst[dp++]=table[(i0>>2)&0x3f];
                dst[dp++]=table[((i0<<4)|(i1>>4))&0x3f];
                dst[dp++]=table[((i1<<2)|(i2>>6))&0x3f];
                dst[dp++]=table[i2&0x3f];
                sp+=3;linepos+=4;
                if(linemax>0 && linepos>=linemax && sp<length){
                    for(int i=0;i<newline.length;i++)dst[dp++]=newline[i];
                    linepos=0;
                }
            }
            if(sp+1==length){
                int i0=src[sp]&0xff;
                dst[dp++]=table[(i0>>2)&0x3f];
                dst[dp++]=table[(i0<<4)&0x3f];
                if(doPadding){dst[dp++]=(byte)'=';dst[dp++]=(byte)'=';}
            }else if(sp+2==length){
                int i0=src[sp]&0xff,i1=src[sp+1]&0xff;
                dst[dp++]=table[(i0>>2)&0x3f];
                dst[dp++]=table[((i0<<4)|(i1>>4))&0x3f];
                dst[dp++]=table[(i1<<2)&0x3f];
                if(doPadding)dst[dp++]=(byte)'=';
            }
            return dp;
        }
    }

    public static class Decoder{
        private final boolean isURL;
        private final boolean isMIME;

        private static final Decoder BASIC=new Decoder(false,false);
        private static final Decoder URL=new Decoder(true,false);
        private static final Decoder MIME=new Decoder(false,true);

        private Decoder(boolean isURL,boolean isMIME){
            this.isURL=isURL;this.isMIME=isMIME;
        }

        public byte[] decode(byte[] src){
            byte[] out=new byte[decodedOutLength(src)];
            int n=decode0(src,out,0);
            if(n!=out.length)out=Arrays.copyOf(out,n);
            return out;
        }

        public byte[] decode(String src){
            return decode(src.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));
        }

        public int decode(byte[] src,byte[] dst){
            int len=decodedOutLength(src);
            if(dst.length<len)throw new IllegalArgumentException("Output byte array is too small for decoding all input bytes");
            return decode0(src,dst,0);
        }

        private int decodedOutLength(byte[] src){
            int len=src.length;
            if(len==0)return 0;
            int[] base64=isURL?URL_DECODE:BASIC_DECODE;
            if(len==1){
                if(isMIME)return 0;
                throw new IllegalArgumentException("Input byte[] should at least have 2 bytes for base64 bytes");
            }
            int paddings=0;
            if(isMIME){
                int skipped=0;
                for(int i=0;i<src.length;i++){
                    int v=base64[src[i]&0xff];
                    if(v==-2){len=i;break;}
                    if(v==-1)skipped++;
                }
                len-=skipped;
            }else{
                if(base64[src[len-1]&0xff]==-2){
                    paddings++;
                    if(base64[src[len-2]&0xff]==-2)paddings++;
                }
            }
            if(paddings==0 && len%4!=0)paddings=4-len%4;
            return 3*((len+3)/4)-paddings;
        }

        private int decode0(byte[] src,byte[] dst,int dp){
            int[] base64=isURL?URL_DECODE:BASIC_DECODE;
            int bits=0,shiftto=18,sp=0,length=src.length;
            boolean done=false;
            while(sp<length && !done){
                int v=base64[src[sp]&0xff];
                if(v==-2){
                    if(shiftto==18)throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                    if(shiftto==6){
                        if(sp+1>=length || base64[src[sp+1]&0xff]!=-2)
                            throw new IllegalArgumentException("Input byte array has wrong 4-byte ending unit");
                        sp++;
                    }
                    done=true;
                }else if(v==-1){
                    if(!isMIME)throw new IllegalArgumentException("Illegal base64 character "+Integer.toString(src[sp],16));
                }else{
                    bits|=v<<shiftto;
                    shiftto-=6;
                    if(shiftto<0){
                        dst[dp++]=(byte)(bits>>16);
                        dst[dp++]=(byte)(bits>>8);
                        dst[dp++]=(byte)bits;
                        bits=0;
                        shiftto=18;
                    }
                }
                sp++;
            }
            if(shiftto==12)throw new IllegalArgumentException("Last unit does not have enough valid bits");
            if(shiftto==6)dst[dp++]=(byte)(bits>>16);
            else if(shiftto==0){dst[dp++]=(byte)(bits>>16);dst[dp++]=(byte)(bits>>8);}
            while(sp<length){
                // Basic/URL report the leftover byte's own index; MIME reports the index after it.
                if(isMIME && base64[src[sp++]&0xff]<0)continue;
                throw new IllegalArgumentException("Input byte array has incorrect ending byte at "+sp);
            }
            return dp;
        }
    }
}
