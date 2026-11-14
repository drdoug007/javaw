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
        if (algorithm.equalsIgnoreCase("SHA-1") || algorithm.equalsIgnoreCase("SHA1")
                || algorithm.equalsIgnoreCase("SHA")
                || algorithm.equalsIgnoreCase("1.3.14.3.2.26")
                || algorithm.equalsIgnoreCase("OID.1.3.14.3.2.26")) {
            return new Sha1(algorithm);
        }
        if (algorithm.equalsIgnoreCase("MD5")) {
            return new Md5(algorithm);
        }
        if (algorithm.equalsIgnoreCase("SHA-512") || algorithm.equalsIgnoreCase("SHA512")
                || algorithm.equalsIgnoreCase("2.16.840.1.101.3.4.2.3")
                || algorithm.equalsIgnoreCase("OID.2.16.840.1.101.3.4.2.3")) {
            return Sha512.sha512(algorithm);
        }
        if (algorithm.equalsIgnoreCase("SHA-384") || algorithm.equalsIgnoreCase("SHA384")
                || algorithm.equalsIgnoreCase("2.16.840.1.101.3.4.2.2")
                || algorithm.equalsIgnoreCase("OID.2.16.840.1.101.3.4.2.2")) {
            return Sha512.sha384(algorithm);
        }
        return null;
    }
}
