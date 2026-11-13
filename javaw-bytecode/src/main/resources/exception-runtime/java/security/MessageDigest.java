package java.security;

import javaw.runtime.Digests;
import java.util.Objects;

public abstract class MessageDigest extends MessageDigestSpi {
    private final String algorithm;
    private String providerName;
    private boolean inProgress;

    protected MessageDigest(String algorithm) {
        this.algorithm = algorithm;
    }

    public static MessageDigest getInstance(String algorithm) throws NoSuchAlgorithmException {
        Objects.requireNonNull(algorithm,"null algorithm name");
        MessageDigest digest = Digests.create(algorithm);
        if (digest == null) {
            throw new NoSuchAlgorithmException(algorithm + " MessageDigest not available");
        }
        digest.providerName = "SUN";
        return digest;
    }

    public final String getAlgorithm(){return algorithm;}

    public void update(byte input) {
        engineUpdate(input);
        inProgress = true;
    }

    public void update(byte[] input) {
        engineUpdate(input,0,input.length);
        inProgress = true;
    }

    public void update(byte[] input,int offset,int len) {
        if (input == null) {
            throw new IllegalArgumentException("No input buffer given");
        }
        if (input.length - offset < len) {
            throw new IllegalArgumentException("Input buffer too short");
        }
        engineUpdate(input,offset,len);
        inProgress = true;
    }

    public byte[] digest() {
        // As in the JDK, resetting is the engine's job: engineDigest() resets, MessageDigest does not.
        byte[] result = engineDigest();
        inProgress = false;
        return result;
    }

    public byte[] digest(byte[] input) {
        update(input);
        return digest();
    }

    public int digest(byte[] buf,int offset,int len) throws DigestException {
        if (buf == null) {
            throw new IllegalArgumentException("No output buffer given");
        }
        if (buf.length - offset < len) {
            throw new IllegalArgumentException("Output buffer too small for specified offset and length");
        }
        int count = engineDigest(buf,offset,len);
        inProgress = false;
        return count;
    }

    public void reset() {
        engineReset();
        inProgress = false;
    }

    public final int getDigestLength() {
        int length = engineGetDigestLength();
        if (length == 0) {
            try {
                MessageDigest copy = (MessageDigest) clone();
                length = copy.digest().length;
            } catch (CloneNotSupportedException e) {
                length = 0;
            }
        }
        return length;
    }

    public static boolean isEqual(byte[] digesta,byte[] digestb) {
        if (digesta == digestb) {
            return true;
        }
        if (digesta == null || digestb == null || digesta.length != digestb.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < digesta.length; i++) {
            result |= digesta[i] ^ digestb[i];
        }
        return result == 0;
    }

    @Override
    public String toString() {
        return algorithm + " Message Digest from " + (providerName == null ? "(no provider)" : providerName)
                + ", " + (inProgress ? "<in progress>" : "<initialized>") + "\n";
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        if (this instanceof Cloneable) {
            return super.clone();
        }
        throw new CloneNotSupportedException();
    }
}
