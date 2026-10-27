package one.dastec.javaw.bytecode.cache;

import java.util.*;

public class CacheMetadata {
    public static final String COMPILER_VERSION = "1.0-SNAPSHOT-biginteger-and-v241";
    private String groupId;
    private String artifactId;
    private String version;
    private String jarSha256;
    private String compilerVersion;
    private long timestamp;
    private List<String> exportedFunctions = new ArrayList<>();
    private List<String> classes = new ArrayList<>();

    public CacheMetadata() {
        this.timestamp = System.currentTimeMillis();
        this.compilerVersion = COMPILER_VERSION;
    }

    public CacheMetadata(String groupId, String artifactId, String version, String jarSha256) {
        this();
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.jarSha256 = jarSha256;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getJarSha256() {
        return jarSha256;
    }

    public void setJarSha256(String jarSha256) {
        this.jarSha256 = jarSha256;
    }

    public String getCompilerVersion() {
        return compilerVersion;
    }

    public void setCompilerVersion(String compilerVersion) {
        this.compilerVersion = compilerVersion;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public List<String> getExportedFunctions() {
        return exportedFunctions;
    }

    public void setExportedFunctions(List<String> exportedFunctions) {
        this.exportedFunctions = exportedFunctions;
    }

    public List<String> getClasses() {
        return classes;
    }

    public void setClasses(List<String> classes) {
        this.classes = classes;
    }

    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"groupId\": \"").append(escape(groupId)).append("\",\n");
        sb.append("  \"artifactId\": \"").append(escape(artifactId)).append("\",\n");
        sb.append("  \"version\": \"").append(escape(version)).append("\",\n");
        sb.append("  \"jarSha256\": \"").append(escape(jarSha256)).append("\",\n");
        sb.append("  \"compilerVersion\": \"").append(escape(compilerVersion)).append("\",\n");
        sb.append("  \"timestamp\": ").append(timestamp).append(",\n");

        sb.append("  \"exportedFunctions\": [\n");
        for (int i = 0; i < exportedFunctions.size(); i++) {
            sb.append("    \"").append(escape(exportedFunctions.get(i))).append("\"");
            if (i < exportedFunctions.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ],\n");

        sb.append("  \"classes\": [\n");
        for (int i = 0; i < classes.size(); i++) {
            sb.append("    \"").append(escape(classes.get(i))).append("\"");
            if (i < classes.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    public static CacheMetadata fromJson(String json) {
        CacheMetadata meta = new CacheMetadata();
        meta.setGroupId(extractField(json, "groupId"));
        meta.setArtifactId(extractField(json, "artifactId"));
        meta.setVersion(extractField(json, "version"));
        meta.setJarSha256(extractField(json, "jarSha256"));
        meta.setCompilerVersion(extractField(json, "compilerVersion"));
        String tsStr = extractNumberField(json, "timestamp");
        if (tsStr != null) {
            meta.setTimestamp(Long.parseLong(tsStr));
        }
        meta.setExportedFunctions(extractArray(json, "exportedFunctions"));
        meta.setClasses(extractArray(json, "classes"));
        return meta;
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String extractField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static String extractNumberField(String json, String field) {
        String pattern = "\"" + field + "\"\\s*:\\s*(\\d+)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private static List<String> extractArray(String json, String arrayField) {
        List<String> list = new ArrayList<>();
        int idx = json.indexOf("\"" + arrayField + "\"");
        if (idx == -1) return list;
        int startBracket = json.indexOf("[", idx);
        int endBracket = json.indexOf("]", startBracket);
        if (startBracket != -1 && endBracket != -1) {
            String inner = json.substring(startBracket + 1, endBracket);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"([^\"]*)\"").matcher(inner);
            while (m.find()) {
                list.add(m.group(1));
            }
        }
        return list;
    }
}
