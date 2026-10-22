package com.example.javaw.bytecode.linker;

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

        linked.append(mainWat.substring(0, lastClosing));

        Set<String> seenGlobals = new HashSet<>();
        Set<String> seenFuncs = new HashSet<>();

        for (String depWat : dependencyWats) {
            if (depWat == null || depWat.isBlank()) continue;

            String[] lines = depWat.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("(module") || trimmed.equals(")")) {
                    continue;
                }
                if (trimmed.startsWith("(import \"env\" \"memory\"") || trimmed.startsWith("(import \"env\" \"__heap_alloc\"")) {
                    continue;
                }

                // Check duplicate global definitions
                if (trimmed.startsWith("(global $")) {
                    String gName = trimmed.split("\\s+")[1];
                    if (!seenGlobals.add(gName)) {
                        continue;
                    }
                }

                // Check duplicate function definitions
                if (trimmed.startsWith("(func $")) {
                    String fName = trimmed.split("\\s+")[1];
                    if (!seenFuncs.add(fName)) {
                        continue;
                    }
                }

                linked.append("    ").append(trimmed).append("\n");
            }
        }

        linked.append(")\n");
        return linked.toString();
    }
}
