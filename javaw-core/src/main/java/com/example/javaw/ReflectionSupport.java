package com.example.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithModifiers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Module-linked source reflection: generated descriptors and ordinary, typed Wasm calls. */
final class ReflectionSupport {
    private record Member(int id, String kind, String owner, String name, String type,
                          List<String> parameters, int modifiers, boolean varargs, Node declaration) {}
    private final List<TypeDeclaration<?>> types;
    private final Map<String, TypeDeclaration<?>> declarations = new LinkedHashMap<>();
    private final List<Member> members = new ArrayList<>();
    private final Map<String, AnnotationDeclaration> annotations = new LinkedHashMap<>();
    private final Map<String,String> repeatableContainers = new LinkedHashMap<>();
    private final List<String> annotationObjects = new ArrayList<>();
    private final Map<String, RecordDeclaration> records = new LinkedHashMap<>();
    private final Set<String> descriptorTypes = new LinkedHashSet<>();

    private ReflectionSupport(List<TypeDeclaration<?>> types) {
        this.types=new ArrayList<>(types);
        types.forEach(t -> declarations.put(t.getNameAsString(),t));
        types.stream().filter(t -> t instanceof AnnotationDeclaration).forEach(t -> annotations.put(t.getNameAsString(),(AnnotationDeclaration)t));
    }

    static ReflectionSupport prepare(CompilationUnit cu) {
        for (var type:cu.findAll(com.github.javaparser.ast.type.ClassOrInterfaceType.class))
            if(Set.of("ClassLoader","URLClassLoader").contains(type.getNameAsString()))
                throw JavaSourcePreparation.error(type,"Unsupported dynamic class loading; link all source types before Wasm instantiation and use Class.forName");
        boolean needed=cu.findAll(MethodCallExpr.class).stream().anyMatch(c -> Set.of("getDeclaredFields","getDeclaredField","getFields","getField",
                "getDeclaredMethods","getDeclaredMethod","getMethods","getMethod","getDeclaredConstructors","getDeclaredConstructor",
                "isRecord","getRecordComponents","isAnnotation","isPrimitive","getConstructors","getConstructor","getDeclaredAnnotations","getAnnotations","getAnnotation","getDeclaredAnnotation","getAnnotationsByType","getDeclaredAnnotationsByType","isAnnotationPresent","forName").contains(c.getNameAsString()));
        if(!needed) return null;
        for(MethodCallExpr call:cu.findAll(MethodCallExpr.class))
            if(call.getNameAsString().equals("forName") && call.getArguments().size()!=1)
                throw JavaSourcePreparation.error(call,"Unsupported Class.forName overload; use the one-argument linked-module lookup (no runtime loader or deferred initialization)");
        LibrarySupport.add(cu,"java.lang.annotation", """
            enum RetentionPolicy { SOURCE, CLASS, RUNTIME }
            enum ElementType { TYPE, FIELD, METHOD, PARAMETER, CONSTRUCTOR, LOCAL_VARIABLE, ANNOTATION_TYPE, PACKAGE, TYPE_PARAMETER, TYPE_USE, MODULE, RECORD_COMPONENT }
            @Retention(RetentionPolicy.RUNTIME) @interface Retention { RetentionPolicy value(); }
            @Retention(RetentionPolicy.RUNTIME) @interface Target { ElementType[] value(); }
            @Retention(RetentionPolicy.RUNTIME) @interface Inherited {}
            @Retention(RetentionPolicy.RUNTIME) @interface Documented {}
            @Retention(RetentionPolicy.RUNTIME) @interface Repeatable { Class<? extends Annotation> value(); }
            """);
        LibrarySupport.add(cu,"java.lang", """
            @Retention(RetentionPolicy.RUNTIME) @Documented
            @interface Deprecated { String since() default ""; boolean forRemoval() default false; }
            """);
        List<TypeDeclaration<?>> originals=new ArrayList<>();
        cu.findAll(TypeDeclaration.class).forEach(originals::add);
        ReflectionSupport support=new ReflectionSupport(originals.stream().filter(t -> !t.getNameAsString().startsWith("__Javaw")).toList());
        for(String name:List.of("Field","Method","Constructor","AccessibleObject","AnnotatedElement","Annotation","Modifier","RecordComponent","__JavawReflection"))
            if(support.declarations.containsKey(name)) throw JavaSourcePreparation.error(support.declarations.get(name),
                    "Reserved reflection runtime type '"+name+"'; rename the source type");
        support.generate(cu);
        return support;
    }

