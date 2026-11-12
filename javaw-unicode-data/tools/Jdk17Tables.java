import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.lang.reflect.*;
import java.text.BreakIterator;
import java.util.*;
import java.util.zip.ZipFile;

/**
 * Generates the Java 17 / Unicode 13 data the bundled runtime and its tests need, from the running JDK 17's own APIs.
 * Run one table per process with a JDK 17 {@code java} launcher (single-file source mode, no compile step):
 * {@code java [flags] Jdk17Tables.java <table> --output <file>}. {@code GenerateJdk17Data.java} runs all of them
 * with the flags each needs. Output uses {@code \n} line endings and UTF-8 on every platform.
 */
public class Jdk17Tables {
    public static void main(String[] args) throws Exception {
        if (Runtime.version().feature() != 17) throw new IllegalArgumentException("JDK 17 required");
        if (args.length != 3 || !args[1].equals("--output")) throw new IllegalArgumentException("usage: <table> --output <file>");
        Path output = Path.of(args[2]);
        switch (args[0]) {
            case "unicode-case" -> unicodeCase(output);
            case "unicode-properties" -> unicodeProperties(output);
            case "string-uppercase" -> stringUppercase(output);
            case "string-lowercase" -> stringLowercase(output);
            case "character-traversal" -> characterTraversal(output);
            default -> throw new IllegalArgumentException("unknown table " + args[0]);
        }
    }

