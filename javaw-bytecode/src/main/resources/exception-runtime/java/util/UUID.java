package java.util;

/** Immutable bit-pair API required by H2 Bits; no parsing, entropy or text API. */
public final class UUID implements java.io.Serializable,Comparable<UUID> {
    private final long most,least;
    public UUID(long most,long least){this.most=most;this.least=least;}
    public long getMostSignificantBits(){return most;}
    public long getLeastSignificantBits(){return least;}
    public boolean equals(Object other){return other instanceof UUID && most==((UUID)other).most && least==((UUID)other).least;}
    public int hashCode(){long bits=most^least;return (int)(bits^(bits>>>32));}
    public int compareTo(UUID other){return most==other.most?(least==other.least?0:least<other.least?-1:1):most<other.most?-1:1;}
}