    private void generate(CompilationUnit cu) {
        prepareRepeatableAnnotations(cu);
        for (var annotation : annotations.values()) {
            var iface=new ClassOrInterfaceDeclaration().setInterface(true).setName(annotation.getNameAsString());
            iface.addExtendedType("Annotation");
            annotation.getAnnotations().forEach(a -> iface.getAnnotations().add(a.clone()));
            for(BodyDeclaration<?> member:annotation.getMembers()) {
                if(member instanceof AnnotationMemberDeclaration method)
                    iface.addMember(StaticJavaParser.parseBodyDeclaration("public "+method.getTypeAsString()+" "+method.getNameAsString()+"();"));
                else if(member instanceof FieldDeclaration field) iface.addMember(field.clone());
            }
            if(annotation.containsData(JavaSourcePreparation.PACKAGE)) iface.setData(JavaSourcePreparation.PACKAGE,annotation.getData(JavaSourcePreparation.PACKAGE));
            types.set(types.indexOf(annotation),iface); declarations.put(iface.getNameAsString(),iface); annotation.replace(iface);
        }
        for(var type:new ArrayList<>(types)) if(type instanceof RecordDeclaration record) {
            records.put(record.getNameAsString(),record);
            var owner=RecordReflectionSupport.lower(record,this::annotationTarget);
            types.set(types.indexOf(record),owner); declarations.put(owner.getNameAsString(),owner); record.replace(owner);
        }
        for(TypeDeclaration<?> type:types) {
            descriptorTypes.add(type.getNameAsString());
            if(!(type instanceof ClassOrInterfaceDeclaration) && !(type instanceof EnumDeclaration)) continue;
            TypeDeclaration<?> owner=type;
            boolean iface=owner instanceof ClassOrInterfaceDeclaration c && c.isInterface();
            String name=owner.getNameAsString();
            if(owner instanceof EnumDeclaration enumeration) {
                for(var constant:enumeration.getEntries()) {
                    if(!constant.getClassBody().isEmpty()) throw JavaSourcePreparation.error(constant,
                            "Unsupported enum constant-specific class body in source reflection; move behavior to enum methods");
                    var member=new Member(members.size()+1,"Field",name,constant.getNameAsString(),name,List.of(),16409,false,constant);
                    members.add(member); fieldBridge(owner,member);
                }
            }
            for(FieldDeclaration field:owner.getFields()) for(VariableDeclarator variable:field.getVariables()) {
                String fieldType=erased(variable.getTypeAsString(),field);
                var member=new Member(members.size()+1,"Field",name,variable.getNameAsString(),fieldType,List.of(),
                        flags(field)|(iface?25:0),false,field);
                members.add(member); descriptorTypes.add(fieldType);
                fieldBridge(owner,member);
            }
            for(MethodDeclaration method:new ArrayList<>(owner.getMethods())) {
                if(method.getNameAsString().startsWith("__javaw_")) continue;
                var member=new Member(members.size()+1,"Method",name,method.getNameAsString(),erased(method.getTypeAsString(),method),
                        parameters(method),flags(method)|(iface&&!method.isPrivate()?1:0)|(iface&&method.getBody().isEmpty()&&!method.isStatic()?1024:0),method.getParameters().stream().anyMatch(Parameter::isVarArgs),method);
                members.add(member); descriptorTypes.add(member.type); descriptorTypes.addAll(member.parameters);
                executableBridge(owner,member,method);
            }
            if(!iface) {
                List<ConstructorDeclaration> constructors=new ArrayList<>(owner.getConstructors());
                if(constructors.isEmpty()) {
                    var ctor=StaticJavaParser.parseBodyDeclaration((owner instanceof EnumDeclaration?"private ":owner.isPublic()?"public ":"")+name+"() {}").asConstructorDeclaration();
                    // Explicit default construction preserves the same source access and initialization rules.
                    owner.addMember(ctor); constructors.add(ctor);
                }
                for(ConstructorDeclaration constructor:constructors) {
                    var constructorParameters=new ArrayList<>(parameters(constructor));
                    if(owner instanceof EnumDeclaration) { constructorParameters.add(0,"int"); constructorParameters.add(0,"String"); }
                    var member=new Member(members.size()+1,"Constructor",name,name,name,constructorParameters,owner instanceof EnumDeclaration?2:flags(constructor),
                            constructor.getParameters().stream().anyMatch(Parameter::isVarArgs),constructor);
                    members.add(member); descriptorTypes.addAll(member.parameters);
                    executableBridge(owner,member,constructor);
                }
            }
        }
        for(var type:types) if(type instanceof EnumDeclaration enumeration) {
            String name=type.getNameAsString();
            for(String source:List.of("public static "+name+"[] values();","public static "+name+" valueOf(String name);")) {
                var method=StaticJavaParser.parseBodyDeclaration(source).asMethodDeclaration();
                if(!enumeration.getMethodsByName(method.getNameAsString()).isEmpty()) continue;
                var member=new Member(members.size()+1,"Method",name,method.getNameAsString(),method.getTypeAsString(),parameters(method),9,false,method);
                members.add(member); descriptorTypes.add(member.type); descriptorTypes.addAll(member.parameters);
                executableBridge(type,member,method);
            }
        }
        StringBuilder helper=new StringBuilder("class __JavawReflection {");
        numericConversions(helper);
        helper.append("public static boolean __javaw_class_isRecord(Class<?> owner) { if(owner==null) throw new NullPointerException(); return ")
                .append(records.isEmpty()?"false":records.keySet().stream().map(n->"owner=="+n+".class").collect(Collectors.joining("||"))).append("; }");
        helper.append("public static RecordComponent[] __javaw_class_getRecordComponents(Class<?> owner) { if(owner==null) throw new NullPointerException();");
        for(var record:records.values()) {
            helper.append("if(owner==").append(record.getNameAsString()).append(".class) return new RecordComponent[]{");
            List<String> components=new ArrayList<>();
            for(var parameter:record.getParameters()) {
                var member=new Member(members.size()+1,"RecordComponent",record.getNameAsString(),parameter.getNameAsString(),erased(RecordReflectionSupport.componentType(parameter),parameter),List.of(),0,false,parameter);
                members.add(member);
                components.add("new RecordComponent("+member.id+","+quote(member.name)+","+classLiteral(member.owner)+","+classLiteral(member.type)+")");
            }
            helper.append(String.join(",",components)).append("};");
        }
        helper.append("return null; }");

        helper.append("public static Class<?> __javaw_reflect_load(String name) { if(name==null) throw new NullPointerException();");
        for(var type:types) {
            String pkg=type.containsData(JavaSourcePreparation.PACKAGE)?type.getData(JavaSourcePreparation.PACKAGE):"";
            if(!pkg.isEmpty()) helper.append("if(name.equals(").append(quote(pkg+"."+type.getNameAsString())).append(")) return ").append(type.getNameAsString()).append(".class;");
        }
        helper.append("Class<?> result=Class.forName(name); if(result==null) throw new ClassNotFoundException(name); return result; }");
        helper.append("public static boolean __javaw_class_isAnnotation(Class<?> owner) { if(owner==null) throw new NullPointerException(); return ")
                .append(annotations.keySet().stream().map(n->"owner=="+n+".class").collect(Collectors.joining("||"))).append("; }");
        helper.append("public static boolean __javaw_class_isPrimitive(Class<?> owner) { if(owner==null) throw new NullPointerException(); return ")
                .append(List.of("boolean","byte","short","char","int","long","float","double","void").stream().map(n->"owner=="+n+".class").collect(Collectors.joining("||"))).append("; }");
        helper.append("public static boolean __javaw_reflect_parameters(Class<?>[] actual,Class<?>[] expected) { int count=actual==null?0:actual.length; if(count!=expected.length) return false; for(int i=0;i<count;i++) if(actual[i]!=expected[i]) return false; return true; }");
        for(String kind:List.of("Field","Method","Constructor")) {
            String plural=kind.equals("Field")?"Fields":kind.equals("Method")?"Methods":"Constructors";
            helper.append("public static ").append(kind).append("[] __javaw_class_getDeclared").append(plural).append("(Class<?> owner) { if(owner==null) throw new NullPointerException();");
            for(var type:types) {
                String name=type.getNameAsString();
                helper.append("if(owner==").append(name).append(".class) return new ").append(kind).append("[]{")
                        .append(members.stream().filter(m -> m.kind.equals(kind)&&m.owner.equals(name)).map(this::descriptor).collect(Collectors.joining(","))).append("};");
            }
            helper.append("return new ").append(kind).append("[0]; }");
            helper.append("public static ").append(kind).append("[] __javaw_class_get").append(plural).append("(Class<?> owner) { if(owner==null) throw new NullPointerException();");
            for(var type:types) {
                helper.append("if(owner==").append(type.getNameAsString()).append(".class) return new ").append(kind).append("[]{")
                        .append(publicMembers(type,kind,new HashSet<>()).stream().map(this::descriptor).collect(Collectors.joining(","))).append("};");
            }
            helper.append("return new ").append(kind).append("[0]; }");
            for(boolean declared:List.of(true,false)) {
                String prefix=declared?"getDeclared":"get";
                helper.append("public static ").append(kind).append(" __javaw_class_").append(prefix).append(kind).append("(Class<?> owner");
                if(!kind.equals("Constructor")) helper.append(",String name");
                if(!kind.equals("Field")) helper.append(",Class<?>... parameters");
                helper.append(") {");
                if(!kind.equals("Constructor")) helper.append("if(name==null) throw new NullPointerException();");
                helper.append("for(").append(kind).append(" member:__javaw_class_").append(prefix).append(plural).append("(owner)) if(");
                if(!kind.equals("Constructor")) helper.append("member.getName().equals(name)");
                if(kind.equals("Method")) helper.append(" && ");
                if(!kind.equals("Field")) helper.append("__javaw_reflect_parameters(parameters,member.getParameterTypes())");
                helper.append(") return member; throw new ").append(kind.equals("Field")?"NoSuchFieldException":"NoSuchMethodException").append("(")
                        .append(kind.equals("Constructor")?"owner.getName()":"name").append("); }");
            }
        }
        for(String action:List.of("field_get","field_set","method_invoke","construct")) {
            String result=action.equals("field_set")?"void":"Object";
            helper.append("public static ").append(result).append(" __javaw_").append(action).append("(int id");
            if(!action.equals("construct")) helper.append(",Object receiver");
            if(action.equals("field_set")) helper.append(",Object value");
            if(action.equals("method_invoke")||action.equals("construct")) helper.append(",Object[] arguments");
            helper.append(") {");
            for(Member member:members) if(member.kind.equals(action.startsWith("field")?"Field":action.equals("construct")?"Constructor":"Method")) {
                helper.append("if(id==").append(member.id).append(") {");
                if(!result.equals("void")) helper.append("return ");
                helper.append(member.owner).append(".__javaw_bridge_").append(action).append("_").append(member.id).append("(");
                if(!action.equals("construct")) helper.append("receiver");
                if(action.equals("field_set")) helper.append(",value");
                if(action.equals("method_invoke")) helper.append(",arguments");
                if(action.equals("construct")) helper.append("arguments");
                helper.append(");"); if(result.equals("void")) helper.append("return;"); helper.append("}");
            }
            helper.append("throw new IllegalArgumentException(); }");
        }
        annotationMetadata(helper,cu);
        helper.append("public static String[] __javaw_reflect_type_names() { return new String[]{")
                .append(descriptorTypes.stream().map(ReflectionSupport::quote).collect(Collectors.joining(","))).append("}; }");
        helper.append("}");
        try(var input=ReflectionSupport.class.getResourceAsStream("/javaw/reflection.java")) {
            if(input==null) throw new IllegalStateException("Missing reflection runtime source");
            LibrarySupport.add(cu,"java.lang.reflect",new String(input.readAllBytes(),StandardCharsets.UTF_8));
        } catch(IOException error) { throw new IllegalStateException(error); }
        for (var type : cu.findAll(TypeDeclaration.class)) {
            String name=type.getNameAsString();
            if(name.equals("Annotation")) type.setData(JavaSourcePreparation.PACKAGE,"java.lang.annotation");
            if(Set.of("ReflectiveOperationException","NoSuchFieldException","NoSuchMethodException","IllegalAccessException","InstantiationException","ClassNotFoundException").contains(name))
                type.setData(JavaSourcePreparation.PACKAGE,"java.lang");
        }
        LibrarySupport.add(cu,"java.lang.reflect",helper.toString());
        for(String source:annotationObjects) LibrarySupport.add(cu,"java.lang.annotation",source);
    }

