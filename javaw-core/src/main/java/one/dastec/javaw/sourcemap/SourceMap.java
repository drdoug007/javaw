package one.dastec.javaw.sourcemap;

import java.util.*;

/**
 * Parsed representation of a Source Map v3, allowing querying of original source positions
 * from generated WebAssembly / WAT positions.
 */
public class SourceMap {
    private final int version;
    private final String file;
    private final List<String> sources;
    private final List<String> sourcesContent;
    private final List<String> names;
    private final List<MappingEntry> mappings;
    private final Map<Integer, List<MappingEntry>> lineMappings; // 0-based genLine -> entries

    public SourceMap(int version, String file, List<String> sources, List<String> sourcesContent,
                     List<String> names, List<MappingEntry> mappings) {
        this.version = version;
        this.file = file;
        this.sources = sources != null ? sources : Collections.emptyList();
        this.sourcesContent = sourcesContent != null ? sourcesContent : Collections.emptyList();
        this.names = names != null ? names : Collections.emptyList();
        this.mappings = mappings != null ? mappings : Collections.emptyList();

        Map<Integer, List<MappingEntry>> map = new HashMap<>();
        for (MappingEntry entry : this.mappings) {
            map.computeIfAbsent(entry.getGeneratedLine(), k -> new ArrayList<>()).add(entry);
        }
        for (List<MappingEntry> list : map.values()) {
            list.sort(Comparator.comparingInt(MappingEntry::getGeneratedColumn));
        }
        this.lineMappings = map;
    }

    public int getVersion() {
        return version;
    }

    public String getFile() {
        return file;
    }

    public List<String> getSources() {
        return sources;
    }

    public List<String> getSourcesContent() {
        return sourcesContent;
    }

    public List<String> getNames() {
        return names;
    }

    public List<MappingEntry> getMappings() {
        return mappings;
    }

    /**
     * Finds the closest matching source mapping for a given generated line and column (0-based).
     */
    public MappingEntry findMapping(int genLine, int genCol) {
        List<MappingEntry> list = lineMappings.get(genLine);
        if (list == null || list.isEmpty()) {
            return null;
        }
        MappingEntry best = list.get(0);
        for (MappingEntry entry : list) {
            if (entry.getGeneratedColumn() <= genCol) {
                best = entry;
            } else {
                break;
            }
        }
        return best;
    }

    /**
     * Parses a Source Map v3 JSON string.
     */
    public static SourceMap parse(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty source map JSON");
        }

        int version = 3;
        String file = "";
        List<String> sources = new ArrayList<>();
        List<String> sourcesContent = new ArrayList<>();
        List<String> names = new ArrayList<>();
        String mappingsStr = "";

        // Minimal robust JSON parser for standard source map fields
        int vIdx = json.indexOf("\"version\":");
        if (vIdx >= 0) {
            int end = findValueEnd(json, vIdx + 10);
            try {
                version = Integer.parseInt(json.substring(vIdx + 10, end).trim());
            } catch (Exception ignored) {
            }
        }

        file = extractStringField(json, "file");
        sources = extractStringArrayField(json, "sources");
        sourcesContent = extractStringArrayField(json, "sourcesContent");
        names = extractStringArrayField(json, "names");
        mappingsStr = extractStringField(json, "mappings");

        List<MappingEntry> mappings = decodeMappings(mappingsStr, sources, names);

