package dastec.one.javaw.sourcemap;

import java.util.ArrayList;
import java.util.List;

/**
 * Base64 Variable-Length Quantity (VLQ) encoder and decoder for Source Map v3.
 */
public class Base64VLQ {
    private static final String BASE64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    public static String encode(int value) {
        StringBuilder sb = new StringBuilder();
        int vlq = value < 0 ? ((-value << 1) | 1) : (value << 1);
        do {
            int digit = vlq & 0x1F;
            vlq >>>= 5;
            if (vlq > 0) {
                digit |= 0x20; // continuation bit
            }
            sb.append(BASE64_CHARS.charAt(digit));
        } while (vlq > 0);
        return sb.toString();
    }

    public static List<Integer> decode(String str) {
        List<Integer> result = new ArrayList<>();
        int value = 0;
        int shift = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int digit = BASE64_CHARS.indexOf(c);
            if (digit < 0) {
                throw new IllegalArgumentException("Invalid Base64 character: " + c);
            }
            boolean hasContinuation = (digit & 0x20) != 0;
            digit &= 0x1F;
            value += digit << shift;
            if (hasContinuation) {
                shift += 5;
            } else {
                boolean isNegative = (value & 1) == 1;
                value >>>= 1;
                result.add(isNegative ? -value : value);
                value = 0;
                shift = 0;
            }
        }
        return result;
    }
}