    static String erasedArrayType(String type,Node context) {
        return erased(type,context);
    }
    private static String erased(String type,Node context) {
        return GenericTypes.eraseVariables(type,context).replaceAll("<[^<>]*>", "");
    }
    private static List<String> parameters(CallableDeclaration<?> method) {
        return method.getParameters().stream().map(p -> erased(p.getTypeAsString(),method)+(p.isVarArgs()?"[]":"")).toList();
    }
    private static int flags(NodeWithModifiers<?> declaration) {
        int flags=0;
        for(var modifier:declaration.getModifiers()) flags|=switch(modifier.getKeyword()) {
            case PUBLIC->1;case PRIVATE->2;case PROTECTED->4;case STATIC->8;case FINAL->16;case SYNCHRONIZED->32;
            case VOLATILE->64;case TRANSIENT->128;case NATIVE->256;case ABSTRACT->1024;case STRICTFP->2048;default->0;
        };
        return flags;
    }
    private static String quote(String value) { return new StringLiteralExpr(value).toString(); }
    private String classLiteral(String type) {
        descriptorTypes.add(type);
        return type+".class";
    }
    private String descriptor(Member member) {
        String arguments=member.id+","+quote(member.name)+","+classLiteral(member.owner)+",";
        if(!member.kind.equals("Constructor")) arguments+=classLiteral(member.type)+",";
        if(!member.kind.equals("Field")) arguments+="new Class<?>[]{"+member.parameters.stream().map(this::classLiteral).collect(Collectors.joining(","))+"},";
        arguments+=member.modifiers;
        if(!member.kind.equals("Field")) arguments+=","+member.varargs;
        return "new "+member.kind+"("+arguments+")";
    }
    private List<Member> publicMembers(TypeDeclaration<?> type,String kind,Set<String> seen) {
        if(!seen.add(type.getNameAsString())) return List.of();
        List<Member> result=new ArrayList<>(members.stream().filter(m -> m.owner.equals(type.getNameAsString())&&m.kind.equals(kind)&&(m.modifiers&1)!=0).toList());
        if(!kind.equals("Constructor") && type instanceof ClassOrInterfaceDeclaration declaration) {
            var parents=new ArrayList<>(declaration.getExtendedTypes()); parents.addAll(declaration.getImplementedTypes());
            for(var parent:parents) {
                var target=declarations.get(parent.getNameAsString()); if(target==null) continue;
                for(Member inherited:publicMembers(target,kind,seen))
                    if(kind.equals("Field") || result.stream().noneMatch(m -> m.name.equals(inherited.name)&&m.parameters.equals(inherited.parameters))) result.add(inherited);
            }
        } else if(!kind.equals("Constructor") && type instanceof EnumDeclaration enumeration) {
            for(var parent:enumeration.getImplementedTypes()) {
                var target=declarations.get(parent.getNameAsString()); if(target==null) continue;
                for(Member inherited:publicMembers(target,kind,seen))
                    if(kind.equals("Field") || result.stream().noneMatch(m -> m.name.equals(inherited.name)&&m.parameters.equals(inherited.parameters))) result.add(inherited);
            }
        }
        return result;
    }

