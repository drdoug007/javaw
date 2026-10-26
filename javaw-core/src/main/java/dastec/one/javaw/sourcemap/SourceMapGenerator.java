package dastec.one.javaw.sourcemap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Generator for standard Source Map v3 files and WebAssembly sourceMappingURL annotations.
 */
public class SourceMapGenerator {
    private String file = "";
    private final List<String> sources = new ArrayList<>();
    private final List<String> sourcesContent = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final Map<Integer, List<Mapping>> lineMappings = new TreeMap<>(); // genLine (0-based) -> list of mappings

    public static class Mapping {
        public final int genCol;
        public final int sourceIdx;
        public final int origLine;
        public final int origCol;
        public final int nameIdx;

        public Mapping(int genCol, int sourceIdx, int origLine, int origCol, int nameIdx) {
            this.genCol = genCol;
            this.sourceIdx = sourceIdx;
            this.origLine = origLine;
            this.origCol = origCol;
            this.nameIdx = nameIdx;
        }
    }

    public SourceMapGenerator() {
    }

    public SourceMapGenerator(String file) {
        this.file = file != null ? file : "";
    }

    public void setFile(String file) {
        this.file = file != null ? file : "";
    }

    public String getFile() {
        return file;
    }

    public int addSource(String sourcePath, String sourceContent) {
        if (sourcePath == null) sourcePath = "Source.java";
        int idx = sources.indexOf(sourcePath);
        if (idx >= 0) {
            if (sourceContent != null && idx < sourcesContent.size() && sourcesContent.get(idx) == null) {
                sourcesContent.set(idx, sourceContent);
            }
            return idx;
        }
        sources.add(sourcePath);
        sourcesContent.add(sourceContent);
        return sources.size() - 1;
    }

    public int getSourceIndex(String sourcePath) {
        return sources.indexOf(sourcePath);
    }

    public int addName(String name) {
        if (name == null || name.isEmpty()) return -1;
        int idx = names.indexOf(name);
        if (idx >= 0) return idx;
        names.add(name);
        return names.size() - 1;
    }

    public void addMapping(int genLine, int genCol, String sourceFile, int origLine, int origCol) {
        addMapping(genLine, genCol, sourceFile, origLine, origCol, null);
    }

    public void addMapping(int genLine, int genCol, String sourceFile, int origLine, int origCol, String name) {
        if (genLine < 0 || genCol < 0) return;
        int sourceIdx = -1;
        if (sourceFile != null) {
            sourceIdx = addSource(sourceFile, null);
        }
        int nameIdx = addName(name);

        List<Mapping> list = lineMappings.computeIfAbsent(genLine, k -> new ArrayList<>());
        // Prevent exact duplicate column mappings
        if (!list.isEmpty()) {
            Mapping last = list.get(list.size() - 1);
            if (last.genCol == genCol && last.sourceIdx == sourceIdx && last.origLine == origLine && last.origCol == origCol) {
                return;
            }
        }
        list.add(new Mapping(genCol, sourceIdx, origLine, origCol, nameIdx));
    }

    public String encodeMappings() {
        StringBuilder sb = new StringBuilder();
        int maxGenLine = lineMappings.isEmpty() ? 0 : Collections.max(lineMappings.keySet());
        int prevSourceIdx = 0;
        int prevOrigLine = 0;
        int prevOrigCol = 0;
        int prevNameIdx = 0;

        for (int genLine = 0; genLine <= maxGenLine; genLine++) {
            if (genLine > 0) {
                sb.append(";");
            }
            List<Mapping> mappings = lineMappings.get(genLine);
            if (mappings == null || mappings.isEmpty()) {
                continue;
            }
            int prevGenCol = 0;
            for (int i = 0; i < mappings.size(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                Mapping m = mappings.get(i);
                // Field 1: generated column delta
                sb.append(Base64VLQ.encode(m.genCol - prevGenCol));
                prevGenCol = m.genCol;

                if (m.sourceIdx >= 0) {
                    // Field 2: source index delta
                    sb.append(Base64VLQ.encode(m.sourceIdx - prevSourceIdx));
                    prevSourceIdx = m.sourceIdx;

                    // Field 3: original line delta
                    sb.append(Base64VLQ.encode(m.origLine - prevOrigLine));
                    prevOrigLine = m.origLine;

                    // Field 4: original column delta
                    sb.append(Base64VLQ.encode(m.origCol - prevOrigCol));
                    prevOrigCol = m.origCol;

                    if (m.nameIdx >= 0) {
                        // Field 5: name index delta
                        sb.append(Base64VLQ.encode(m.nameIdx - prevNameIdx));
                        prevNameIdx = m.nameIdx;
                    }
                }
            }
        }
        return sb.toString();
    }

    public String generateJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": 3,\n");
        sb.append("  \"file\": \"").append(escapeJson(file)).append("\",\n");

        sb.append("  \"sources\": [");
        for (int i = 0; i < sources.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(escapeJson(sources.get(i))).append("\"");
        }
        sb.append("],\n");

        boolean hasContent = sourcesContent.stream().anyMatch(Objects::nonNull);
        if (hasContent) {
            sb.append("  \"sourcesContent\": [");
            for (int i = 0; i < sourcesContent.size(); i++) {
                if (i > 0) sb.append(", ");
                String content = sourcesContent.get(i);
                if (content != null) {
                    sb.append("\"").append(escapeJson(content)).append("\"");
                } else {
                    sb.append("null");
                }
            }
            sb.append("],\n");
        }

