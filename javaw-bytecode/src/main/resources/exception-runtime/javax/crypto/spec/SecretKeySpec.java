package javax.crypto.spec;

import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Locale;
import javax.crypto.SecretKey;

public final class SecretKeySpec implements KeySpec, SecretKey {
    private final byte[] key;
    private final String algorithm;

    public SecretKeySpec(byte[] key,String algorithm) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("Empty key");
        }
        this.key = key.clone();
        this.algorithm = algorithm;
    }

    public SecretKeySpec(byte[] key,int offset,int len,String algorithm) {
        if (key == null || algorithm == null) {
            throw new IllegalArgumentException("Missing argument");
        }
        if (key.length == 0) {
            throw new IllegalArgumentException("Empty key");
        }
        if (offset < 0) {
            throw new ArrayIndexOutOfBoundsException("offset is negative");
        }
        if (len < 0) {
            throw new ArrayIndexOutOfBoundsException("len is negative");
        }
        if (key.length - offset < len) {
            throw new IllegalArgumentException("Invalid offset/length combination");
        }
        this.key = Arrays.copyOfRange(key,offset,offset + len);
        this.algorithm = algorithm;
    }

    public String getAlgorithm(){return algorithm;}

    public String getFormat(){return "RAW";}

    public byte[] getEncoded(){return key.clone();}

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SecretKey)) {
            return false;
        }
        // Same order as the JDK, so a key whose getAlgorithm() is null throws NullPointerException.
        String thatAlgorithm = ((SecretKey) obj).getAlgorithm();
        if (!thatAlgorithm.equalsIgnoreCase(algorithm)) {
            // As in the JDK, "DESede" and "TripleDES" name the same algorithm in either order.
            if ((!thatAlgorithm.equalsIgnoreCase("DESede") || !algorithm.equalsIgnoreCase("TripleDES"))
                    && (!thatAlgorithm.equalsIgnoreCase("TripleDES") || !algorithm.equalsIgnoreCase("DESede"))) {
                return false;
            }
        }
        byte[] thatKey = ((SecretKey) obj).getEncoded();
        try {
            return MessageDigest.isEqual(key,thatKey);
        } finally {
            // The JDK erases the other key's encoded bytes.
            if (thatKey != null) {
                for (int i = 0; i < thatKey.length; i++) {
                    thatKey[i] = 0;
                }
            }
        }
    }

    @Override
    public int hashCode() {
        int v = 0;
        for (int i = 1; i < key.length; i++) {
            v += key[i] * i;
        }
        // JDK 21 formula: "TripleDES" hashes as "desede" for 1.1 compatibility.
        return v ^ (algorithm.equalsIgnoreCase("TripleDES") ? "desede" : algorithm.toLowerCase(Locale.ENGLISH)).hashCode();
    }
}