    private void fieldBridge(TypeDeclaration<?> owner,Member member) {
        String target=(member.modifiers&8)!=0?member.owner:"(("+member.owner+")receiver)";
        String check=receiverCheck(member);
        String get="public static Object __javaw_bridge_field_get_"+member.id+"(Object receiver) {"+check+"return "+box(target+"."+member.name,member.type)+"; }";
        owner.addMember(StaticJavaParser.parseBodyDeclaration(get));
        String set="public static void __javaw_bridge_field_set_"+member.id+"(Object receiver,Object value) {"+check;
        if((member.modifiers&16)!=0) set+="throw new IllegalAccessException();";
        else set+=conversion("value",member.type,"converted")+target+"."+member.name+"=converted;";
        owner.addMember(StaticJavaParser.parseBodyDeclaration(set+"}"));
    }
    private String receiverCheck(Member member) {
        if((member.modifiers&8)!=0) return "";
        return "if(receiver==null) throw new NullPointerException(); if(!(receiver instanceof "+member.owner+")) throw new IllegalArgumentException();";
    }
    private void executableBridge(TypeDeclaration<?> owner,Member member,CallableDeclaration<?> declaration) {
        boolean constructor=member.kind.equals("Constructor");
        String body="public static Object __javaw_bridge_"+(constructor?"construct":"method_invoke")+"_"+member.id+"("+(constructor?"":"Object receiver,")+"Object[] arguments) {";
        if(constructor && owner instanceof EnumDeclaration) body+="throw new IllegalArgumentException(\"Cannot reflectively create enum objects\");";
        else if(constructor && owner instanceof ClassOrInterfaceDeclaration c && c.isAbstract()) body+="throw new InstantiationException();";
        else {
            if(!constructor) body+=receiverCheck(member);
            body+="int count=arguments==null?0:arguments.length; if(count!="+member.parameters.size()+") throw new IllegalArgumentException();";
            for(int i=0;i<member.parameters.size();i++) body+=conversion("arguments["+i+"]",member.parameters.get(i),"p"+i);
            String arguments=java.util.stream.IntStream.range(0,member.parameters.size()).mapToObj(i->"p"+i).collect(Collectors.joining(","));
            String call=constructor?"new "+member.owner+"("+arguments+")":((member.modifiers&8)!=0?member.owner:"(("+member.owner+")receiver)")+"."+member.name+"("+arguments+")";
            String invocation=member.type.equals("void")?call+"; return null;":"return "+box(call,member.type)+";";
            if(owner instanceof EnumDeclaration && member.name.equals("valueOf") && member.parameters.equals(List.of("String")))
                invocation="if(p0==null) throw new NullPointerException(); "+member.owner+" result="+call+"; if(result==null) throw new IllegalArgumentException(); return result;";
            body+="try {"+invocation+"} catch(Throwable target) { throw new InvocationTargetException(target); }";
        }
        owner.addMember(StaticJavaParser.parseBodyDeclaration(body+"}"));
    }
    private static String box(String value,String type) {
        String wrapper=BoxingSupport.wrapper(type);
        return wrapper==null?value:wrapper+".valueOf("+value+")";
    }
    private String conversion(String value,String type,String local) {
        if(BoxingSupport.wrapper(type)!=null) return type+" "+local+"=__JavawReflection.__javaw_as_"+type+"("+value+");";
        if(type.endsWith("[]")) return "if("+value+"!=null && !__JavawReflection.__javaw_array_matches("+value+","+classLiteral(type)+")) throw new IllegalArgumentException(); "+type+" "+local+"=("+type+")"+value+";";
        String check=type.equals("Object")?"":type.equals("String")?"if("+value+"!=null && !__JavawReflection.__javaw_is_string("+value+")) throw new IllegalArgumentException();"
                :"if("+value+"!=null && !("+value+" instanceof "+type+")) throw new IllegalArgumentException();";
        return check+type+" "+local+"=("+type+")"+value+";";
    }
    private void numericConversions(StringBuilder source) {
        Map<String,List<String>> allowed=new LinkedHashMap<>();
        allowed.put("boolean",List.of("Boolean")); allowed.put("byte",List.of("Byte")); allowed.put("short",List.of("Byte","Short")); allowed.put("char",List.of("Character"));
        allowed.put("int",List.of("Byte","Short","Character","Integer")); allowed.put("long",List.of("Byte","Short","Character","Integer","Long"));
        allowed.put("float",List.of("Byte","Short","Character","Integer","Long","Float")); allowed.put("double",List.of("Byte","Short","Character","Integer","Long","Float","Double"));
        for(var entry:allowed.entrySet()) {
            source.append("public static ").append(entry.getKey()).append(" __javaw_as_").append(entry.getKey()).append("(Object value) {");
            for(String wrapper:entry.getValue()) source.append("if(value instanceof ").append(wrapper).append(") return ((").append(wrapper).append(")value).")
                    .append(BoxingSupport.primitive(wrapper)).append("Value();");
            source.append("throw new IllegalArgumentException(); }");
        }
        source.append("public static native boolean __javaw_is_string(Object value);");
        source.append("public static native boolean __javaw_array_matches(Object value,Class<?> expected);");
    }

