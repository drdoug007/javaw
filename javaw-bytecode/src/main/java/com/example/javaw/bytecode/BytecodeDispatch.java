package com.example.javaw.bytecode;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Closed-world virtual dispatch with method access, override slots, and validated private nests. */
public final class BytecodeDispatch {
    private static final Set<String> FINAL_OBJECT_METHODS = Set.of("getClass()Ljava/lang/Class;", "notify()V", "notifyAll()V",
            "wait()V", "wait(J)V", "wait(JI)V");
    private static final java.util.regex.Pattern ARRAY_DESCRIPTOR = java.util.regex.Pattern.compile("\\[+(?:[ZBCSIJFD]|L[^;]+;)");
    private final Map<String, ClassNode> linked;
    private final String caller;
    private final BytecodeObjectLayouts layouts;
    private final Map<String, Integer> classIds = new LinkedHashMap<>();
    private final Map<String, Integer> referenceArrayIds = new LinkedHashMap<>();
    private record Declaration(String owner, MethodNode method) {}
    private static final class DefaultConflict extends IllegalArgumentException {
        DefaultConflict(String message) { super(message); }
    }

    public boolean isDispatchFailure(String target) {
        return target.equals(BytecodeExceptions.Implicit.LINKAGE.helper)
                || target.equals(BytecodeExceptions.Implicit.ABSTRACT_METHOD.helper)
                || target.equals(BytecodeExceptions.Implicit.ILLEGAL_ACCESS.helper);
    }

    public BytecodeDispatch(Map<String, ClassNode> linked) {
        this(linked, null);
    }

