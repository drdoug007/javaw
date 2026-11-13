package java.util;

/**
 * Single-threaded StringTokenizer for the bundled runtime. Delimiters are matched one UTF-16 char at a time;
 * supplementary-character (surrogate pair) delimiters are not supported.
 */
public class StringTokenizer implements Enumeration<Object> {
    private final String str;
    private String delimiters;
    private final boolean returnDelims;
    private int position;

    public StringTokenizer(String str) {
        this(str, " \t\n\r\f", false);
    }

    public StringTokenizer(String str, String delim) {
        this(str, delim, false);
    }

    public StringTokenizer(String str, String delim, boolean returnDelims) {
        if (str == null) {
            throw new NullPointerException();
        }
        this.str = str;
        this.delimiters = delim;
        this.returnDelims = returnDelims;
    }

    public boolean hasMoreTokens() {
        return skipDelimiters(position) < str.length();
    }

    public String nextToken() {
        int start = skipDelimiters(position);
        if (start >= str.length()) {
            throw new NoSuchElementException();
        }
        position = scanToken(start);
        return str.substring(start, position);
    }

    public String nextToken(String delim) {
        delimiters = delim;
        return nextToken();
    }

    public boolean hasMoreElements() {
        return hasMoreTokens();
    }

    public Object nextElement() {
        return nextToken();
    }

    public int countTokens() {
        int count = 0;
        int index = position;
        while (index < str.length()) {
            index = skipDelimiters(index);
            if (index >= str.length()) {
                break;
            }
            index = scanToken(index);
            count++;
        }
        return count;
    }

    /** Moves past delimiters, unless they are returned as tokens. */
    private int skipDelimiters(int start) {
        if (delimiters == null) {
            throw new NullPointerException();
        }
        while (!returnDelims && start < str.length() && delimiters.indexOf(str.charAt(start)) >= 0) {
            start++;
        }
        return start;
    }

    /** Moves past one token; with returnDelims, a delimiter on its own is a one-character token. */
    private int scanToken(int start) {
        int index = start;
        while (index < str.length() && delimiters.indexOf(str.charAt(index)) < 0) {
            index++;
        }
        if (returnDelims && index == start && start < str.length()) {
            index++;
        }
        return index;
    }
}