    private void annotationMetadata(StringBuilder helper,CompilationUnit cu) {
        helper.append("public static Annotation[] __javaw_member_annotations(int id) {");
        for(Member member:members) helper.append("if(id==").append(member.id).append(") return ").append(annotationArray(member.kind.equals("RecordComponent")?declaredAnnotations(member.declaration).stream().filter(a -> annotationTarget(a,"RECORD_COMPONENT")).toList():declaredAnnotations(member.declaration))).append(";");
        helper.append("return new Annotation[0]; }");
        helper.append("public static Annotation[] __javaw_class_getDeclaredAnnotations(Class<?> owner) { if(owner==null) throw new NullPointerException();");
        for(var type:types) helper.append("if(owner==").append(type.getNameAsString()).append(".class) return ").append(annotationArray(type)).append(";");
        helper.append("return new Annotation[0]; }");
        helper.append("public static Annotation[] __javaw_class_getAnnotations(Class<?> owner) { if(owner==null) throw new NullPointerException();");
        for(var type:types) helper.append("if(owner==").append(type.getNameAsString()).append(".class) return ").append(annotationArray(inheritedAnnotations(type,new HashSet<>()))).append(";");
        helper.append("return new Annotation[0]; }");
        helper.append("public static <A extends Annotation> A __javaw_class_getAnnotation(Class<?> owner,Class<A> type) { if(type==null) throw new NullPointerException(); for(Annotation annotation:__javaw_class_getAnnotations(owner)) if(annotation.annotationType()==type) return (A)annotation; return null; }");
        helper.append("public static boolean __javaw_class_isAnnotationPresent(Class<?> owner,Class<? extends Annotation> type) { return __javaw_class_getAnnotation(owner,type)!=null; }");
        helper.append("public static <A extends Annotation> A __javaw_class_getDeclaredAnnotation(Class<?> owner,Class<A> type) { if(type==null) throw new NullPointerException(); for(Annotation annotation:__javaw_class_getDeclaredAnnotations(owner)) if(annotation.annotationType()==type) return (A)annotation; return null; }");
        annotationQueries(helper);

    }
    private void annotationQueries(StringBuilder helper) {
        helper.append("public static <A extends Annotation> A[] __javaw_annotations_by_type(Annotation[] source,Class<A> type) { if(type==null) throw new NullPointerException();");
        for(String name:annotations.keySet()) {
            String container=repeatableContainers.get(name);
            helper.append("if(type==").append(name).append(".class) { int count=0; for(Annotation annotation:source) { if(annotation.annotationType()==")
                    .append(name).append(".class) count++;");
            if(container!=null) helper.append("else if(annotation.annotationType()==").append(container).append(".class) count+=((").append(container).append(")annotation).value().length;");
            helper.append("} ").append(name).append("[] result=new ").append(name).append("[count]; int index=0; for(Annotation annotation:source) { if(annotation.annotationType()==")
                    .append(name).append(".class) result[index++]=(").append(name).append(")annotation;");
            if(container!=null) helper.append("else if(annotation.annotationType()==").append(container).append(".class) for(").append(name).append(" value:((").append(container).append(")annotation).value()) result[index++]=value;");
            helper.append("} return (A[])result; }");
        }
        helper.append("return (A[])new Annotation[0]; }");
        helper.append("public static <A extends Annotation> A[] __javaw_class_getDeclaredAnnotationsByType(Class<?> owner,Class<A> type) { return __javaw_annotations_by_type(__javaw_class_getDeclaredAnnotations(owner),type); }");
        helper.append("public static <A extends Annotation> A[] __javaw_class_getAnnotationsByType(Class<?> owner,Class<A> type) { A[] result=__javaw_class_getDeclaredAnnotationsByType(owner,type); if(result.length!=0) return result;");
        for(var schema:annotations.values()) if(hasMetaAnnotation(schema,"Inherited")) {
            helper.append("if(type==").append(schema.getNameAsString()).append(".class) {");
            for(var declaration:types) if(declaration instanceof ClassOrInterfaceDeclaration type && !type.isInterface() && !type.getExtendedTypes().isEmpty())
                helper.append("if(owner==").append(type.getNameAsString()).append(".class) return __javaw_class_getAnnotationsByType(").append(type.getExtendedTypes(0).getNameAsString()).append(".class,type);");
            helper.append("}");
        }
        helper.append("return result; }");
    }

