package com.example.javaw.bytecode;

import com.example.javaw.bytecode.cache.CacheMetadata;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;

/** Deterministic inventory, not an admission bypass or a proof of method reachability. */
public final class DependencyAudit {
    private record ClassInfo(String artifact, Set<String> references) {}
    private record Selected(JarEntry entry, int version) {}
    private static final class Finding {
        long count;
        String first;
        void add(String site) { count++; if (first == null || site.compareTo(first) < 0) first = site; }
    }
    private final SortedMap<String, ClassInfo> classes = new TreeMap<>();
    private final SortedMap<String, Finding> normalizedOpcodes = new TreeMap<>(), opcodes = new TreeMap<>(), members = new TreeMap<>(), features = new TreeMap<>();
    private final SortedSet<String> resources = new TreeSet<>(), providers = new TreeSet<>(), artifacts = new TreeSet<>();
    private final SortedSet<String> duplicateClasses = new TreeSet<>();
    private int classCount;

    private void finding(Map<String, Finding> table, String key, String site) {
        table.computeIfAbsent(key, ignored -> new Finding()).add(site);
    }

    /** artifact coordinates and local JAR paths are supplied separately from the portable lock. */
    public void scan(String artifact, Path path, int javaFeature) throws IOException {
        if (!artifacts.add(artifact)) throw new IllegalArgumentException("duplicate artifact " + artifact);
        try (JarFile jar = new JarFile(path.toFile())) {
            SortedMap<String, Selected> selected = new TreeMap<>();
            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;
                String logical = entry.getName();
                int version = 0;
                if (logical.startsWith("META-INF/versions/")) {
                    if (!jar.isMultiRelease()) continue;
                    String rest = logical.substring("META-INF/versions/".length());
                    int slash = rest.indexOf('/');
                    if (slash < 0) continue;
                    try { version = Integer.parseInt(rest.substring(0, slash)); }
                    catch (NumberFormatException ignored) { continue; }
                    if (version < 9 || version > javaFeature) continue;
                    logical = rest.substring(slash + 1);
                }
                Selected previous = selected.get(logical);
                if (previous == null || version > previous.version) selected.put(logical, new Selected(entry, version));
                else if (version == previous.version) throw new IOException("duplicate JAR entry " + logical);
            }
            for (var item : selected.entrySet()) {
                String name = item.getKey();
                byte[] bytes;
                try (var stream = jar.getInputStream(item.getValue().entry)) { bytes = stream.readAllBytes(); }
                if (name.endsWith(".class")) {
                    if (name.equals("module-info.class")) continue;
                    scanClass(artifact, bytes, javaFeature);
                } else {
                    String kind = name.startsWith("META-INF/services/") ? "service"
                            : name.startsWith("META-INF/spring") ? "spring" : "resource";
                    resources.add(row(artifact, name, kind, bytes.length));
                    if (kind.equals("service")) {
                        for (String line : new String(bytes, StandardCharsets.UTF_8).split("\\R")) {
                            String provider = line.split("#", 2)[0].trim();
                            if (!provider.isEmpty()) providers.add(row(artifact, name, provider.replace('.', '/')));
                        }
                    }
                }
            }
        }
    }

    private void scanClass(String artifact, byte[] bytes, int javaFeature) {
        ClassNode type = new ClassNode(); new ClassReader(bytes).accept(type, ClassReader.SKIP_FRAMES);
        Set<String> refs = new TreeSet<>();
        ClassInfo previous = classes.putIfAbsent(type.name, new ClassInfo(artifact, refs));
        if (previous != null) duplicateClasses.add(row(type.name, previous.artifact, artifact));
        classCount++;
        String classSite = type.name;
        addType(refs, type.superName); type.interfaces.forEach(name -> addType(refs, name));
        addType(refs, type.outerClass); addType(refs, type.nestHostClass);
        if (type.nestMembers != null) type.nestMembers.forEach(name -> addType(refs, name));
        if (type.permittedSubclasses != null) type.permittedSubclasses.forEach(name -> addType(refs, name));
        signature(refs, type.signature, false);
        annotations(refs, type.visibleAnnotations); annotations(refs, type.invisibleAnnotations);
        annotations(refs, type.visibleTypeAnnotations); annotations(refs, type.invisibleTypeAnnotations);
        if (type.recordComponents != null) for (RecordComponentNode component : type.recordComponents) {
            descriptor(refs, component.descriptor); signature(refs, component.signature, true);
            annotations(refs, component.visibleAnnotations); annotations(refs, component.invisibleAnnotations);
            annotations(refs, component.visibleTypeAnnotations); annotations(refs, component.invisibleTypeAnnotations);
        }
        if ((type.version & 0xffff) > javaFeature + 44) finding(features, row(artifact, "class-version", type.version & 0xffff), classSite);
        for (FieldNode field : type.fields) {
            descriptor(refs, field.desc); signature(refs, field.signature, true);
            annotations(refs, field.visibleAnnotations); annotations(refs, field.invisibleAnnotations);
            annotations(refs, field.visibleTypeAnnotations); annotations(refs, field.invisibleTypeAnnotations);
            constant(artifact, refs, field.value, classSite + "." + field.name);
            if ((field.access & Opcodes.ACC_VOLATILE) != 0) finding(features, row(artifact, "volatile", field.name), classSite);
        }
        for (MethodNode method : type.methods) {
            String site = type.name + "." + method.name + method.desc;
            descriptor(refs, method.desc); signature(refs, method.signature, false);
            method.exceptions.forEach(name -> addType(refs, name));
            annotations(refs, method.visibleAnnotations); annotations(refs, method.invisibleAnnotations);
            annotations(refs, method.visibleTypeAnnotations); annotations(refs, method.invisibleTypeAnnotations);
            if (method.visibleParameterAnnotations != null)
                for (List<AnnotationNode> values : method.visibleParameterAnnotations) annotations(refs, values);
            if (method.invisibleParameterAnnotations != null)
                for (List<AnnotationNode> values : method.invisibleParameterAnnotations) annotations(refs, values);
            annotationValue(refs, method.annotationDefault);
            if (method.name.equals("<clinit>")) finding(features, row(artifact, "class-initializer", "<clinit>"), site);
            if ((method.access & Opcodes.ACC_NATIVE) != 0) finding(features, row(artifact, "native", method.name + method.desc), site);
            if ((method.access & Opcodes.ACC_SYNCHRONIZED) != 0) finding(features, row(artifact, "synchronized", method.name), site);
            for (TryCatchBlockNode handler : method.tryCatchBlocks) {
                addType(refs, handler.type); finding(features, row(artifact, "exception-handler", Objects.toString(handler.type, "finally")), site);
            }
            for (AbstractInsnNode insn : method.instructions) {
                annotations(refs, insn.visibleTypeAnnotations); annotations(refs, insn.invisibleTypeAnnotations);
                int opcode = insn.getOpcode();
                if (BytecodeInput.normalizedOpcode(opcode))
                    finding(normalizedOpcodes, row(artifact, opcode, BytecodeCompatibility.opcodeName(opcode)), site);
                else if (opcode >= 0 && !BytecodeCompatibility.admittedOpcode(opcode))
                    finding(opcodes, row(artifact, opcode, BytecodeCompatibility.opcodeName(opcode)), site);
                if (insn instanceof TypeInsnNode test) addType(refs, test.desc);
                else if (insn instanceof FieldInsnNode field) member(artifact, refs, opcode, field.owner, field.name, field.desc, site);
                else if (insn instanceof MethodInsnNode call) member(artifact, refs, opcode, call.owner, call.name, call.desc, site);
                else if (insn instanceof LdcInsnNode ldc) constant(artifact, refs, ldc.cst, site);
                else if (insn instanceof MultiANewArrayInsnNode array) descriptor(refs, array.desc);
                else if (insn instanceof InvokeDynamicInsnNode dynamic) {
                    descriptor(refs, dynamic.desc); handle(artifact, refs, dynamic.bsm, site);
                    finding(features, row(artifact, "invokedynamic-bootstrap", dynamic.bsm.getOwner() + "." + dynamic.bsm.getName() + dynamic.bsm.getDesc()), site);
                    for (Object arg : dynamic.bsmArgs) constant(artifact, refs, arg, site);
                }
            }
        }
        boolean legacy = false;
        for (MethodNode method : type.methods) {
            if (Arrays.stream(method.instructions.toArray()).anyMatch(insn -> BytecodeInput.normalizedOpcode(insn.getOpcode()))) {
                legacy = true;
                finding(features, row(artifact, "normalized-subroutine-method", method.name + method.desc), type.name);
            }
        }
        if (legacy) BytecodeInput.normalize(type); // Verify the real dependency methods can be normalized.
        refs.remove(type.name);
    }

    private void member(String artifact, Set<String> refs, int opcode, String owner, String name, String desc, String site) {
        addType(refs, owner); descriptor(refs, desc);
        finding(members, row(artifact, opcode, owner, name, desc), site);
        String category = owner.startsWith("java/lang/reflect/") || owner.equals("java/lang/Class") ? "reflection"
                : owner.startsWith("java/lang/invoke/") ? "method-handles"
                : owner.startsWith("java/io/") || owner.startsWith("java/nio/") ? "io-nio"
                : owner.startsWith("java/net/") || owner.startsWith("javax/net/") ? "network"
                : owner.startsWith("java/util/concurrent/") || owner.equals("java/lang/Thread") || owner.contains("ThreadLocal") ? "concurrency"
                : owner.equals("java/lang/ClassLoader") || owner.equals("java/util/ServiceLoader") ? "class-resource-service-loading"
                : owner.startsWith("sun/") || owner.startsWith("jdk/") ? "jdk-internal"
                : owner.equals("java/lang/System") || owner.equals("java/lang/Runtime") ? "platform" : null;
        if (category != null) finding(features, row(artifact, category, owner + "." + name + desc), site);
    }

    private void handle(String artifact, Set<String> refs, Handle handle, String site) {
        member(artifact, refs, -handle.getTag(), handle.getOwner(), handle.getName(), handle.getDesc(), site);
    }

    private void constant(String artifact, Set<String> refs, Object value, String site) {
        if (value instanceof Type type) descriptor(refs, type.getDescriptor());
        else if (value instanceof Handle handle) handle(artifact, refs, handle, site);
        else if (value instanceof ConstantDynamic dynamic) {
            descriptor(refs, dynamic.getDescriptor()); handle(artifact, refs, dynamic.getBootstrapMethod(), site);
            finding(features, row(artifact, "constant-dynamic-bootstrap", dynamic.getBootstrapMethod().toString()), site);
            for (int i = 0; i < dynamic.getBootstrapMethodArgumentCount(); i++) constant(artifact, refs, dynamic.getBootstrapMethodArgument(i), site);
        } else if (value instanceof String) finding(features, row(artifact, "string-constant", "LDC/ConstantValue"), site);
    }

    private void addType(Set<String> refs, String name) {
        if (name == null) return;
        if (name.startsWith("[")) descriptor(refs, name); else refs.add(name);
    }

    private void descriptor(Set<String> refs, String descriptor) {
        Type type = Type.getType(descriptor);
        switch (type.getSort()) {
            case Type.OBJECT -> refs.add(type.getInternalName());
            case Type.ARRAY -> descriptor(refs, type.getElementType().getDescriptor());
            case Type.METHOD -> {
                descriptor(refs, type.getReturnType().getDescriptor());
                for (Type arg : type.getArgumentTypes()) descriptor(refs, arg.getDescriptor());
            }
        }
    }

    private void signature(Set<String> refs, String signature, boolean field) {
        if (signature == null) return;
        if (field) new SignatureReader(signature).acceptType(signatureVisitor(refs));
        else new SignatureReader(signature).accept(signatureVisitor(refs));
    }

    private SignatureVisitor signatureVisitor(Set<String> refs) {
        return new SignatureVisitor(Opcodes.ASM9) {
            private String owner;
            @Override public void visitClassType(String name) { owner = name; refs.add(name); }
            @Override public void visitInnerClassType(String name) { owner += "$" + name; refs.add(owner); }
            @Override public SignatureVisitor visitTypeArgument(char wildcard) { return signatureVisitor(refs); }
            @Override public SignatureVisitor visitArrayType() { return signatureVisitor(refs); }
            @Override public SignatureVisitor visitClassBound() { return signatureVisitor(refs); }
            @Override public SignatureVisitor visitInterfaceBound() { return signatureVisitor(refs); }
            @Override public SignatureVisitor visitSuperclass() { return signatureVisitor(refs); }
            @Override public SignatureVisitor visitInterface() { return signatureVisitor(refs); }
            @Override public SignatureVisitor visitParameterType() { return signatureVisitor(refs); }
            @Override public SignatureVisitor visitReturnType() { return signatureVisitor(refs); }
            @Override public SignatureVisitor visitExceptionType() { return signatureVisitor(refs); }
        };
    }

    private void annotations(Set<String> refs, List<? extends AnnotationNode> annotations) {
        if (annotations != null) for (AnnotationNode annotation : annotations) annotationValue(refs, annotation);
    }

    private void annotationValue(Set<String> refs, Object value) {
        if (value instanceof AnnotationNode annotation) {
            descriptor(refs, annotation.desc);
            if (annotation.values != null) for (int i = 1; i < annotation.values.size(); i += 2) annotationValue(refs, annotation.values.get(i));
        } else if (value instanceof Type type) descriptor(refs, type.getDescriptor());
        else if (value instanceof String[] enumeration) descriptor(refs, enumeration[0]);
        else if (value instanceof List<?> values) values.forEach(item -> annotationValue(refs, item));
    }

    public void write(Path directory, Collection<String> roots, int javaFeature) throws IOException {
        for (String root : roots) if (!classes.containsKey(root))
            throw new IllegalArgumentException("unresolved audit root " + root);
        Files.createDirectories(directory);
        Set<String> reachable = new TreeSet<>();
        Deque<String> pending = new ArrayDeque<>(roots);
        while (!pending.isEmpty()) {
            String name = pending.removeFirst();
            if (!reachable.add(name)) continue;
            ClassInfo info = classes.get(name);
            if (info != null) pending.addAll(info.references);
        }
        SortedSet<String> types = new TreeSet<>(), external = new TreeSet<>();
        for (var entry : classes.entrySet()) {
            types.add(row(entry.getKey(), entry.getValue().artifact, reachable.contains(entry.getKey()) ? "root-reference-closure" : "not-in-static-closure"));
            for (String ref : entry.getValue().references)
                if (!classes.containsKey(ref)) external.add(row(ref, entry.getKey(), reachable.contains(entry.getKey())));
        }
        files(directory, "artifacts.tsv", "artifact", artifacts);
        files(directory, "classes.tsv", "class\tartifact\tstaticEstimate", types);
        files(directory, "external-types.tsv", "externalType\treferringClass\treferrerInStaticClosure", external);
        files(directory, "resources.tsv", "artifact\tresource\tkind\tbytes", resources);
        files(directory, "services.tsv", "artifact\tserviceResource\tprovider", providers);
        files(directory, "duplicate-classes.tsv", "class\tfirstArtifact\totherArtifact", duplicateClasses);
        findings(directory, "normalized-opcodes.tsv", "artifact\topcode\tname", normalizedOpcodes);
        findings(directory, "unsupported-opcodes.tsv", "artifact\topcode\tname", opcodes);
        findings(directory, "member-references.tsv", "artifact\topcodeOrNegativeHandleTag\towner\tmember\tdescriptor", members);
        findings(directory, "requirements.tsv", "artifact\tcategory\tsymbol", features);
        files(directory, "summary.tsv", "metric\tvalue", List.of(
                row("compilerRevision", CacheMetadata.COMPILER_VERSION), row("javaFeature", javaFeature),
                row("artifacts", artifacts.size()), row("classDefinitions", classCount), row("uniqueClasses", classes.size()),
                row("resources", resources.size()), row("serviceProviders", providers.size()), row("duplicateClasses", duplicateClasses.size()),
                row("staticClosureBundledClasses", classes.keySet().stream().filter(reachable::contains).count()),
                row("roots", String.join(",", new TreeSet<>(roots))), row("analysis", "class-reference estimate; reflection/resources/generated classes are not complete reachability")));
    }

    private void findings(Path directory, String name, String header, Map<String, Finding> findings) throws IOException {
        List<String> rows = new ArrayList<>();
        findings.forEach((key, value) -> rows.add(key + row("", value.count, value.first)));
        files(directory, name, header + "\tcount\tfirstSite", rows);
    }

    private static String row(Object... values) {
        return String.join("\t", Arrays.stream(values).map(Objects::toString)
                .map(value -> value.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n")).toList());
    }

    private void files(Path directory, String name, String header, Collection<String> rows) throws IOException {
        Files.write(directory.resolve(name), (header + "\n" + String.join("\n", rows) + "\n").getBytes(StandardCharsets.UTF_8));
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) throw new IllegalArgumentException("Usage: DependencyAudit manifest.tsv output-directory java-feature [root-internal-name ...]");
        DependencyAudit audit = new DependencyAudit();
        int javaFeature = Integer.parseInt(args[2]);
        if (javaFeature < 9) throw new IllegalArgumentException("audit requires Java target 9 or later");
        for (String line : Files.readAllLines(Path.of(args[0]))) {
            String[] fields = line.split("\t", 2);
            if (fields.length != 2) throw new IllegalArgumentException("invalid manifest row");
            audit.scan(fields[0], Path.of(fields[1]), javaFeature);
        }
        audit.write(Path.of(args[1]), Arrays.asList(args).subList(3, args.length), javaFeature);
    }
}