    public BytecodeDispatch(Map<String, ClassNode> linked, String caller) {
        this.caller = caller;
        if (BytecodeExceptionRuntime.enabled(linked)) {
            linked = new LinkedHashMap<>(linked);
            linked.putIfAbsent(BytecodeObjects.OBJECT, BytecodeObjects.declaration());
        }
        this.linked = linked;
        this.layouts = new BytecodeObjectLayouts(linked);
        int id = 1;
        for (String name : new TreeSet<>(linked.keySet())) classIds.put(name, id++);
        TreeSet<String> components = new TreeSet<>(linked.keySet());
        components.add("java/lang/Object");
        int arrayId = -100;
        for (String component : components) referenceArrayIds.put(component, arrayId--);
        TreeSet<String> arrays = new TreeSet<>();
        for (ClassNode type : linked.values()) {
            for (FieldNode field : type.fields) collectArrays(arrays, field.desc);
            for (MethodNode method : type.methods) {
                collectArrays(arrays, method.desc);
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction instanceof TypeInsnNode t) {
                        collectArrays(arrays, t.desc);
                        if (t.getOpcode() == Opcodes.ANEWARRAY) collectArrays(arrays, arrayDescriptor(t.desc));
                    }
                    if (instruction instanceof LdcInsnNode ldc && ldc.cst instanceof Type literal)
                        collectArrays(arrays,literal.getDescriptor());
                    if (instruction instanceof MultiANewArrayInsnNode m) collectArrays(arrays, m.desc);
                    if (instruction instanceof MethodInsnNode m) {
                        collectArrays(arrays, m.desc);
                        if (m.owner.startsWith("[")) collectArrays(arrays, m.owner);
                    }
                    if (instruction instanceof FieldInsnNode f) collectArrays(arrays, f.desc);
                    if (instruction instanceof FrameNode frame) {
                        if(frame.local != null) for(Object value : frame.local) if(value instanceof String text) collectArrays(arrays,text);
                        if(frame.stack != null) for(Object value : frame.stack) if(value instanceof String text) collectArrays(arrays,text);
                    }
                }
            }
        }
        for (String descriptor : arrays) {
            if (BytecodeArrays.primitive(descriptor) != null) continue;
            String component = descriptor.substring(1);
            if(component.startsWith("L")) component = component.substring(1,component.length()-1);
            if(!referenceArrayIds.containsKey(component)) referenceArrayIds.put(component, arrayId--);
        }
    }

    private static void collectArrays(Set<String> arrays, String descriptor) {
        var matcher = ARRAY_DESCRIPTOR.matcher(descriptor);
        while(matcher.find()) {
            String array = matcher.group();
            if (!BytecodeDeclarations.validDescriptor(array, false)) continue;
            while(array.startsWith("[")) { arrays.add(array); array = array.substring(1); }
        }
    }

    private static int arrayRank(String descriptor) {
        int max = 0, current = 0;
        for (char c : descriptor.toCharArray()) { current = c == '[' ? current + 1 : 0; max = Math.max(max, current); }
        return max;
    }

    private static String arrayDescriptor(String component) {
        return component.startsWith("[") ? "[" + component : "[L" + component + ";";
    }

    Map<String,Integer> runtimeTypes() {
        Map<String,Integer> types=new LinkedHashMap<>();
        classIds.forEach((name,id)->types.put("L"+name+";",id));
        for(var primitive:BytecodeArrays.PRIMITIVES)types.put(primitive.descriptor(),primitive.classId());
        referenceArrayIds.forEach((component,id)->types.put(arrayDescriptor(component),id));
        types.putAll(BytecodeClasses.primitiveTokens());
        return types;
    }

    boolean descriptorAssignable(String from,String to) { return arrayAssignable(from,to); }

    private boolean arrayAssignable(String from, String to) {
        if (from.equals(to)) return true;
        if (from.startsWith("[")) {
            if (to.equals("Ljava/lang/Object;")) return true;
            if (to.equals("Ljava/lang/Cloneable;") || to.equals("Ljava/io/Serializable;")) {
                ClassNode marker = linked.get(to.substring(1, to.length() - 1));
                return marker != null && (marker.access & Opcodes.ACC_INTERFACE) != 0;
            }
            return to.startsWith("[") && arrayAssignable(from.substring(1), to.substring(1));
        }
        if (from.startsWith("L") && to.startsWith("L"))
            return subtype(from.substring(1, from.length()-1), to.substring(1, to.length()-1), new HashSet<>());
        return false;
    }

    private String packageName(String name) {
        int slash = name.lastIndexOf('/');
        return slash < 0 ? "" : name.substring(0, slash);
    }

    /** JVM class accessibility uses public/package access on the symbolic class name. */
    void checkClassAccess(String name) {
        ClassNode type = linked.get(name);
        if (type == null) throw new IllegalArgumentException("link the symbolic class/interface " + name);
        if ((type.access & Opcodes.ACC_PUBLIC) == 0 && caller != null
                && !packageName(name).equals(packageName(caller)))
            throw new IllegalArgumentException("inaccessible symbolic class/interface " + name);
    }

    private boolean nestMember(ClassNode host, ClassNode member) {
        if (!packageName(host.name).equals(packageName(member.name))) return false;
        if (member.name.equals(host.name)) return member.nestHostClass == null;
        return host.name.equals(member.nestHostClass) && host.nestMembers != null
                && host.nestMembers.contains(member.name);
    }

    void checkPrivateAccess(String declaringOwner) {
        if (declaringOwner.equals(caller)) return;
        ClassNode from = linked.get(caller), target = linked.get(declaringOwner);
        if (from != null && target != null) {
            String hostName = from.nestHostClass == null ? from.name : from.nestHostClass;
            ClassNode host = linked.get(hostName);
            if (host != null && host.nestHostClass == null && nestMember(host, from) && nestMember(host, target)) return;
        }
        throw new IllegalArgumentException("private member access requires the declaring class or a validated linked nest; "
                + "link the nest host and reciprocal same-package NestHost/NestMembers metadata");
    }

    public boolean isIdentityHashIntrinsic(MethodInsnNode call) {
        return exceptionRuntimeEnabled() && !linked.containsKey("java/lang/System") && BytecodeObjects.identityHash(call);
    }

    public boolean isArraycopyIntrinsic(MethodInsnNode call) {
        return BytecodeArrays.isArraycopy(call) && !linked.containsKey(call.owner);
    }

    /** Active-use initialization is available with the isolated exception runtime. */
    public String initializationCall(String owner) {
        return exceptionRuntimeEnabled() && !owner.equals(BytecodeObjects.OBJECT) && linked.containsKey(owner)
                ? "call " + BytecodeInitialization.symbol(owner) : "";
    }

    public boolean exceptionRuntimeEnabled() { return BytecodeExceptionRuntime.enabled(linked); }

    public String exceptionSupportIntrinsic(MethodInsnNode call) {
        if (!exceptionRuntimeEnabled() || !call.owner.equals("javaw/runtime/ExceptionSupport") || call.getOpcode() != Opcodes.INVOKESTATIC) return null;
        String metadata = switch(call.name+call.desc) {
            case "classMirror(I)Ljava/lang/Class;" -> "$__class_mirror";
            case "typeName(I)Ljava/lang/String;" -> "$__type_name";
            case "simpleName(I)Ljava/lang/String;" -> "$__type_simple";
            case "canonicalName(I)Ljava/lang/String;" -> "$__type_canonical";
            case "enumType(I)Z" -> "$__type_enum";
            case "typeModifiers(I)I" -> "$__type_modifiers";
            case "componentType(I)Ljava/lang/Class;" -> "$__type_component";
            case "superType(I)Ljava/lang/Class;" -> "$__type_super";
            case "interfaces(I)[Ljava/lang/Class;" -> "$__type_interfaces";
            case "assignable(II)Z" -> "$__type_assignable";
            default -> null;
        };
        if(metadata!=null)return metadata;
        if (call.name.equals("capture") && call.desc.equals("()[Ljava/lang/StackTraceElement;")) return "$__trace_capture";
        if (call.name.equals("className") && call.desc.equals("(Ljava/lang/Throwable;)Ljava/lang/String;")) return "$__exception_class_name";
        return null;
    }

    /** Explicitly linked definitions opt individual implicit failures into Java exception execution. */
    public boolean hasImplicitException(BytecodeExceptions.Implicit kind) {
        return linked.containsKey(kind.className);
    }

    public String implicitFailureWat(BytecodeExceptions.Implicit kind) {
        return hasImplicitException(kind) ? "call " + kind.helper + " unreachable" : "unreachable";
    }

    String implicitExceptionRuntimeWat(BytecodeExceptions.Implicit kind) {
        String name = kind.className;
        String simpleName = name.substring(name.lastIndexOf('/') + 1);
        ClassNode type = linked.get(name);
        if (type == null) return "";
        try {
            ClassNode root = linked.get("java/lang/Throwable");
            if (root == null || (root.access & Opcodes.ACC_INTERFACE) != 0
                    || (type.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != Opcodes.ACC_PUBLIC
                    || !subtype(name, "java/lang/Throwable", new HashSet<>()))
                throw new IllegalArgumentException("link a public concrete " + simpleName + " class extending linked Throwable");
            Declaration init = ownMethod(name, "<init>", "()V");
            if (init == null || (init.method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != Opcodes.ACC_PUBLIC)
                throw new IllegalArgumentException("link a public bytecode " + simpleName + "() constructor");
            Integer size = layouts.sizes().get(name);
            if (size == null) throw new IllegalArgumentException("link a complete " + simpleName + " object layout through Object");
            if (kind == BytecodeExceptions.Implicit.OUT_OF_MEMORY || kind == BytecodeExceptions.Implicit.STACK_OVERFLOW) {
                // The host must reserve this exported prefix. Throwing OOME cannot allocate.
                StringBuilder data = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    int value = i < 4 ? (classId(name) >>> (i * 8)) & 255 : 0;
                    data.append(String.format(java.util.Locale.ROOT, "\\%02x", value));
                }
                int address = BytecodeTraces.emergencyAddress(kind, linked);
                String prefix = kind == BytecodeExceptions.Implicit.OUT_OF_MEMORY
                        ? "    (global (export \"__exception_heap_base\") i32 (i32.const "
                            + (exceptionRuntimeEnabled() ? BytecodeTraces.stackBase(linked) + BytecodeTraces.CAPACITY * 8 : 8 + size) + "))\n" : "";
                return prefix + "    (data (i32.const " + address + ") \"" + data + "\")\n"
                        + "    (func " + kind.helper + " i32.const " + address + " throw $__java_exception)\n";
            }
            return "    (func " + kind.helper + " (local $exception i32)\n"
                    + "        i32.const " + size + "\n        call $__heap_alloc\n        local.tee $exception\n"
                    + "        i32.eqz\n        if\n            " + implicitFailureWat(BytecodeExceptions.Implicit.OUT_OF_MEMORY) + "\n        end\n"
                    + "        local.get $exception\n        i32.const 0\n        i32.const " + size + "\n        memory.fill\n"
                    + "        local.get $exception\n        i32.const " + classId(name) + "\n        i32.store\n"
                    + "        local.get $exception\n"
                    + (exceptionRuntimeEnabled() && kind == BytecodeExceptions.Implicit.ARITHMETIC ? "        call " + BytecodeStrings.symbol("/ by zero") + "\n" : "")
                    + "        call " + TypeHelper.getMethodFunctionName(name, "<init>",
                            exceptionRuntimeEnabled() && kind == BytecodeExceptions.Implicit.ARITHMETIC ? "(Ljava/lang/String;)V" : "()V") + "\n"
                    + "        local.get $exception\n        throw $__java_exception\n    )\n";
        } catch (IllegalArgumentException error) {
            throw BytecodeCompatibility.failure(type, null, null, "implicit " + kind.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + " exception runtime", error.getMessage());
        }
    }

    public int classId(String name) {
        Integer id = classIds.get(name);
        if (id == null) throw new IllegalArgumentException("Unlinked class " + name);
        return id;
    }

    /** Concrete runtime IDs assignable to a linked class/interface type-test target. */
    public Set<Integer> typeTestIds(String target) {
        if (target.startsWith("[")) {
            if (!BytecodeDeclarations.validDescriptor(target, false))
                throw new IllegalArgumentException("use a valid array descriptor");
            BytecodeArrays.Primitive primitive = BytecodeArrays.primitive(target);
            if (primitive != null) return Set.of(primitive.classId());
            referenceArrayId(target.substring(1).startsWith("L")
                    ? target.substring(2, target.length()-1) : target.substring(1));
            Set<Integer> ids = new LinkedHashSet<>();
            for (var entry : referenceArrayIds.entrySet())
                if (arrayAssignable(arrayDescriptor(entry.getKey()), target)) ids.add(entry.getValue());
            return ids;
        }
        if (!target.equals("java/lang/Object")) {
            ClassNode type = linked.get(target);
            if (type == null || target.startsWith("["))
                throw new IllegalArgumentException("link the type-test class/interface; array type tests are unsupported");
            checkClassAccess(target);
        }
        Set<Integer> result = new LinkedHashSet<>();
        for (var entry : classIds.entrySet()) {
            ClassNode type = linked.get(entry.getKey());
            validateTypeHierarchy(type.name, new HashSet<>(), new HashSet<>());
            if ((type.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) == 0
                    && layouts.contains(type.name) && subtype(type.name, target, new HashSet<>()))
                result.add(entry.getValue());
        }
        ClassNode marker = linked.get(target);
        if (target.equals("java/lang/Object") || marker != null && (marker.access & Opcodes.ACC_INTERFACE) != 0
                && (target.equals("java/lang/Cloneable") || target.equals("java/io/Serializable")))
        {
            for (BytecodeArrays.Primitive primitive : BytecodeArrays.PRIMITIVES) result.add(primitive.classId());
            result.addAll(referenceArrayIds.values());
        }
        return result;
    }

    public int referenceArrayId(String component) {
        if (component.startsWith("[") && (!BytecodeDeclarations.validDescriptor(component, false) || arrayRank(component) >= 255))
            throw new IllegalArgumentException("use a valid array component with resulting rank at most 255");
        String leaf = component;
        if (component.startsWith("[")) {
            Type type = Type.getType(component).getElementType();
            leaf = type.getSort() == Type.OBJECT ? type.getInternalName() : "java/lang/Object";
        }
        if (!leaf.equals("java/lang/Object")) checkClassAccess(leaf);
        Integer id = referenceArrayIds.get(component);
        if (id == null) throw new IllegalArgumentException("link the reference-array component " + component);
        return id;
    }

    /** Store checks use the actual array component, independent of its static covariant type. */
    String referenceArrayRuntimeWat() {
        StringBuilder wat = new StringBuilder("(func $__reference_array_address (param $a i32) (param $i i32) (result i32)\n");
        wat.append("local.get $a i32.eqz if ").append(implicitFailureWat(BytecodeExceptions.Implicit.NULL_POINTER)).append(" end\nlocal.get $a i32.load\n");
        // All reference-array tags are below the reserved primitive-tag range.
        wat.append("i32.const -100 i32.gt_s if unreachable end\n");
        wat.append("local.get $i local.get $a i32.load offset=4 i32.ge_u if ").append(implicitFailureWat(BytecodeExceptions.Implicit.BOUNDS)).append(" end\n");
        wat.append("local.get $a i32.const 8 i32.add local.get $i i32.const 4 i32.mul i32.add)\n");
        wat.append("(func $__reference_array_store (param $a i32) (param $i i32) (param $v i32) (local $p i32)\n");
        wat.append("local.get $a local.get $i call $__reference_array_address local.set $p\n");
        wat.append("local.get $v if\n");
        // Runtime metadata is module-wide; component accessibility is checked at each instruction.
        BytecodeDispatch metadata = new BytecodeDispatch(linked);
        for (var entry : referenceArrayIds.entrySet()) {
            wat.append("local.get $a i32.load i32.const ").append(entry.getValue()).append(" i32.eq if\n");
            wat.append("i32.const 0\n");
            for (int id : metadata.typeTestIds(entry.getKey()))
                wat.append("local.get $v i32.load i32.const ").append(id).append(" i32.eq i32.or\n");
            wat.append("i32.eqz if ").append(implicitFailureWat(BytecodeExceptions.Implicit.ARRAY_STORE)).append(" end\nend\n");
        }
        wat.append("end local.get $p local.get $v i32.store)\n");
        return wat.toString();
    }

    public int arrayId(String descriptor) {
        BytecodeArrays.Primitive primitive = BytecodeArrays.primitive(descriptor);
        if (primitive != null) return primitive.classId();
        String component = descriptor.substring(1);
        return referenceArrayId(component.startsWith("L") ? component.substring(1,component.length()-1) : component);
    }

    public void validateMultiArray(MultiANewArrayInsnNode instruction) {
        if (!BytecodeDeclarations.validDescriptor(instruction.desc, false) || !instruction.desc.startsWith("[") || instruction.dims < 1 || instruction.dims > arrayRank(instruction.desc))
            throw new IllegalArgumentException("MULTIANEWARRAY requires 1..rank dimensions and an array descriptor");
        arrayId(instruction.desc);
    }

    public static String multiArraySymbol(String descriptor, int dimensions) {
        StringBuilder symbol = new StringBuilder("$__multi_");
        for(char c : descriptor.toCharArray()) symbol.append(Integer.toHexString(c)).append('_');
        return symbol.append(dimensions).toString();
    }

    String multiArrayRuntimeWat() {
        Map<String, MultiANewArrayInsnNode> sites = new TreeMap<>();
        for(ClassNode type : linked.values()) for(MethodNode method : type.methods)
            for(AbstractInsnNode instruction : method.instructions)
                if(instruction instanceof MultiANewArrayInsnNode multi)
                    sites.put(multiArraySymbol(multi.desc,multi.dims),multi);
        StringBuilder wat = new StringBuilder();
        for(var entry : sites.entrySet()) {
            var multi = entry.getValue();
            for(int level=0;level<multi.dims;level++) {
                wat.append("(func ").append(entry.getKey()).append(level==0 ? "" : "_"+level);
                for(int i=0;i<multi.dims;i++) wat.append(" (param $d").append(i).append(" i32)");
                wat.append(" (result i32) (local $a i32) (local $i i32)\n");
                if(level==0) for(int i=0;i<multi.dims;i++)
                    wat.append("local.get $d").append(i).append(" i32.const 0 i32.lt_s if ").append(implicitFailureWat(BytecodeExceptions.Implicit.NEGATIVE_SIZE)).append(" end\n");
                String descriptor = multi.desc.substring(level);
                BytecodeArrays.Primitive primitive = BytecodeArrays.primitive(descriptor);
                wat.append("local.get $d").append(level).append(" i32.const ").append(arrayId(descriptor))
                        .append(" i32.const ").append(primitive==null ? 4 : primitive.width())
                        .append(" call $__new_primitive_array local.set $a\n");
                if(level+1<multi.dims) {
                    wat.append("block $done loop $next local.get $i local.get $d").append(level)
                            .append(" i32.ge_u br_if $done\nlocal.get $a i32.const 8 i32.add local.get $i i32.const 4 i32.mul i32.add\n");
                    for(int i=0;i<multi.dims;i++) wat.append("local.get $d").append(i).append(' ');
                    wat.append("call ").append(entry.getKey()).append('_').append(level+1)
                            .append(" i32.store\nlocal.get $i i32.const 1 i32.add local.set $i br $next end end\n");
                }
                wat.append("local.get $a)\n");
            }
        }
        return wat.toString();
    }

    private void validateTypeHierarchy(String name, Set<String> visiting, Set<String> complete) {
        if ("java/lang/Object".equals(name) || complete.contains(name)) return;
        ClassNode type = linked.get(name);
        if (type == null || !visiting.add(name))
            throw new IllegalArgumentException("link a complete acyclic type hierarchy: " + name);
        if (type.superName != null) validateTypeHierarchy(type.superName, visiting, complete);
        for (String iface : type.interfaces) validateTypeHierarchy(iface, visiting, complete);
        visiting.remove(name);
        complete.add(name);
    }

    private record FieldDeclaration(String owner, FieldNode field) {}

    private FieldDeclaration resolveField(String name, String field, String descriptor, Set<String> visiting) {
        if ("java/lang/Object".equals(name) || name == null) return null;
        ClassNode type = linked.get(name);
        if (type == null || !visiting.add(name))
            throw new IllegalArgumentException("link a complete acyclic field hierarchy: " + name);
        for (FieldNode candidate : type.fields)
            if (candidate.name.equals(field) && candidate.desc.equals(descriptor))
                return new FieldDeclaration(name, candidate);
        // JVM field resolution searches direct superinterfaces before the superclass.
        for (String iface : type.interfaces) {
            FieldDeclaration found = resolveField(iface, field, descriptor, visiting);
            if (found != null) return found;
        }
        FieldDeclaration found = resolveField(type.superName, field, descriptor, visiting);
        visiting.remove(name);
        return found;
    }

    private FieldDeclaration fieldDeclaration(FieldInsnNode access) {
        ClassNode symbolic = linked.get(access.owner);
        if (symbolic == null) throw new IllegalArgumentException("link the field owner " + access.owner);
        checkClassAccess(access.owner);
        FieldDeclaration resolved = resolveField(access.owner, access.name, access.desc, new HashSet<>());
        if (resolved == null) throw new IllegalArgumentException("link a matching field declaration");
        boolean isStatic = access.getOpcode() == Opcodes.GETSTATIC || access.getOpcode() == Opcodes.PUTSTATIC;
        int flags = resolved.field.access;
        if (((flags & Opcodes.ACC_STATIC) != 0) != isStatic)
            throw new IllegalArgumentException("field opcode does not match static/instance declaration");
        if ((flags & Opcodes.ACC_PUBLIC) == 0) {
            if ((flags & Opcodes.ACC_PRIVATE) != 0) checkPrivateAccess(resolved.owner);
            else if (!packageName(resolved.owner).equals(packageName(caller))
                    && ((flags & Opcodes.ACC_PROTECTED) == 0 || !subtype(caller, resolved.owner, new HashSet<>())))
                throw new IllegalArgumentException("inaccessible protected/package field " + resolved.owner);
        }
        return resolved;
    }

    public String fieldOwner(FieldInsnNode access) {
        return fieldDeclaration(access).owner;
    }

    void validateField(FieldInsnNode access, MethodNode method) {
        FieldDeclaration resolved = fieldDeclaration(access);
        boolean write = access.getOpcode() == Opcodes.PUTFIELD || access.getOpcode() == Opcodes.PUTSTATIC;
        if (write && (resolved.field.access & Opcodes.ACC_FINAL) != 0
                && (!resolved.owner.equals(caller) || !method.name.equals(
                    access.getOpcode() == Opcodes.PUTFIELD ? "<init>" : "<clinit>")))
            throw new IllegalArgumentException("final field writes require the declaring class initializer/constructor");
    }

    /** Null means no extra receiver restriction; otherwise the permitted runtime class IDs. */
    public Set<Integer> fieldReceiverIds(FieldInsnNode access) {
        FieldDeclaration resolved = fieldDeclaration(access);
        if ((resolved.field.access & Opcodes.ACC_PROTECTED) == 0
                || packageName(resolved.owner).equals(packageName(caller))) return null;
        Set<Integer> result = new LinkedHashSet<>();
        for (var entry : classIds.entrySet())
            if (subtype(entry.getKey(), caller, new HashSet<>())) result.add(entry.getValue());
        return result;
    }

    private Declaration declaration(String name, String method, String descriptor, Set<String> visited) {
        if (!visited.add(name)) return null;
        ClassNode type = linked.get(name);
        if (type == null) return null;
        for (MethodNode candidate : type.methods)
            if (candidate.name.equals(method) && candidate.desc.equals(descriptor))
                return new Declaration(name, candidate);
        return declaration(type.superName, method, descriptor, visited);
    }

    private boolean subtype(String name, String target, Set<String> visited) {
        if (Objects.equals(name, target)) return true;
        if (!visited.add(name)) return false;
        ClassNode type = linked.get(name);
        if (type == null) return false;
        if (subtype(type.superName, target, visited)) return true;
        for (String iface : type.interfaces) if (subtype(iface, target, visited)) return true;
        return false;
    }

    private void interfaceDeclarations(String name, String method, String descriptor,
                                       Set<String> visited, List<Declaration> result) {
        if (name == null || name.equals("java/lang/Object") || !visited.add(name)) return;
        ClassNode type = linked.get(name);
        if (type == null) throw new IllegalArgumentException("link the complete interface hierarchy: " + name);
        if ((type.access & Opcodes.ACC_INTERFACE) != 0) {
            for (MethodNode candidate : type.methods)
                if (candidate.name.equals(method) && candidate.desc.equals(descriptor)
                        && (candidate.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0)
                    result.add(new Declaration(name, candidate));
        }
        interfaceDeclarations(type.superName, method, descriptor, visited, result);
        for (String iface : type.interfaces) interfaceDeclarations(iface, method, descriptor, visited, result);
    }

    private Declaration interfaceMethod(String receiver, String method, String descriptor) {
        List<Declaration> declarations = new ArrayList<>();
        interfaceDeclarations(receiver, method, descriptor, new HashSet<>(), declarations);
        // An abstract redeclaration also suppresses an ancestor default (re-abstraction).
        List<Declaration> maximal = declarations.stream().filter(candidate -> declarations.stream().noneMatch(other ->
                !candidate.owner.equals(other.owner) && subtype(other.owner, candidate.owner, new HashSet<>())))
                .toList();
        List<Declaration> defaults = maximal.stream()
                .filter(candidate -> (candidate.method.access & Opcodes.ACC_ABSTRACT) == 0).toList();
        if (defaults.size() > 1)
            throw new DefaultConflict("conflicting maximally-specific interface defaults for " + receiver
                    + "." + method + descriptor + "; provide a public class override");
        if (defaults.size() == 1) return defaults.get(0);
        return maximal.isEmpty() ? null : maximal.get(0);
    }

    /** Resolve INVOKESTATIC without inheriting interface static methods. */
    public String staticTarget(MethodInsnNode call) {
        ClassNode symbolic = linked.get(call.owner);
        if (symbolic == null || ((symbolic.access & Opcodes.ACC_INTERFACE) != 0) != call.itf)
            throw new IllegalArgumentException("link the static owner with the matching class/interface reference kind");
        checkClassAccess(call.owner);
        if (call.name.startsWith("<"))
            throw new IllegalArgumentException("initializers cannot be invoked with INVOKESTATIC");
        Declaration resolved = null;
        Set<String> visited = new HashSet<>();
        String name = call.owner;
        while (name != null && !name.equals("java/lang/Object")) {
            ClassNode type = linked.get(name);
            if (type == null || !visited.add(name))
                throw new IllegalArgumentException("link a complete acyclic static method superclass chain: " + name);
            resolved = ownMethod(name, call.name, call.desc);
            if (resolved != null || call.itf) break;
            name = type.superName;
        }
        if (resolved == null)
            throw new IllegalArgumentException("link a matching static declaration; interface static methods are not inherited");
        if ((resolved.method.access & Opcodes.ACC_STATIC) == 0
                || (resolved.method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0)
            throw new IllegalArgumentException("static call requires a concrete non-native static declaration");
        checkMemberAccess(resolved); // Member visibility is identical; static calls have no receiver restriction.
        return TypeHelper.getMethodFunctionName(resolved.owner, call.name, call.desc);
    }

    private Declaration classSpecialDeclaration(String name, String method, String descriptor) {
        Set<String> visited = new HashSet<>();
        String current = name;
        while (current != null && !current.equals("java/lang/Object")) {
            ClassNode type = linked.get(current);
            if (type == null || !visited.add(current))
                throw new IllegalArgumentException("link a complete acyclic special method superclass chain: " + current);
            Declaration found = ownMethod(current, method, descriptor);
            if (found != null) return found;
            current = type.superName;
        }
        Declaration object = ownMethod(BytecodeObjects.OBJECT, method, descriptor);
        return object != null ? object : interfaceMethod(name, method, descriptor);
    }

    /** Object constructor intrinsic; reference kind must not bypass ordinary special-call checks. */
    public static boolean isObjectConstructor(MethodInsnNode call) {
        return call.getOpcode() == Opcodes.INVOKESPECIAL && !call.itf
                && call.owner.equals("java/lang/Object") && call.name.equals("<init>") && call.desc.equals("()V");
    }

    /** Class INVOKESPECIAL resolution, including exact constructors and ACC_SUPER selection. */
    public String specialTarget(MethodInsnNode call) {
        ClassNode owner = linked.get(call.owner), from = linked.get(caller);
        if (call.itf || owner == null || (owner.access & Opcodes.ACC_INTERFACE) != 0 || from == null)
            throw new IllegalArgumentException("link the special-call class owner and caller with a class reference");
        checkClassAccess(call.owner);
        boolean constructor = call.name.equals("<init>");
        if (call.name.startsWith("<") && !constructor)
            throw new IllegalArgumentException("class initializers cannot be invoked with INVOKESPECIAL");
        Declaration resolved = constructor ? ownMethod(call.owner, call.name, call.desc)
                : classSpecialDeclaration(call.owner, call.name, call.desc);
        if (resolved == null)
            throw new IllegalArgumentException("link a matching special declaration; constructors are not inherited");
        checkMemberAccess(resolved);
        if ((resolved.method.access & Opcodes.ACC_STATIC) != 0)
            throw new IllegalArgumentException("special call cannot resolve to a static declaration");
        if (!constructor) {
            boolean superclass = !caller.equals(call.owner) && subtype(from.superName, call.owner, new HashSet<>());
            if (!caller.equals(call.owner) && !superclass)
                throw new IllegalArgumentException("non-constructor class special calls require the caller or a superclass owner");
            if (superclass && ((from.access & Opcodes.ACC_SUPER) != 0 || (from.version & 0xffff) >= Opcodes.V1_8))
                resolved = classSpecialDeclaration(from.superName, call.name, call.desc);
        }
        if (resolved == null || (resolved.method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0)
            throw new IllegalArgumentException("special call requires a concrete non-native instance implementation");
        return TypeHelper.getMethodFunctionName(resolved.owner, call.name, call.desc);
    }

    public String interfaceSuperTarget(MethodInsnNode call) {
        ClassNode owner = linked.get(call.owner), from = linked.get(caller);
        if (!call.itf || owner == null || (owner.access & Opcodes.ACC_INTERFACE) == 0 || from == null)
            throw new IllegalArgumentException("link the interface-special owner and caller with an interface reference");
        checkClassAccess(call.owner);
        if (call.name.startsWith("<"))
            throw new IllegalArgumentException("interface special calls cannot name initializers");
        if (!caller.equals(call.owner) && !from.interfaces.contains(call.owner))
            throw new IllegalArgumentException("interface-special owner must be the caller interface or a direct superinterface");
        validateTypeHierarchy(call.owner, new HashSet<>(), new HashSet<>());
        Declaration target = ownMethod(call.owner, call.name, call.desc);
        if (target == null) target = interfaceMethod(call.owner, call.name, call.desc);
        if (target != null && (target.method.access & Opcodes.ACC_PRIVATE) != 0) {
            checkPrivateAccess(target.owner);
        }
        if (target == null || (target.method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE)) == 0
                || (target.method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE)) != 0)
            throw new IllegalArgumentException("interface-special calls require an accessible concrete default/private implementation");
        return TypeHelper.getMethodFunctionName(target.owner, call.name, call.desc);
    }

    private boolean crossPackageProtected(Declaration method) {
        return (method.method.access & Opcodes.ACC_PROTECTED) != 0 && caller != null
                && !packageName(method.owner).equals(packageName(caller));
    }

    private void checkMemberAccess(Declaration method) {
        if ((method.method.access & Opcodes.ACC_PUBLIC) != 0) return;
        if ((method.method.access & Opcodes.ACC_PRIVATE) != 0) {
            checkPrivateAccess(method.owner);
            return;
        }
        if (caller != null && packageName(caller).equals(packageName(method.owner))) return;
        if (crossPackageProtected(method) && subtype(caller, method.owner, new HashSet<>())) return;
        throw new IllegalArgumentException("inaccessible protected/package method " + method.owner
                + "; call from the declaring package or an eligible protected subclass");
    }

    private Declaration ownMethod(String name, String method, String descriptor) {
        ClassNode type = linked.get(name);
        if (type == null) return null;
        for (MethodNode candidate : type.methods)
            if (candidate.name.equals(method) && candidate.desc.equals(descriptor)) return new Declaration(name, candidate);
        return null;
    }

    /** Check declared non-private instance overrides even when no virtual call reaches them. */
    void validateOverrides() {
        validateOverrides(linked.get(caller));
    }

    void validateOverrides(ClassNode type) {
        if (type == null) return;
        for (MethodNode method : type.methods) {
            if (method.name.startsWith("<") || (method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0) continue;
            if (!type.name.equals("java/lang/Object") && FINAL_OBJECT_METHODS.contains(method.name + method.desc))
                throw BytecodeCompatibility.failure(type, method, null, "method override " + method.name + method.desc,
                        "cannot override final java/lang/Object method");
            if ((type.access & Opcodes.ACC_INTERFACE) != 0) continue;
            String parent = type.superName;
            while (parent != null && (!parent.equals("java/lang/Object") || exceptionRuntimeEnabled())) {
                ClassNode ancestor = linked.get(parent); // Hierarchy declarations have already been validated.
                Declaration inherited = ownMethod(parent, method.name, method.desc);
                if (inherited != null && (inherited.method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0) {
                    try { virtualImplementation(type.name, inherited); }
                    catch (IllegalArgumentException error) {
                        throw BytecodeCompatibility.failure(type, method, null,
                                "method override " + method.name + method.desc, error.getMessage());
                    }
                }
                parent = ancestor.superName;
            }
        }
    }

    private Declaration virtualImplementation(String receiver, Declaration resolved) {
        if ((linked.get(resolved.owner).access & Opcodes.ACC_INTERFACE) != 0) {
            // Private class methods do not implement or suppress an interface slot.
            String current = receiver;
            while (current != null && linked.containsKey(current)) {
                ClassNode type = linked.get(current);
                Declaration implementation = ownMethod(current, resolved.method.name, resolved.method.desc);
                if (implementation != null && (implementation.method.access & Opcodes.ACC_PRIVATE) == 0) return implementation;
                current = type.superName;
            }
            return interfaceMethod(receiver, resolved.method.name, resolved.method.desc);
        }
        List<String> path = new ArrayList<>();
        String current = receiver;
        while (!current.equals(resolved.owner)) {
            ClassNode type = linked.get(current);
            if (type == null || path.contains(current))
                throw new IllegalArgumentException("link the complete receiver superclass chain through " + resolved.owner);
            path.add(current);
            current = type.superName;
            if (current == null) throw new IllegalArgumentException("receiver does not extend " + resolved.owner);
        }
        Declaration selected = resolved;
        Collections.reverse(path);
        for (String name : path) {
            Declaration candidate = ownMethod(name, resolved.method.name, resolved.method.desc);
            if (candidate == null || (candidate.method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) != 0) continue;
            // A different-package same-named method does not override a package-private slot.
            // Once a same-package override widens access, subsequent overrides are transitive.
            if ((selected.method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) != 0
                    || packageName(selected.owner).equals(packageName(candidate.owner))) {
                if ((selected.method.access & Opcodes.ACC_FINAL) != 0)
                    throw new IllegalArgumentException("cannot override final instance method " + selected.owner
                            + "." + selected.method.name + selected.method.desc);
                int access = candidate.method.access;
                if ((selected.method.access & Opcodes.ACC_PUBLIC) != 0 && (access & Opcodes.ACC_PUBLIC) == 0
                        || (selected.method.access & Opcodes.ACC_PROTECTED) != 0
                        && (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED)) == 0)
                    throw new IllegalArgumentException("override reduces virtual method visibility in " + candidate.owner);
                selected = candidate;
            }
        }
        return selected;
    }

    public BytecodeExceptions.Implicit defaultSelectionFailure(MethodInsnNode call) {
        if (exceptionRuntimeEnabled() && call.owner.startsWith("[")
                && BytecodeObjects.objectMethod(call.name, call.desc)) return null;
        if (BytecodeArrays.isClone(call) || call.getOpcode() != Opcodes.INVOKEVIRTUAL && call.getOpcode() != Opcodes.INVOKEINTERFACE) return null;
        try {
            Declaration declared = declaration(call.owner, call.name, call.desc, new HashSet<>());
            if (declared == null) interfaceMethod(call.owner, call.name, call.desc);
            return null;
        } catch (DefaultConflict conflict) {
            if (!hasImplicitException(BytecodeExceptions.Implicit.LINKAGE)) throw conflict;
            return BytecodeExceptions.Implicit.LINKAGE;
        }
    }

    public Map<Integer, String> targets(MethodInsnNode call) {
        boolean arrayOwner = exceptionRuntimeEnabled() && call.owner.startsWith("[")
                && BytecodeObjects.objectMethod(call.name, call.desc) && !call.name.equals("clone") && !call.name.equals("finalize");
        String symbolicOwner = arrayOwner ? BytecodeObjects.OBJECT : call.owner;
        ClassNode owner = linked.get(symbolicOwner);
        boolean interfaceCall = call.getOpcode() == Opcodes.INVOKEINTERFACE;
        if (owner == null || ((owner.access & Opcodes.ACC_INTERFACE) != 0) != interfaceCall || call.itf != interfaceCall)
            throw new IllegalArgumentException("link the declaring class/interface with the matching invocation/reference kind");
        if (arrayOwner) typeTestIds(call.owner); else checkClassAccess(call.owner);
        if (call.name.startsWith("<"))
            throw new IllegalArgumentException("initializers cannot be invoked with virtual/interface instructions");
        Declaration declared = declaration(symbolicOwner, call.name, call.desc, new HashSet<>());
        if (defaultSelectionFailure(call) != null) return Map.of();
        if (declared == null) declared = interfaceMethod(call.owner, call.name, call.desc);
        if (declared == null || (declared.method.access & Opcodes.ACC_STATIC) != 0)
            throw new IllegalArgumentException("link a matching instance method in the declaring hierarchy");
        checkMemberAccess(declared);
        boolean privateCall = (declared.method.access & Opcodes.ACC_PRIVATE) != 0;
        Map<Integer, String> result = new LinkedHashMap<>();
        for (var entry : classIds.entrySet()) {
            ClassNode type = linked.get(entry.getKey());
            if (arrayOwner || (type.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE)) != 0
                    || !subtype(type.name, symbolicOwner, new HashSet<>())) continue;
            // The JVM protected receiver rule applies outside the declaring package.
            if (crossPackageProtected(declared) && !subtype(type.name, caller, new HashSet<>())) continue;
            // Private methods never participate in overriding, even on a subclass receiver.
            Declaration implementation;
            try { implementation = privateCall ? declared : virtualImplementation(type.name, declared); }
            catch (DefaultConflict conflict) {
                BytecodeExceptions.Implicit error = interfaceCall ? BytecodeExceptions.Implicit.ABSTRACT_METHOD : BytecodeExceptions.Implicit.LINKAGE;
                if (!hasImplicitException(error)) throw conflict;
                result.put(entry.getValue(), error.helper);
                continue;
            }
            BytecodeExceptions.Implicit failure = null;
            if (implementation == null || (implementation.method.access & Opcodes.ACC_ABSTRACT) != 0)
                failure = BytecodeExceptions.Implicit.ABSTRACT_METHOD;
            else if (interfaceCall && (implementation.method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0)
                failure = BytecodeExceptions.Implicit.ILLEGAL_ACCESS;
            if (failure != null && hasImplicitException(failure)) {
                result.put(entry.getValue(), failure.helper);
                continue;
            }
            if (!layouts.contains(type.name) || implementation == null
                    || (!privateCall && (declared.method.access & Opcodes.ACC_PUBLIC) != 0
                        && (implementation.method.access & Opcodes.ACC_PUBLIC) == 0)
                    || (implementation.method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE)) != 0)
                throw new IllegalArgumentException("link a complete hierarchy and accessible concrete class/default implementation for "
                        + type.name + "." + call.name + call.desc + "; implement abstract or re-abstracted methods");
            result.put(entry.getValue(), TypeHelper.getMethodFunctionName(implementation.owner, call.name, call.desc));
        }
        if (exceptionRuntimeEnabled() && BytecodeObjects.objectMethod(call.name, call.desc)
                && !call.name.equals("clone") && !call.name.equals("finalize") && (arrayOwner || call.owner.equals(BytecodeObjects.OBJECT)
                    || call.owner.equals("java/lang/Cloneable") || call.owner.equals("java/io/Serializable"))) {
            Set<Integer> arrays = arrayOwner ? typeTestIds(call.owner) : typeTestIds(BytecodeObjects.OBJECT);
            for (int id : arrays) if (id < 0)
                result.put(id, TypeHelper.getMethodFunctionName(BytecodeObjects.OBJECT, call.name, call.desc));
        }
        return result;
    }
}