    private static boolean hasMetaAnnotation(AnnotationDeclaration schema,String name) {
        return schema.getAnnotations().stream().anyMatch(a -> a.getName().getIdentifier().equals(name));
    }

    private static int retentionRank(AnnotationDeclaration schema) {
        var retention=schema.getAnnotations().stream().filter(a -> a.getName().getIdentifier().equals("Retention")).findFirst();
        if(retention.isEmpty()) return 1;
        String value=retention.get().toString();
        return value.contains("RUNTIME")?2:value.contains("SOURCE")?0:1;
    }

    private void prepareRepeatableAnnotations(CompilationUnit cu) {
        for(var schema:annotations.values()) {
            var repeatable=schema.getAnnotations().stream().filter(a -> a.getName().getIdentifier().equals("Repeatable")).findFirst();
            if(repeatable.isEmpty()) continue;
            var literal=repeatable.get().findFirst(ClassExpr.class).orElseThrow(() -> JavaSourcePreparation.error(repeatable.get(),"Repeatable annotation requires a linked container class literal"));
            String name=schema.getNameAsString(),container=literal.getType().asString();
            var target=annotations.get(container);
            if(target==null) throw JavaSourcePreparation.error(repeatable.get(),"Repeatable annotation container is not a linked annotation: "+container);
            var value=target.getMembers().stream().filter(m -> m instanceof AnnotationMemberDeclaration a && a.getNameAsString().equals("value"))
                    .map(m -> (AnnotationMemberDeclaration)m).findFirst();
            if(value.isEmpty() || !value.get().getTypeAsString().equals(name+"[]"))
                throw JavaSourcePreparation.error(repeatable.get(),"Repeatable container value() must return "+name+"[]");
            for(var member:target.getMembers()) if(member instanceof AnnotationMemberDeclaration a && !a.getNameAsString().equals("value") && a.getDefaultValue().isEmpty())
                throw JavaSourcePreparation.error(a,"Repeatable container members other than value() require defaults");
            if(hasMetaAnnotation(schema,"Inherited") && !hasMetaAnnotation(target,"Inherited"))
                throw JavaSourcePreparation.error(repeatable.get(),"An inherited repeatable annotation requires an inherited container");
            if(retentionRank(target)<retentionRank(schema))
                throw JavaSourcePreparation.error(repeatable.get(),"Repeatable annotation requires a container with at least the same retention; runtime annotations need a runtime-retained container");
            if(hasMetaAnnotation(schema,"Documented") && !hasMetaAnnotation(target,"Documented"))
                throw JavaSourcePreparation.error(repeatable.get(),"A documented repeatable annotation requires a documented container");
            repeatableContainers.put(name,container);
        }
        for(String name:repeatableContainers.keySet()) {
            var seen=new HashSet<String>(); String next=name;
            while(next!=null) {
                if(!seen.add(next)) throw JavaSourcePreparation.error(annotations.get(name),"Cyclic repeatable annotation containers are unsupported");
                next=repeatableContainers.get(next);
            }
        }
        for(Node node:cu.findAll(Node.class)) if(node instanceof NodeWithAnnotations<?> annotated) {
            Map<String,List<AnnotationExpr>> groups=annotated.getAnnotations().stream().filter(a -> annotations.containsKey(a.getName().getIdentifier()))
                    .collect(Collectors.groupingBy(a -> a.getName().getIdentifier(),LinkedHashMap::new,Collectors.toList()));
            var replacements=new LinkedHashMap<String,AnnotationExpr>();
            for(var group:groups.entrySet()) if(group.getValue().size()>1) {
                String container=repeatableContainers.get(group.getKey());
                if(container==null) throw JavaSourcePreparation.error(group.getValue().get(1),"Repeated runtime annotation requires @Repeatable and a linked container");
                if(groups.containsKey(container)) throw JavaSourcePreparation.error(group.getValue().get(1),"Repeated annotations cannot be combined with their explicit container");
                var values=new NodeList<Expression>();
                group.getValue().forEach(a -> values.add(a.clone()));
                replacements.put(group.getKey(),new SingleMemberAnnotationExpr(new Name(container),new ArrayInitializerExpr(values)));
            }
            if(replacements.isEmpty()) continue;
            var normalized=new NodeList<AnnotationExpr>(); var emitted=new HashSet<String>();
            for(var use:annotated.getAnnotations()) {
                String name=use.getName().getIdentifier();
                if(!replacements.containsKey(name)) normalized.add(use);
                else if(emitted.add(name)) normalized.add(replacements.get(name));
            }
            annotated.setAnnotations(normalized);
        }
    }

