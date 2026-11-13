package javaw.runtime;

import java.security.MessageDigest;

/** The fixed built-in algorithm table. Names are case-insensitive; the caller's spelling is kept. */
public final class Digests {
    private Digests(){}

    public static MessageDigest create(String algorithm) {
        if (algorithm.equalsIgnoreCase("SHA-256") || algorithm.equalsIgnoreCase("SHA256")
                || algorithm.equalsIgnoreCase("2.16.840.1.101.3.4.2.1")
                || algorithm.equalsIgnoreCase("OID.2.16.840.1.101.3.4.2.1")) {
            return new Sha256(algorithm);
        }
        return null;
    }
}