    /** javaw/runtime/UnicodeCase.java: simple upper/lower case mappings as code-point ranges and deltas. */
    static void unicodeCase(Path output) throws IOException {
        int upperSum = 1, lowerSum = 1, chars = 1;
        for (int cp = -1; cp <= 0x110000; cp++) { upperSum = 31 * upperSum + Character.toUpperCase(cp); lowerSum = 31 * lowerSum + Character.toLowerCase(cp); }
        for (int cp = 0; cp <= 65535; cp++) { chars = 31 * chars + Character.toUpperCase((char) cp); chars = 31 * chars + Character.toLowerCase((char) cp); }
        System.err.println("JDK 17 whole-domain checksums: " + upperSum + " " + lowerSum + " " + chars);
        List<List<int[]>> tables = List.of(new ArrayList<>(), new ArrayList<>());
        for (int kind = 0; kind < 2; kind++) {
            int start = -1, end = -1, delta = 0;
            for (int cp = 0; cp <= 0x110000; cp++) {
                int mapped = cp == 0x110000 ? cp : kind == 0 ? Character.toUpperCase(cp) : Character.toLowerCase(cp);
                int next = mapped - cp;
                if (start >= 0 && (cp != end + 1 || next != delta)) {
                    tables.get(kind).add(new int[]{start, end, delta});
                    start = -1;
                }
                if (next != 0) { if (start < 0) { start = cp; delta = next; } end = cp; }
            }
        }
        StringBuilder out = new StringBuilder("""
            package javaw.runtime;

            /** Generated Java 17 / Unicode 13 simple case mappings. Do not edit by hand.
             * Regenerate with javaw-unicode-data/tools/Jdk17Tables.java and a JDK 17 installation.
             * Each triple is an inclusive code-point range and its signed mapping delta.
             * Ints are packed as two UTF-16 units to avoid a large array-initializer method.
             */
            public final class UnicodeCase {
                private UnicodeCase(){}
            """);
        String[] names = {"UPPER", "LOWER"};
        for (int kind = 0; kind < 2; kind++) {
            List<String> chunks = new ArrayList<>();
            for (int[] row : tables.get(kind)) chunks.add(packed(row));
            out.append("    private static final String ").append(names[kind]).append("=\n");
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < chunks.size(); i += 8)
                lines.add("        \"" + String.join("", chunks.subList(i, Math.min(i + 8, chunks.size()))) + (i + 8 >= chunks.size() ? "\";" : "\" +"));
            out.append(String.join("\n", lines)).append('\n');
        }
        out.append("""
                private static int number(String data,int index){return (data.charAt(index)<<16)|data.charAt(index+1);}
                private static int convert(int code,String ranges){
                    int low=0,high=ranges.length()/6-1;
                    while(low<=high){int middle=(low+high)>>>1,index=middle*6;
                        if(code<number(ranges,index))high=middle-1;
                        else if(code>number(ranges,index+2))low=middle+1;
                        else return code+number(ranges,index+4);
                    }return code;
                }
                public static int upper(int code){return convert(code,UPPER);}
                public static int lower(int code){return convert(code,LOWER);}
            }
            """);
        write(output, out);
        System.out.println("Java 17 case tables: " + tables.get(0).size() + " upper and " + tables.get(1).size() + " lower ranges");
    }

    static int propertyFlags(int c) {
        int f = Character.getType(c);
        if (Character.isLowerCase(c)) f |= 32;
        if (Character.isUpperCase(c)) f |= 64;
        if (Character.isAlphabetic(c)) f |= 128;
        if (Character.isJavaIdentifierStart(c)) f |= 256;
        if (Character.isJavaIdentifierPart(c)) f |= 512;
        if (Character.isUnicodeIdentifierStart(c)) f |= 1024;
        if (Character.isUnicodeIdentifierPart(c)) f |= 2048;
        return f;
    }

    /** javaw/runtime/UnicodeProperties.java: Character property bits and numeric values as code-point ranges. */
    static void unicodeProperties(Path output) throws IOException {
        int bmp = 1;
        for (int c = 0; c <= 65535; c++) { char v = (char) c;
            int f = Character.getType(v); if (Character.isDefined(v)) f |= 32; if (Character.isDigit(v)) f |= 64;
            if (Character.isLetter(v)) f |= 128; if (Character.isLetterOrDigit(v)) f |= 256;
            if (Character.isLowerCase(v)) f |= 512; if (Character.isUpperCase(v)) f |= 1024;
            if (Character.isJavaIdentifierStart(v)) f |= 2048; if (Character.isJavaIdentifierPart(v)) f |= 4096;
            if (Character.isSpaceChar(v)) f |= 8192; if (Character.isISOControl(v)) f |= 16384;
            bmp = 31 * bmp + f; bmp = 31 * bmp + Character.getNumericValue(v); bmp = 31 * bmp + Character.digit(v, 36);
        }
        for (int c : new int[]{-1, Integer.MIN_VALUE, Integer.MAX_VALUE, 0x110000, 0x104a5, 0x1d7d8, 0x216c, 0x00bd, 0x1f600}) {
            for (int r = 0; r <= 38; r++) bmp = 31 * bmp + Character.digit(c, r);
            bmp = 31 * bmp + (Character.isDefined(c) ? 1 : 0) + (Character.isDigit(c) ? 2 : 0) + (Character.isLetter(c) ? 4 : 0)
                    + (Character.isLetterOrDigit(c) ? 8 : 0) + (Character.isSpaceChar(c) ? 16 : 0) + (Character.isISOControl(c) ? 32 : 0);
        }
        System.err.println("JDK 17 BMP/digit checksum: " + bmp);
        int start = 0, previous = propertyFlags(0), number = Character.getNumericValue(0), hash = 1;
        for (int c = -1; c <= 0x110000; c++) { hash = 31 * hash + propertyFlags(c); hash = 31 * hash + Character.getNumericValue(c); }
        System.err.println("JDK 17 properties checksum: " + hash);
        List<int[]> rows = new ArrayList<>();
        for (int c = 1; c <= 0x110000; c++) {
            int f = c == 0x110000 ? -1 : propertyFlags(c), n = Character.getNumericValue(c);
            if (f != previous || n != number) { rows.add(new int[]{start, c - 1, previous, number}); start = c; previous = f; number = n; }
        }
        StringBuilder out = new StringBuilder("""
            package javaw.runtime;

            /** Generated Java 17 / Unicode 13 Character properties. See javaw-unicode-data/tools/Jdk17Tables.java.
             * Range records pack inclusive start/end, property bits and numeric value as UTF-16 int pairs.
             * Chunks avoid the class-file UTF-8 constant size limit and large initializer methods.
             */
            public final class UnicodeProperties {
                private UnicodeProperties(){}
                private static final String[] DATA={
            """);
        for (int chunkStart = 0; chunkStart < rows.size(); chunkStart += 128) {
            List<int[]> chunk = rows.subList(chunkStart, Math.min(chunkStart + 128, rows.size()));
            for (int i = 0; i < chunk.size(); i += 4) {
                StringBuilder packed = new StringBuilder();
                for (int[] row : chunk.subList(i, Math.min(i + 4, chunk.size()))) packed.append(packed(row));
                out.append("        \"").append(packed).append(i + 4 < chunk.size() ? "\"+" : "\",").append('\n');
            }
        }
        out.append("""
                };
                private static int number(int row,int column){String data=DATA[row>>>7];int index=(row&127)*8+column*2;return (data.charAt(index)<<16)|data.charAt(index+1);}
                private static int find(int code){
                    if(code<0 || code>0x10ffff)return -1;
                    int low=0,high=COUNT-1;
                    while(low<=high){int mid=(low+high)>>>1;if(code<number(mid,0))high=mid-1;else if(code>number(mid,1))low=mid+1;else return mid;}
                    return -1;
                }
                public static int flags(int code){int row=find(code);return row<0?0:number(row,2);}
                public static int numeric(int code){int row=find(code);return row<0?-1:number(row,3);}
            }
            """.replace("COUNT", String.valueOf(rows.size())));
        write(output, out);
        System.out.println("Java 17 properties: " + rows.size() + " ranges");
    }

    /** javaw/runtime/StringUpperData.java: full ROOT uppercase expansions, unchanged-copy ranges and combining classes. */
    static void stringUppercase(Path output) throws Exception {
        // sun.text.Normalizer needs --add-exports java.base/sun.text=ALL-UNNAMED; reflection keeps this file compilable without it.
        Method combiningClass = Class.forName("sun.text.Normalizer").getMethod("getCombiningClass", int.class);
        List<int[]> expansions = new ArrayList<>(), ranges = new ArrayList<>();
        List<Integer> identities = new ArrayList<>();
        for (int cp = 0; cp <= 0x10ffff; cp++) {
            String s = new String(Character.toChars(cp)), u = s.toUpperCase(Locale.ROOT);
            if (!u.equals(new String(Character.toChars(Character.toUpperCase(cp))))) {
                int[] row = new int[u.length() + 1]; row[0] = cp;
                for (int i = 0; i < u.length(); i++) row[i + 1] = u.charAt(i);
                expansions.add(row);
            }
            if (u.equals(s) && u != s) identities.add(cp);
            int cc = (int) combiningClass.invoke(null, cp);
            if (cc != 0) {
                int[] last = ranges.isEmpty() ? null : ranges.get(ranges.size() - 1);
                if (last != null && last[1] + 1 == cp && last[2] == cc) last[1] = cp;
                else ranges.add(new int[]{cp, cp, cc});
            }
        }
        List<int[]> identityRanges = new ArrayList<>();
        for (int cp : identities) {
            int[] last = identityRanges.isEmpty() ? null : identityRanges.get(identityRanges.size() - 1);
            if (last != null && last[1] + 1 == cp) last[1] = cp; else identityRanges.add(new int[]{cp, cp});
        }
        StringBuilder out = new StringBuilder("""
            package javaw.runtime;
            /** Generated from Java 17 / Unicode 13. See javaw-unicode-data/tools/Jdk17Tables.java. */
            public final class StringUpperData {
             private StringUpperData(){}
            """);
        List<String> tests = new ArrayList<>();
        for (int[] range : identityRanges) tests.add("(cp>=" + range[0] + " && cp<=" + range[1] + ")");
        out.append(" public static boolean copiesUnchanged(int cp){return ").append(tests.isEmpty() ? "false" : String.join(" || ", tests)).append(";}\n");
        List<String> keys = new ArrayList<>(), values = new ArrayList<>(), classes = new ArrayList<>();
        for (int[] row : expansions) {
            keys.add(String.valueOf(row[0]));
            StringBuilder value = new StringBuilder("\"");
            for (int i = 1; i < row.length; i++) value.append(unit(row[i]));
            values.add(value.append('"').toString());
        }
        for (int[] row : ranges) for (int v : row) classes.add(String.valueOf(v));
        out.append(" private static final int[] KEYS={").append(String.join(",", keys)).append("};\n");
        out.append(" private static final String[] VALUES={").append(String.join(",", values)).append("};\n");
        out.append(" private static final int[] CLASSES={").append(String.join(",", classes)).append("};\n");
        out.append("""
             public static String expansion(int cp){int low=0,high=KEYS.length-1;while(low<=high){int m=(low+high)>>>1;if(cp<KEYS[m])high=m-1;else if(cp>KEYS[m])low=m+1;else return VALUES[m];}return null;}
             public static int combining(int cp){int low=0,high=CLASSES.length/3-1;while(low<=high){int m=(low+high)>>>1,i=m*3;if(cp<CLASSES[i])high=m-1;else if(cp>CLASSES[i+1])low=m+1;else return CLASSES[i+2];}return 0;}
            }
            """);
        write(output, out);
        System.out.println(expansions.size() + " expansions, " + ranges.size() + " combining-class ranges");
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(owner);
    }

    /**
     * javaw/runtime/StringLowerData.java: the JDK 17 ROOT word-break tables and cased ranges for String lowercase
     * contexts. Needs {@code --add-opens} for java.base/sun.text and java.base/java.lang, and the CLDR locale provider.
     */
    static void stringLowercase(Path output) throws Exception {
        BreakIterator b = BreakIterator.getWordInstance(Locale.ROOT);
        Method category = b.getClass().getDeclaredMethod("lookupCategory", int.class); category.setAccessible(true);
        int categories = (int) field(b, "numCategories");
        short[] states = (short[]) field(b, "stateTable"), back = (short[]) field(b, "backwardsStateTable");
        boolean[] end = (boolean[]) field(b, "endStates"), look = (boolean[]) field(b, "lookaheadStates");
        int[] flags = new int[end.length];
        for (int i = 0; i < end.length; i++) flags[i] = (end[i] ? 1 : 0) | (look[i] ? 2 : 0);
        for (String lang : new String[]{"en", "de", "fr", "it", "ja", "zh", "ko", "tr", "az", "lt", "ar", "ru", "el", "lo"}) {
            BreakIterator other = BreakIterator.getWordInstance(new Locale(lang));
            if (other.getClass() != b.getClass() || !field(other, "checksum").equals(field(b, "checksum")))
                throw new IllegalArgumentException("Different word data: " + lang);
        }
        Method isCased = Class.forName("java.lang.ConditionalSpecialCasing").getDeclaredMethod("isCased", int.class); isCased.setAccessible(true);
        List<int[]> cased = new ArrayList<>(), rows = new ArrayList<>();
        int caseStart = -1;
        for (int cp = 0; cp <= 0x110000; cp++) {
            boolean yes = cp < 0x110000 && (boolean) isCased.invoke(null, cp);
            if (yes && caseStart < 0) caseStart = cp;
            else if (!yes && caseStart >= 0) { cased.add(new int[]{caseStart, cp - 1}); caseStart = -1; }
        }
        int start = 0, previous = (int) category.invoke(b, 0);
        for (int cp = 1; cp <= 0x110000; cp++) {
            int next = cp == 0x110000 ? -2 : (int) category.invoke(b, cp);
            if (next != previous) { rows.add(new int[]{start, cp - 1, previous}); start = cp; previous = next; }
        }
        String rules;
        try (ZipFile sources = new ZipFile(Path.of(System.getProperty("java.home"), "lib", "src.zip").toFile())) {
            rules = new String(sources.getInputStream(sources.getEntry("java.base/sun/text/resources/BreakIteratorRules.java")).readAllBytes(), StandardCharsets.UTF_8);
        }
        StringBuilder out = new StringBuilder(rules.substring(0, rules.indexOf("package ")));
        out.append("""
            package javaw.runtime;
            /** Generated Java 17 ROOT word-break tables. See javaw-unicode-data/tools/Jdk17Tables.java
             * and legal/word-break for upstream source, license and reproduction notes.
             * Each category range packs inclusive start, end and category as UTF-16 int pairs.
             */
            public final class StringLowerData {
             private StringLowerData(){}
            """);
        StringBuilder casedText = new StringBuilder();
        for (int[] row : cased) casedText.append(packed(row));
        out.append(" private static final String CASED=\"").append(casedText).append("\";\n");
        out.append(" private static final String STATES=\"").append(units(states)).append("\";\n");
        out.append(" private static final String BACK=\"").append(units(back)).append("\";\n");
        StringBuilder flagText = new StringBuilder();
        for (int flag : flags) flagText.append(unit(flag));
        out.append(" private static final String FLAGS=\"").append(flagText).append("\";\n");
        out.append(" private static final String[] RANGES={\n");
        for (int i = 0; i < rows.size(); i += 128) {
            StringBuilder packed = new StringBuilder();
            for (int[] row : rows.subList(i, Math.min(i + 128, rows.size()))) packed.append(packed(row));
            out.append("  \"").append(packed).append("\",\n");
        }
        out.append("""
             };
             private static int number(int row,int column){String s=RANGES[row>>>7];int i=(row&127)*6+column*2;return(s.charAt(i)<<16)|s.charAt(i+1);}
             public static boolean cased(int cp){int low=0,high=CASED.length()/4-1;while(low<=high){int m=(low+high)>>>1,i=m*4;int start=(CASED.charAt(i)<<16)|CASED.charAt(i+1),end=(CASED.charAt(i+2)<<16)|CASED.charAt(i+3);if(cp<start)high=m-1;else if(cp>end)low=m+1;else return true;}return false;}
             public static int category(int cp){int low=0,high=COUNT-1;while(low<=high){int m=(low+high)>>>1;if(cp<number(m,0))high=m-1;else if(cp>number(m,1))low=m+1;else return number(m,2);}return -1;}
             public static int state(int state,int category){return STATES.charAt(state*CATEGORIES+category);}
             public static int backward(int state,int category){return BACK.charAt(state*CATEGORIES+category);}
             public static boolean end(int state){return(FLAGS.charAt(state)&1)!=0;}
             public static boolean look(int state){return(FLAGS.charAt(state)&2)!=0;}
            }
            """.replace("COUNT", String.valueOf(rows.size())).replace("CATEGORIES", String.valueOf(categories)));
        write(output, out);
        System.out.println(rows.size() + " category ranges, " + states.length + " transitions, " + flags.length + " states");
    }

    private static String units(short[] values) {
        StringBuilder out = new StringBuilder();
        for (short value : values) out.append(unit(value));
        return out.toString();
    }

    private static int probe(int kind, int index, int end) {
        char[] text = {'a', 0xd801, 0xdc28, 0xd800, 'x', 0xdc00, 0xd800, 0xd800, 0xdc00}; String s = new String(text);
        try {
            switch (kind) {
                case 0: return Character.codePointAt(text, index, end);
                case 1: return Character.codePointBefore(text, index, end);
                case 2: return Character.codePointCount(text, index, end);
                case 3: return Character.codePointCount(s, index, end);
                case 4: return Character.codePointAt(text, index);
                case 5: return Character.codePointBefore(text, index);
                case 6: return Character.codePointAt(s, index);
                case 7: return Character.codePointBefore(s, index);
                case 8: return Character.codePointAt((char[]) null, index, end);
                case 9: return Character.codePointBefore((char[]) null, index, end);
                case 10: return Character.codePointCount((CharSequence) null, index, end);
                case 11: return Character.codePointAt((char[]) null, index);
                case 12: return Character.codePointBefore((char[]) null, index);
                default: throw new AssertionError();
            }
        } catch (NullPointerException e) { return -1; }
        catch (ArrayIndexOutOfBoundsException e) { return -2; }
        catch (StringIndexOutOfBoundsException e) { return -3; }
        catch (IndexOutOfBoundsException e) { return -4; }
    }

    /** character-traversal-jdk17.txt, the test oracle: Character code-point traversal results and failures on JDK 17. */
    static void characterTraversal(Path output) throws IOException {
        StringBuilder out = new StringBuilder();
        int cases = 0;
        for (int kind = 0; kind < 13; kind++) for (int i = -1; i <= 10; i++) for (int end = -1; end <= 10; end++) {
            out.append(kind).append(' ').append(i).append(' ').append(end).append(' ').append(probe(kind, i, end)).append('\n');
            cases++;
        }
        write(output, out);
        System.out.println("Java 17 Character traversal oracle: " + cases + " cases");
    }

    /** A UTF-16 unit as a Java string escape: three-digit octal below 256, otherwise lowercase {@code \\uXXXX}. */
    static String unit(int value) {
        if (value < 0) throw new IllegalArgumentException("negative unit " + value);
        return value < 256 ? String.format("\\%03o", value) : String.format("\\u%04x", value);
    }

    /** Each int as its high and low UTF-16 units. */
    static String packed(int... values) {
        StringBuilder out = new StringBuilder();
        for (int value : values) out.append(unit((value >> 16) & 65535)).append(unit(value & 65535));
        return out.toString();
    }

    static void write(Path output, CharSequence text) throws IOException {
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output, text, StandardCharsets.UTF_8);
    }
}
