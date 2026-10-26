package dastec.one.javaw.bytecode.linker;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.*;

public class WasmLinker {

    public static String link(String mainWat, List<String> dependencyWats) {
        if (dependencyWats == null || dependencyWats.isEmpty()) {
            return mainWat;
        }

        StringBuilder linked = new StringBuilder();
        int lastClosing = mainWat.lastIndexOf(")");
        if (lastClosing == -1) {
            return mainWat;
        }

        linked.append(mainWat, 0, lastClosing);

        Set<String> seenGlobals = new HashSet<>();
        Set<String> seenFuncs = new HashSet<>();

        for (String depWat : dependencyWats) {
            if (depWat == null || depWat.isBlank()) continue;

            try (BufferedReader reader = new BufferedReader(new StringReader(depWat))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.startsWith("(module") || trimmed.equals(")")) {
                        continue;
                    }
                    if (trimmed.startsWith("(import \"env\" \"memory\"") || trimmed.startsWith("(import \"env\" \"__heap_alloc\"")) {
                        continue;
                    }

                    // Check duplicate global definitions
                    if (trimmed.startsWith("(global $")) {
                        String[] parts = trimmed.split("\\s+");
                        if (parts.length > 1) {
                            String gName = parts[1];
                            if (!seenGlobals.add(gName)) {
                                continue;
                            }
                        }
                    }

                    // Check duplicate function definitions
                    if (trimmed.startsWith("(func $")) {
                        String[] parts = trimmed.split("\\s+");
                        if (parts.length > 1) {
                            String fName = parts[1];
                            if (!seenFuncs.add(fName)) {
                                continue;
                            }
                        }
                    }

                    linked.append("    ").append(trimmed).append("\n");
                }
            } catch (Exception e) {
                // Ignore parsing errors during line iteration
            }
        }

        linked.append(")\n");
        return linked.toString();
    }
}
