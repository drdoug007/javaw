package one.dastec.javaw.bytecode;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.regex.*;

import static org.junit.jupiter.api.Assertions.*;

/** The reference scanner must find exactly what the regex it replaced finds, in the same order. */
class BytecodeDeadCodeScanTest {
    private static final Pattern REFERENCE = Pattern.compile("(?:call|return_call|ref\\.func) (\\$[^\\s()]+)");
    private static final Pattern GLOBAL_REFERENCE = Pattern.compile("(?:global\\.get|global\\.set|\\(global) (\\$[^\\s()]+)");
    private static final String[] TOKENS = {"call", "return_call", "ref.func", "global.get", "global.set", "(global", "call_indirect",
            "x", "$", "$a", "$f_1", "call$", " ", "  ", "\t", "\n", "\u000B", "\f", "\r", "(", ")", "\"", ";", "é", " "};

    @Test
    void matchesTheRegexInEveryRegion() {
        Random random = new Random(20261102);
        for (int round = 0; round < 20_000; round++) {
            StringBuilder text = new StringBuilder();
            for (int i = random.nextInt(24); i > 0; i--) text.append(TOKENS[random.nextInt(TOKENS.length)]);
            int from = random.nextInt(text.length() + 1), to = from + random.nextInt(text.length() - from + 1);
            check(text.toString(), from, to);
        }
    }

    @Test
    void handlesOverlapAndBoundaryCases() {
        for (String text : List.of("call $a", "return_call $a", "ref.func $a call $b", "call $", "call $ call $b", "call  $a",
                "xcall $a", "call $acall $b", "call $a(call $b)", "return_call $a return_call $", "(global $g global.get $g",
                "global.set $g$h", "call $a\u000Bcall $b", "call $a x", "$foocall $bar", "call $é"))
            for (int from = 0; from <= text.length(); from++)
                for (int to = from; to <= text.length(); to++) check(text, from, to);
    }

    private static void check(String text, int from, int to) {
        assertEquals(regex(REFERENCE, text, from, to, ""), scan(BytecodeDeadCode.REFERENCE, text, from, to, ""), text + " [" + from + "," + to + ")");
        assertEquals(regex(GLOBAL_REFERENCE, text, from, to, "global "), scan(BytecodeDeadCode.GLOBAL_REFERENCE, text, from, to, "global "),
                text + " [" + from + "," + to + ")");
    }

    private static List<String> scan(String[] prefixes, String text, int from, int to, String key) {
        List<String> result = new ArrayList<>();
        BytecodeDeadCode.scan(text, from, to, prefixes, key, result);
        return result;
    }

    private static List<String> regex(Pattern pattern, String text, int from, int to, String key) {
        List<String> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(text).region(from, to);
        while (matcher.find()) result.add(key + matcher.group(1));
        return result;
    }
}