    private List<AnnotationExpr> inheritedAnnotations(TypeDeclaration<?> type,Set<String> seen) {
        if(!seen.add(type.getNameAsString())) return List.of();
        var result=new ArrayList<>(declaredAnnotations(type));
        if(type instanceof ClassOrInterfaceDeclaration declaration && !declaration.isInterface() && !declaration.getExtendedTypes().isEmpty()) {
            var parent=declarations.get(declaration.getExtendedTypes(0).getNameAsString());
            if(parent!=null) for(var annotation:inheritedAnnotations(parent,seen)) {
                String name=annotation.getName().getIdentifier();
                if(result.stream().anyMatch(a -> a.getName().getIdentifier().equals(name))) continue;
                if(annotations.get(name).getAnnotations().stream().anyMatch(a -> a.getName().getIdentifier().equals("Inherited"))) result.add(annotation);
            }
        }
        return result;
    }

    private boolean annotationTarget(AnnotationExpr use,String target) {
        var schema=annotations.get(use.getName().getIdentifier());
        if(schema==null) return false;
        var restriction=schema.getAnnotations().stream().filter(a -> a.getName().getIdentifier().equals("Target")).findFirst();
        if(restriction.isEmpty()) return true;
        return restriction.get().findAll(NameExpr.class).stream().anyMatch(n -> n.getNameAsString().equals(target))
                || restriction.get().findAll(FieldAccessExpr.class).stream().anyMatch(n -> n.getNameAsString().equals(target));
    }