        sb.append("  \"names\": [");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"").append(escapeJson(names.get(i))).append("\"");
        }
        sb.append("],\n");

        sb.append("  \"mappings\": \"").append(escapeJson(encodeMappings())).append("\"\n");
        sb.append("}\n");
        return sb.toString();
    }

    public String generateBase64DataUri() {
        String json = generateJson();
        String base64 = Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        return "data:application/json;charset=utf-8;base64," + base64;
    }

    public List<String> getSources() {
        return Collections.unmodifiableList(sources);
    }

    public List<String> getSourcesContent() {
        return Collections.unmodifiableList(sourcesContent);
    }

    public List<String> getNames() {
        return Collections.unmodifiableList(names);
    }

    public Map<Integer, List<Mapping>> getLineMappings() {
        return Collections.unmodifiableMap(lineMappings);
    }

    /**
     * Embeds a sourceMappingURL custom section into a binary WebAssembly (.wasm) byte array.
     */
    public static byte[] embedSourceMapInWasm(byte[] wasmBytes, String sourceMapUrl) {
        if (wasmBytes == null || wasmBytes.length < 8 || sourceMapUrl == null) {
            return wasmBytes;
        }

        // Check if sourceMappingURL custom section already exists
        String existing = extractSourceMapUrlFromWasm(wasmBytes);
        if (existing != null) {
            return wasmBytes; // Already embedded
        }

        byte[] nameBytes = "sourceMappingURL".getBytes(StandardCharsets.UTF_8);
        byte[] urlBytes = sourceMapUrl.getBytes(StandardCharsets.UTF_8);

        ByteArrayOutputStream payload = new ByteArrayOutputStream();
        writeUnsignedLeb128(payload, nameBytes.length);
        try {
            payload.write(nameBytes);
            writeUnsignedLeb128(payload, urlBytes.length);
            payload.write(urlBytes);
        } catch (IOException ignored) {
        }

        byte[] payloadBytes = payload.toByteArray();
        ByteArrayOutputStream customSection = new ByteArrayOutputStream();
        customSection.write(0); // Section ID 0 for custom section
        writeUnsignedLeb128(customSection, payloadBytes.length);
        try {
            customSection.write(payloadBytes);
        } catch (IOException ignored) {
        }

        byte[] sectionBytes = customSection.toByteArray();
        byte[] result = new byte[wasmBytes.length + sectionBytes.length];
        System.arraycopy(wasmBytes, 0, result, 0, wasmBytes.length);
        System.arraycopy(sectionBytes, 0, result, wasmBytes.length, sectionBytes.length);
        return result;
    }

    /**
     * Extracts the sourceMappingURL from a WebAssembly binary module if present.
     */
    public static String extractSourceMapUrlFromWasm(byte[] wasmBytes) {
        if (wasmBytes == null || wasmBytes.length < 8) return null;
        int pos = 8; // Skip magic (4) and version (4)

        while (pos < wasmBytes.length) {
            if (pos >= wasmBytes.length) break;
            int sectionId = wasmBytes[pos++] & 0xFF;
            int[] lenLeb = readUnsignedLeb128(wasmBytes, pos);
            int sectionLength = lenLeb[0];
            pos = lenLeb[1];

            int sectionStart = pos;
            if (sectionId == 0) { // Custom section
                int[] nameLenLeb = readUnsignedLeb128(wasmBytes, pos);
                int nameLen = nameLenLeb[0];
                pos = nameLenLeb[1];
                if (pos + nameLen <= wasmBytes.length) {
                    String sectionName = new String(wasmBytes, pos, nameLen, StandardCharsets.UTF_8);
                    pos += nameLen;
                    if ("sourceMappingURL".equals(sectionName)) {
                        int remaining = sectionLength - (pos - sectionStart);
                        if (remaining > 0 && pos < wasmBytes.length) {
                            int[] urlLenLeb = readUnsignedLeb128(wasmBytes, pos);
                            int urlLen = urlLenLeb[0];
                            int urlPos = urlLenLeb[1];
                            if (urlPos + urlLen <= wasmBytes.length) {
                                return new String(wasmBytes, urlPos, urlLen, StandardCharsets.UTF_8);
                            } else {
                                return new String(wasmBytes, pos, remaining, StandardCharsets.UTF_8);
                            }
                        }
                    }
                }
            }
            pos = sectionStart + sectionLength;
        }
        return null;
    }

    public static String escapeCustomSectionPayload(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        writeUnsignedLeb128(bos, bytes.length);
        try {
            bos.write(bytes);
        } catch (IOException ignored) {
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : bos.toByteArray()) {
            int ub = b & 0xFF;
            if (ub >= 32 && ub <= 126 && ub != '"' && ub != '\\') {
                sb.append((char) ub);
            } else {
                sb.append(String.format("\\%02x", ub));
            }
        }
        return sb.toString();
    }

    public static String escapeRawCustomSectionPayload(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            int ub = b & 0xFF;
            if (ub >= 32 && ub <= 126 && ub != '"' && ub != '\\') {
                sb.append((char) ub);
            } else {
                sb.append(String.format("\\%02x", ub));
            }
        }
        return sb.toString();
    }

    public static void writeUnsignedLeb128(ByteArrayOutputStream out, int value) {
        int remaining = value >>> 7;
        while (remaining != 0) {
            out.write((byte) ((value & 0x7f) | 0x80));
            value = remaining;
            remaining >>>= 7;
        }
        out.write((byte) (value & 0x7f));
    }

    public static int[] readUnsignedLeb128(byte[] bytes, int offset) {
        int result = 0;
        int cur;
        int count = 0;
        do {
            if (offset >= bytes.length) break;
            cur = bytes[offset++] & 0xff;
            result |= (cur & 0x7f) << (count * 7);
            count++;
        } while (((cur & 0x80) == 0x80) && count < 5);
        return new int[]{result, offset};
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 32 || c >= 127) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}
