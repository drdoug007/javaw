package javax.crypto;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javaw.runtime.Digests;

public class Mac implements Cloneable {
    private final String algorithm;
    private final MessageDigest digest;
    private final int blockLength;
    private byte[] innerPad;
    private byte[] outerPad;
    private boolean initialized;
    // The JDK picks its provider on the first getMacLength/reset/update/doFinal or successful init; until
    // then init(null) reports the provider-selection message instead of "Secret key expected".
    private boolean engineChosen;
    // As in the JDK's HmacCore, true until data reaches the inner digest after init, doFinal or reset.
    private boolean first = true;

    private Mac(String algorithm,MessageDigest digest,int blockLength) {
        this.algorithm = algorithm;
        this.digest = digest;
        this.blockLength = blockLength;
    }

    public static final Mac getInstance(String algorithm) throws NoSuchAlgorithmException {
        if (algorithm == null) {
            throw new NullPointerException("null algorithm name");
        }
        // Fixed table over the built-in digests; the caller's spelling is kept.
        if (algorithm.equalsIgnoreCase("HmacSHA256")) {
            return new Mac(algorithm,Digests.create("SHA-256"),64);
        }
        if (algorithm.equalsIgnoreCase("HmacSHA1")) {
            return new Mac(algorithm,Digests.create("SHA-1"),64);
        }
        if (algorithm.equalsIgnoreCase("HmacMD5")) {
            return new Mac(algorithm,Digests.create("MD5"),64);
        }
        if (algorithm.equalsIgnoreCase("HmacSHA384")) {
            return new Mac(algorithm,Digests.create("SHA-384"),128);
        }
        if (algorithm.equalsIgnoreCase("HmacSHA512")) {
            return new Mac(algorithm,Digests.create("SHA-512"),128);
        }
        throw new NoSuchAlgorithmException("Algorithm " + algorithm + " not available");
    }

    public final String getAlgorithm(){return algorithm;}

    public final int getMacLength() {
        engineChosen = true;
        return digest.getDigestLength();
    }

    public final void init(Key key) throws InvalidKeyException {
        if (key == null && !engineChosen) {
            throw new InvalidKeyException("No installed provider supports this key: (null)");
        }
        if (!(key instanceof SecretKey)) {
            throw new InvalidKeyException("Secret key expected");
        }
        byte[] secret = key.getEncoded();
        if (secret == null) {
            throw new InvalidKeyException("Missing key data");
        }
        // RFC 2104: a key longer than the block is replaced by its digest; a key of
        // exactly blockLength is not. Like the JDK, hash it on top of any data still pending
        // since the last init/doFinal/reset (the JDK's digest holds it; ours also holds the
        // eagerly added inner pad, which the JDK adds on the first update).
        if (secret.length > blockLength) {
            if (first) {
                digest.reset();
            }
            byte[] hashed = digest.digest(secret);
            zero(secret);
            secret = hashed;
        }
        innerPad = new byte[blockLength];
        outerPad = new byte[blockLength];
        for (int i = 0; i < blockLength; i++) {
            int k = i < secret.length ? secret[i] : 0;
            innerPad[i] = (byte)(k ^ 0x36);
            outerPad[i] = (byte)(k ^ 0x5c);
        }
        // The JDK erases the key bytes it was given.
        zero(secret);
        restart();
        initialized = true;
        engineChosen = true;
    }

    private static void zero(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = 0;
        }
    }

    public final void update(byte input) {
        engineChosen = true;
        if (!initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        first = false;
        digest.update(input);
    }

    public final void update(byte[] input) {
        engineChosen = true;
        if (!initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        if (input != null) {
            first = false;
            digest.update(input);
        }
    }

    public final void update(byte[] input,int offset,int len) {
        engineChosen = true;
        if (!initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        if (input == null) {
            return;
        }
        if (offset < 0 || len < 0 || input.length - offset < len) {
            throw new IllegalArgumentException("Bad arguments");
        }
        first = false;
        digest.update(input,offset,len);
    }

    public final byte[] doFinal() {
        engineChosen = true;
        if (!initialized) {
            throw new IllegalStateException("MAC not initialized");
        }
        byte[] inner = digest.digest();
        digest.update(outerPad);
        digest.update(inner);
        byte[] result = digest.digest();
        restart();
        return result;
    }

    public final byte[] doFinal(byte[] input) {
        update(input);
        return doFinal();
    }

    public final void reset() {
        engineChosen = true;
        if (initialized) {
            restart();
        }
    }

    // MAC = H(opad || H(ipad || message)): reload the digest so the Mac keeps its key.
    private void restart() {
        first = true;
        digest.reset();
        digest.update(innerPad);
    }
}