    private boolean runtimeAnnotation(AnnotationExpr use) {
        var declaration=annotations.get(use.getName().getIdentifier());
        return declaration!=null && declaration.getAnnotations().stream().anyMatch(a -> a.getName().getIdentifier().equals("Retention")&&a.toString().contains("RUNTIME"));
    }
    private List<AnnotationExpr> declaredAnnotations(Node node) {
        if(!(node instanceof NodeWithAnnotations<?> annotated)) return List.of();
        return annotated.getAnnotations().stream().filter(this::runtimeAnnotation).toList();
    }
    private String annotationArray(Node node) { return annotationArray(declaredAnnotations(node)); }
    private String annotationArray(List<AnnotationExpr> uses) {
        var names=new HashSet<String>();
        List<String> objects=new ArrayList<>();
        for(var use:uses) {
            if(!names.add(use.getName().getIdentifier())) throw JavaSourcePreparation.error(use,
                    "Unsupported repeated runtime annotation; use an explicit container annotation");
            objects.add(annotationObject(use));
        }
        return "new Annotation[]{"+String.join(",",objects)+"}";
    }
    private String annotationObject(AnnotationExpr use) {
        String name=use.getName().getIdentifier(); var declaration=annotations.get(name);
        if(declaration==null) throw JavaSourcePreparation.error(use,"Runtime annotation declaration is not linked: "+name);
        int index=annotationObjects.size(); annotationObjects.add("");
        String generated="__JavawAnnotation_"+index;
        StringBuilder object=new StringBuilder("class "+generated+" implements "+name+" { public Class<?> annotationType() { return "+name+".class; }");
        Map<String,Expression> values=new LinkedHashMap<>();
        if(use instanceof SingleMemberAnnotationExpr single) values.put("value",single.getMemberValue());
        if(use instanceof NormalAnnotationExpr normal) normal.getPairs().forEach(p -> values.put(p.getNameAsString(),p.getValue()));
        StringBuilder equals=new StringBuilder("public boolean equals(Object other) { if(!(other instanceof "+name+")) return false; "+name+" that=("+name+")other;");
        StringBuilder hash=new StringBuilder("public int hashCode() { int hash=0;");
        for(BodyDeclaration<?> member:declaration.getMembers()) if(member instanceof AnnotationMemberDeclaration method) {
            Expression value=values.getOrDefault(method.getNameAsString(),method.getDefaultValue().orElse(null));
            if(value==null) throw JavaSourcePreparation.error(use,"Missing runtime annotation value '"+method.getNameAsString()+"'");
            String type=method.getTypeAsString(), property=method.getNameAsString();
            object.append("public ").append(type).append(" ").append(property).append("() { return ").append(annotationValue(value,type)).append("; }");
            if(method.getType().isArrayType()) {
                String element=method.getType().asArrayType().getComponentType().asString();
                equals.append(type).append(" a_").append(property).append("=").append(property).append("(); ")
                        .append(type).append(" b_").append(property).append("=that.").append(property).append("();")
                        .append("if(a_").append(property).append(".length!=b_").append(property).append(".length) return false;")
                        .append("for(int i=0;i<a_").append(property).append(".length;i++) if(!(")
                        .append(annotationEqual("a_"+property+"[i]","b_"+property+"[i]",element)).append(")) return false;");
                hash.append("int hash_").append(property).append("=1; for(").append(element).append(" element:").append(property).append("()) hash_")
                        .append(property).append("=31*hash_").append(property).append("+").append(annotationHash("element",element)).append(";")
                        .append("hash+=(127*").append(quote(property)).append(".hashCode())^hash_").append(property).append(";");
            } else {
                equals.append("if(!(").append(annotationEqual(property+"()","that."+property+"()",type)).append(")) return false;");
                hash.append("hash+=(127*").append(quote(property)).append(".hashCode())^").append(annotationHash(property+"()",type)).append(";");
            }
        }
        for(String property:values.keySet()) if(declaration.getMembers().stream().noneMatch(m -> m instanceof AnnotationMemberDeclaration a&&a.getNameAsString().equals(property)))
            throw JavaSourcePreparation.error(use,"Unknown runtime annotation value '"+property+"'");
        object.append(equals).append("return true; }").append(hash).append("return hash; }");
        // Values remain typed; array formatting is deliberately left to callers.
        object.append("public String toString() { return ").append(quote("@"+name)).append("; }");
        object.append("}"); annotationObjects.set(index,object.toString()); return "new "+generated+"()";
    }
    private String annotationValue(Expression value,String type) {
        if(value.isAnnotationExpr()) return annotationObject(value.asAnnotationExpr());
        if(type.endsWith("[]")) {
            String element=type.substring(0,type.length()-2);
            List<Expression> entries=value.isArrayInitializerExpr()?value.asArrayInitializerExpr().getValues():List.of(value);
            return "new "+element+"[]{"+entries.stream().map(v -> annotationValue(v,element)).collect(Collectors.joining(","))+"}";
        }
        return value.toString();
    }
    static String annotationEqual(String a,String b,String type) {
        if(type.equals("float")) return "Float.floatToIntBits("+a+")==Float.floatToIntBits("+b+")";
        if(type.equals("double")) return "Double.doubleToLongBits("+a+")==Double.doubleToLongBits("+b+")";
        if(BoxingSupport.wrapper(type)!=null || type.startsWith("Class")) return a+"=="+b;
        return a+".equals("+b+")";
    }
    private static String annotationHash(String value,String type) {
        String wrapper=BoxingSupport.wrapper(type);
        return (wrapper==null?value:wrapper+".valueOf("+value+")")+".hashCode()";
    }

    void finish(CompilationUnit cu) {
        var helper=cu.getClassByName("__JavawReflection").orElseThrow();
        var load=helper.getMethodsByName("__javaw_reflect_load").get(0).getBody().orElseThrow();
        for(var type:cu.findAll(TypeDeclaration.class)) {
            String name=type.getNameAsString();
            String pkg=type.containsData(JavaSourcePreparation.PACKAGE)?type.getData(JavaSourcePreparation.PACKAGE):"";
            if(pkg.isEmpty()) continue;
            load.addStatement(0,StaticJavaParser.parseStatement("if(name.equals("+quote(pkg+"."+name)+")) return "+name+".class;"));
        }
        // Null lookup must fail before evaluating String.equals.
        load.addStatement(0,StaticJavaParser.parseStatement("if(name==null) throw new NullPointerException();"));
    }

    void register(ClassRegistry registry) {
        var helper=registry.getClass("__JavawReflection");
        for(MethodModel method:helper.getMethods().values()) if(method.getName().startsWith("__javaw_class_")) {
            var parameters=method.getParamTypeNames().subList(1,method.getParamTypeNames().size());
            var model=new MethodModel(method.getName().substring("__javaw_class_".length()),"Class",method.getFunctionName(),method.getReturnTypeName(),method.getReturnWasmType(),
                    method.getParamNames().subList(1,method.getParamNames().size()),parameters,method.getParamWasmTypes().subList(1,method.getParamWasmTypes().size()),false,false);
            var declaration=(MethodDeclaration)method.getDeclaration().clone(); declaration.getParameters().remove(0); declaration.setStatic(false);
            model.setDeclaration(declaration); model.setVirtual(false); registry.getClass("Class").addMethod(model);
        }
    }
}