        return new SourceMap(version, file, sources, sourcesContent, names, mappings);
    }

    private static List<MappingEntry> decodeMappings(String mappingsStr, List<String> sources, List<String> names) {
        List<MappingEntry> result = new ArrayList<>();
        if (mappingsStr == null || mappingsStr.isEmpty()) return result;

        String[] lines = mappingsStr.split(";", -1);
        int prevSourceIdx = 0;
        int prevOrigLine = 0;
        int prevOrigCol = 0;
        int prevNameIdx = 0;

        for (int genLine = 0; genLine < lines.length; genLine++) {
            String line = lines[genLine];
            if (line.isEmpty()) continue;

            String[] segments = line.split(",", -1);
            int prevGenCol = 0;

            for (String segment : segments) {
                if (segment.isEmpty()) continue;
                List<Integer> values = Base64VLQ.decode(segment);
                if (values.isEmpty()) continue;

                int genCol = prevGenCol + values.get(0);
                prevGenCol = genCol;

                String sourceFile = null;
                int origLine = -1;
                int origCol = -1;
                String name = null;

                if (values.size() >= 4) {
                    int sourceIdx = prevSourceIdx + values.get(1);
                    prevSourceIdx = sourceIdx;
                    origLine = prevOrigLine + values.get(2);
                    prevOrigLine = origLine;
                    origCol = prevOrigCol + values.get(3);
                    prevOrigCol = origCol;

                    if (sourceIdx >= 0 && sourceIdx < sources.size()) {
                        sourceFile = sources.get(sourceIdx);
                    }
                }

                if (values.size() >= 5) {
                    int nameIdx = prevNameIdx + values.get(4);
                    prevNameIdx = nameIdx;
                    if (nameIdx >= 0 && nameIdx < names.size()) {
                        name = names.get(nameIdx);
                    }
                }

                result.add(new MappingEntry(genLine, genCol, sourceFile, origLine, origCol, name));
            }
        }
        return result;
    }

    private static String extractStringField(String json, String field) {
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return "";
        idx += key.length();
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
        if (idx >= json.length() || json.charAt(idx) != '"') return "";
        idx++; // skip opening quote
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        while (idx < json.length()) {
            char c = json.charAt(idx++);
            if (escape) {
                switch (c) {
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'u' -> {
                        if (idx + 4 <= json.length()) {
                            String hex = json.substring(idx, idx + 4);
                            sb.append((char) Integer.parseInt(hex, 16));
                            idx += 4;
                        }
                    }
                    default -> sb.append(c);
                }
                escape = false;
            } else if (c == '\\') {
                escape = true;
            } else if (c == '"') {
                break;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static List<String> extractStringArrayField(String json, String field) {
        List<String> list = new ArrayList<>();
        String key = "\"" + field + "\":";
        int idx = json.indexOf(key);
        if (idx < 0) return list;
        idx += key.length();
        while (idx < json.length() && Character.isWhitespace(json.charAt(idx))) idx++;
        if (idx >= json.length() || json.charAt(idx) != '[') return list;
        idx++; // skip '['

        while (idx < json.length()) {
            while (idx < json.length() && (Character.isWhitespace(json.charAt(idx)) || json.charAt(idx) == ',')) idx++;
            if (idx >= json.length() || json.charAt(idx) == ']') break;

            if (json.startsWith("null", idx)) {
                list.add(null);
                idx += 4;
            } else if (json.charAt(idx) == '"') {
                idx++; // opening quote
                StringBuilder sb = new StringBuilder();
                boolean escape = false;
                while (idx < json.length()) {
                    char c = json.charAt(idx++);
                    if (escape) {
                        switch (c) {
                            case '"' -> sb.append('"');
                            case '\\' -> sb.append('\\');
                            case 'n' -> sb.append('\n');
                            case 'r' -> sb.append('\r');
                            case 't' -> sb.append('\t');
                            case 'u' -> {
                                if (idx + 4 <= json.length()) {
                                    String hex = json.substring(idx, idx + 4);
                                    sb.append((char) Integer.parseInt(hex, 16));
                                    idx += 4;
                                }
                            }
                            default -> sb.append(c);
                        }
                        escape = false;
                    } else if (c == '\\') {
                        escape = true;
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                }
                list.add(sb.toString());
            } else {
                idx++;
            }
        }
        return list;
    }

    private static int findValueEnd(String json, int start) {
        int idx = start;
        while (idx < json.length() && (json.charAt(idx) != ',' && json.charAt(idx) != '\n' && json.charAt(idx) != '}')) {
            idx++;
        }
        return idx;
    }
}
