package dastec.one.javaw.bytecode;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.util.*;

/** Closed-world adapters for explicitly recognized JDK bootstrap protocols. */
final class BytecodeDynamic implements Opcodes {
    private static final String CONCAT = "java/lang/invoke/StringConcatFactory";
    private static final String LAMBDA = "java/lang/invoke/LambdaMetafactory";
    private static final String LOOKUP = "Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;";
    private static final String CALLSITE = "Ljava/lang/invoke/CallSite;";

    static void normalize(List<ClassNode> classes) {
        Set<String> names = new HashSet<>();
        Map<String,ClassNode> linked = new HashMap<>();
        Map<String,Integer> versions=new HashMap<>();
        for (ClassNode type : classes) {
            // Never let adapter metadata upgrades relax the original declaration rules.
            BytecodeDeclarations.validate(type);
            names.add(type.name); linked.put(type.name,type);versions.put(type.name,type.version&0xffff);
        }
        List<ClassNode> generated = new ArrayList<>();
        for (ClassNode owner : classes) {
            int ordinal=0;
            Map<Object,MethodInsnNode> constants=new HashMap<>();
            for (MethodNode method : List.copyOf(owner.methods)) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof InvokeDynamicInsnNode) &&
                            !(instruction instanceof LdcInsnNode ldc && (ldc.cst instanceof ConstantDynamic || ldc.cst instanceof Type type && type.getSort()==Type.METHOD))) continue;
                    try {
                        String name=owner.name+"$$javaw$dynamic$"+ordinal++;
                        if (!names.add(name)) throw new IllegalArgumentException("reserved adapter name collision: "+name);
                        ClassNode adapter=type(name);adapter.sourceFile=owner.sourceFile;
                        int generatedStart=generated.size();
                        String descriptor;
                        if (instruction instanceof InvokeDynamicInsnNode dynamic) {
                            descriptor=dynamic.desc;
                            if(versions.get(owner.name)<V1_7)throw new IllegalArgumentException("invokedynamic requires Java 7+ class files");
                            if(!BytecodeDeclarations.validMemberName(dynamic.name,true) || dynamic.name.startsWith("<"))
                                throw new IllegalArgumentException("invalid invokedynamic name "+dynamic.name);
                            if(!BytecodeDeclarations.validDescriptor(descriptor,true))throw new IllegalArgumentException("invalid invokedynamic descriptor "+descriptor);
                            if(dynamic.bsm.getTag()!=H_INVOKESTATIC || dynamic.bsm.isInterface())
                                throw new IllegalArgumentException("bootstrap must be a static JDK factory");
                            if(dynamic.bsm.getOwner().equals(CONCAT)) concat(adapter,dynamic);
                            else if(dynamic.bsm.getOwner().equals("java/lang/runtime/ObjectMethods")) record(adapter,owner,dynamic,linked);
                            else if(dynamic.bsm.getOwner().equals(LAMBDA)) lambda(adapter,owner,dynamic,linked,generated,names);
                            else throw new IllegalArgumentException("unsupported bootstrap "+dynamic.bsm);
                        } else if(((LdcInsnNode)instruction).cst instanceof Type methodType) {
                            if(versions.get(owner.name)<V1_7)throw new IllegalArgumentException("method-type constants require Java 7+ class files");
                            if(!BytecodeDeclarations.validDescriptor(methodType.getDescriptor(),true))throw new IllegalArgumentException("invalid method-type constant descriptor");
                            MethodInsnNode existing=constants.get(methodType);
                            if(existing!=null) {
                                method.instructions.set(instruction,new MethodInsnNode(INVOKESTATIC,existing.owner,existing.name,existing.desc,false));
                                continue;
                            }
                            descriptor="()Ljava/lang/invoke/MethodType;";
                            methodType(adapter,owner,methodType,linked);
                        } else {
                            ConstantDynamic constant=(ConstantDynamic)((LdcInsnNode)instruction).cst;
                            if(versions.get(owner.name)<V11)throw new IllegalArgumentException("constant-dynamic requires Java 11+ class files");
                            if(!BytecodeDeclarations.validMemberName(constant.getName(),false))
                                throw new IllegalArgumentException("invalid constant-dynamic name "+constant.getName());
                            MethodInsnNode existing=constants.get(constant);
                            if(existing!=null) {
                                method.instructions.set(instruction,new MethodInsnNode(INVOKESTATIC,existing.owner,existing.name,existing.desc,false));
                                continue;
                            }
                            if(!BytecodeDeclarations.validDescriptor(constant.getDescriptor(),false))throw new IllegalArgumentException("invalid constant-dynamic descriptor "+constant.getDescriptor());
                            descriptor="()"+constant.getDescriptor();
                            constant(adapter,owner,constant,linked);
                        }
                        // Keep the source line of the original bootstrap on adapter failure paths.
                        int sourceLine=-1;
                        for(AbstractInsnNode previous=instruction.getPrevious();previous!=null;previous=previous.getPrevious())
                            if(previous instanceof LineNumberNode line){sourceLine=line.line;break;}
                        if(sourceLine>=0) {
                            sourceLine(adapter,sourceLine);
                            for(int i=generatedStart;i<generated.size();i++)sourceLine(generated.get(i),sourceLine);
                        }
                        MethodInsnNode call=new MethodInsnNode(INVOKESTATIC,name,"link",descriptor,false);
                        method.instructions.set(instruction,call);
                        if(instruction instanceof LdcInsnNode ldc)constants.put(ldc.cst,call);
                        generated.add(adapter);
                    } catch (IllegalArgumentException error) {
                        throw BytecodeCompatibility.failure(owner,method,instruction,"dynamic bootstrap",error.getMessage());
                    }
                }
            }
        }
        classes.addAll(generated);
    }

    private static void methodType(ClassNode adapter,ClassNode caller,Type signature,Map<String,ClassNode> linked) {
        if(argsSlots(signature.getArgumentTypes())>255)throw new IllegalArgumentException("method-type constant exceeds 255 parameter slots");
        BytecodeDispatch dispatch=new BytecodeDispatch(linked,caller.name);
        List<Type> types=new ArrayList<>(List.of(signature.getArgumentTypes()));types.add(signature.getReturnType());
        for(Type type:types)if(reference(type))BytecodeClasses.validateLiteral(type,dispatch);
        if(caller.nestHostClass==null || linked.containsKey(caller.nestHostClass))nest(adapter,caller,linked);
        String descriptor="Ljava/lang/invoke/MethodType;";
        adapter.fields.add(new FieldNode(ACC_PRIVATE|ACC_STATIC,"value",descriptor,null,null));
        MethodNode code=method(adapter,ACC_PUBLIC|ACC_STATIC,"link","()"+descriptor);
        Label ready=new Label();code.visitFieldInsn(GETSTATIC,adapter.name,"value",descriptor);code.visitInsn(DUP);
        code.visitJumpInsn(IFNONNULL,ready);code.visitInsn(POP);
        mirror(code,signature.getReturnType());
        Type[] parameters=signature.getArgumentTypes();code.visitLdcInsn(parameters.length);code.visitTypeInsn(ANEWARRAY,"java/lang/Class");
        for(int i=0;i<parameters.length;i++) {
            code.visitInsn(DUP);code.visitLdcInsn(i);mirror(code,parameters[i]);code.visitInsn(AASTORE);
        }
        call(code,INVOKESTATIC,"java/lang/invoke/MethodType","methodType","(Ljava/lang/Class;[Ljava/lang/Class;)"+descriptor);
        code.visitInsn(DUP);code.visitFieldInsn(PUTSTATIC,adapter.name,"value",descriptor);
        code.visitLabel(ready);code.visitFrame(F_SAME1,0,null,1,new Object[]{"java/lang/invoke/MethodType"});code.visitInsn(ARETURN);finish(code,0);
    }
    private static void mirror(MethodVisitor code,Type type) {
        if(reference(type))code.visitLdcInsn(type);
        else {
            code.visitLdcInsn(BytecodeClasses.primitiveTokens().get(type.getDescriptor()));
            call(code,INVOKESTATIC,"javaw/runtime/ExceptionSupport","classMirror","(I)Ljava/lang/Class;");
        }
    }

    private static void sourceLine(ClassNode type,int line) {
        for(MethodNode method:type.methods) {
            LabelNode start=new LabelNode();
            method.instructions.insert(new LineNumberNode(line,start));
            method.instructions.insert(start);
        }
    }

    static ClassNode type(String name) {
        ClassNode type=new ClassNode();type.version=V17;type.access=ACC_PUBLIC|ACC_FINAL|ACC_SUPER|ACC_SYNTHETIC;
        type.name=name;type.superName="java/lang/Object";return type;
    }
    static MethodNode method(ClassNode owner,int flags,String name,String descriptor) {
        MethodNode method=new MethodNode(flags,name,descriptor,null,null);owner.methods.add(method);return method;
    }
    static void finish(MethodNode method,int locals) {method.maxStack=512;method.maxLocals=locals;}
    private static boolean reference(Type type) {return type.getSort()==Type.OBJECT || type.getSort()==Type.ARRAY;}
    static void call(MethodVisitor code,int opcode,String owner,String name,String descriptor) {
        code.visitMethodInsn(opcode,owner,name,descriptor,opcode==INVOKEINTERFACE);
    }
    private static void convertString(MethodVisitor code,Type type) {
        String descriptor=switch(type.getSort()) {
            case Type.BOOLEAN -> "Z";case Type.CHAR -> "C";case Type.BYTE,Type.SHORT,Type.INT -> "I";
            case Type.LONG -> "J";case Type.FLOAT -> "F";case Type.DOUBLE -> "D";case Type.OBJECT,Type.ARRAY -> "Ljava/lang/Object;";
            default -> throw new IllegalArgumentException("string concatenation conversion unsupported: "+type);
        };
        call(code,INVOKESTATIC,"java/lang/String","valueOf","("+descriptor+")Ljava/lang/String;");
    }

    private static void concat(ClassNode owner,InvokeDynamicInsnNode site) {
        boolean constants=site.bsm.getName().equals("makeConcatWithConstants");
        String expected="("+LOOKUP+(constants?"Ljava/lang/String;[Ljava/lang/Object;":"")+")"+CALLSITE;
        if ((!constants && !site.bsm.getName().equals("makeConcat")) || !site.bsm.getDesc().equals(expected))
            throw new IllegalArgumentException("unsupported string concatenation bootstrap "+site.bsm);
        Type[] args=Type.getArgumentTypes(site.desc);
        String recipe;
        if(constants) {
            if(site.bsmArgs.length==0) { failed(owner,site.desc,"java/lang/invoke/WrongMethodTypeException");return; }
            if(!(site.bsmArgs[0] instanceof String)) { failed(owner,site.desc,"java/lang/ClassCastException");return; }
            recipe=(String)site.bsmArgs[0];
        } else {
            if(site.bsmArgs.length!=0){failed(owner,site.desc,"java/lang/invoke/WrongMethodTypeException");return;}
            recipe="\u0001".repeat(args.length);
        }
        int values=0,literals=0;
        for(char c:recipe.toCharArray()){if(c==1)values++;if(c==2)literals++;}
        if(!Type.getReturnType(site.desc).equals(Type.getType(String.class)) || values!=args.length ||
                (constants?literals!=site.bsmArgs.length-1:site.bsmArgs.length!=0) || argsSlots(args)>200) {
            failed(owner,site.desc,"java/lang/invoke/StringConcatException");return;
        }
        // Validate static constants before emitting any adapter. Handles/types/dynamic constants
        // need their own resolution protocol and must never be approximated with host toString.
        for(int i=1;i<site.bsmArgs.length;i++) if(!(site.bsmArgs[i] instanceof String ||
                site.bsmArgs[i] instanceof Integer || site.bsmArgs[i] instanceof Long || site.bsmArgs[i] instanceof Float || site.bsmArgs[i] instanceof Double))
            throw new IllegalArgumentException("unsupported concat recipe constant "+site.bsmArgs[i]);
        MethodNode code=method(owner,ACC_PUBLIC|ACC_STATIC,"link",site.desc);
        // Convert references once in argument order, before recipe assembly (including repeated/null values).
        int[] slots=new int[args.length],texts=new int[args.length];int local=0;
        for(int i=0;i<args.length;i++){slots[i]=local;local+=args[i].getSize();}
        for(int i=0;i<args.length;i++){
            texts[i]=local++;code.visitVarInsn(args[i].getOpcode(ILOAD),slots[i]);convertString(code,args[i]);
            // StringConcatFactory treats a null toString result as the four characters "null".
            // String.valueOf(Object) itself must retain the JDK behavior of returning that null.
            if(reference(args[i]))call(code,INVOKESTATIC,"javaw/runtime/ExceptionSupport","concatValue","(Ljava/lang/String;)Ljava/lang/String;");
            code.visitVarInsn(ASTORE,texts[i]);
        }
        // A concat expression produces a fresh String, including empty/single argument recipes.
        code.visitTypeInsn(NEW,"java/lang/String");code.visitInsn(DUP);
        call(code,INVOKESPECIAL,"java/lang/String","<init>","()V");
        int value=0,literal=1;StringBuilder text=new StringBuilder();
        for(int i=0;i<=recipe.length();i++) {
            char c=i==recipe.length()?0:recipe.charAt(i);
            if(i<recipe.length() && c!=1 && c!=2){text.append(c);continue;}
            if(text.length()!=0){code.visitLdcInsn(text.toString());append(code);text.setLength(0);}
            if(i==recipe.length())break;
            if(c==1)code.visitVarInsn(ALOAD,texts[value++]);
            else {Object constant=site.bsmArgs[literal++];code.visitLdcInsn(constant instanceof String?constant:constant.toString());}
            append(code);
        }
        code.visitInsn(ARETURN);finish(code,local);
    }
    private static void record(ClassNode owner,ClassNode caller,InvokeDynamicInsnNode site,Map<String,ClassNode> linked) {
        String expected="(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;";
        if(!site.bsm.getName().equals("bootstrap") || !site.bsm.getDesc().equals(expected))
            throw new IllegalArgumentException("unsupported record bootstrap "+site.bsm);
        if(site.bsmArgs.length<2 || !(site.bsmArgs[0] instanceof Type type) || !type.equals(Type.getObjectType(caller.name)) ||
                !(site.bsmArgs[1] instanceof String componentNames))
            throw new IllegalArgumentException("unsupported record bootstrap argument shape");
        String[] labels=componentNames.isEmpty()?new String[0]:componentNames.split(";");
        if(labels.length!=site.bsmArgs.length-2){failed(owner,site.desc,"java/lang/IllegalArgumentException");return;}
        List<Handle> fields=new ArrayList<>();
        for(int i=2;i<site.bsmArgs.length;i++) {
            if(!(site.bsmArgs[i] instanceof Handle field) || field.getTag()!=H_GETFIELD || !field.getOwner().equals(caller.name))
                throw new IllegalArgumentException("record bootstrap requires linked instance field getters");
            fields.add(field);
        }
        String receiver="L"+caller.name+";";
        String descriptor=switch(site.name){case "equals"->"("+receiver+"Ljava/lang/Object;)Z";
            case "hashCode"->"("+receiver+")I";case "toString"->"("+receiver+")Ljava/lang/String;";default->null;};
        if(!site.desc.equals(descriptor) || (caller.access&ACC_FINAL)==0){failed(owner,site.desc,"java/lang/IllegalArgumentException");return;}
        MethodNode code=method(owner,ACC_PUBLIC|ACC_STATIC,"link",site.desc);
        if(site.name.equals("equals")) {
            Label different=new Label(),typed=new Label(),unequal=new Label();
            code.visitVarInsn(ALOAD,0);code.visitVarInsn(ALOAD,1);code.visitJumpInsn(IF_ACMPNE,different);
            code.visitInsn(ICONST_1);code.visitInsn(IRETURN);
            code.visitLabel(different);code.visitFrame(F_SAME,0,null,0,null);
            code.visitVarInsn(ALOAD,1);code.visitTypeInsn(INSTANCEOF,caller.name);code.visitJumpInsn(IFNE,typed);
            code.visitInsn(ICONST_0);code.visitInsn(IRETURN);
            code.visitLabel(typed);code.visitFrame(F_SAME,0,null,0,null);
            code.visitVarInsn(ALOAD,1);code.visitTypeInsn(CHECKCAST,caller.name);code.visitVarInsn(ASTORE,2);
            for(int i=fields.size()-1;i>=0;i--) {
                Handle field=fields.get(i);Type value=Type.getType(field.getDesc());
                recordField(code,field,0,caller);recordField(code,field,2,caller);
                if(reference(value)) {call(code,INVOKESTATIC,"java/util/Objects","equals","(Ljava/lang/Object;Ljava/lang/Object;)Z");code.visitJumpInsn(IFEQ,unequal);}
                else if(value.getSort()==Type.LONG){code.visitInsn(LCMP);code.visitJumpInsn(IFNE,unequal);}
                else if(value.getSort()==Type.FLOAT || value.getSort()==Type.DOUBLE) {
                    call(code,INVOKESTATIC,box(value),"compare","("+field.getDesc()+field.getDesc()+")I");code.visitJumpInsn(IFNE,unequal);
                } else code.visitJumpInsn(IF_ICMPNE,unequal);
            }
            code.visitInsn(ICONST_1);code.visitInsn(IRETURN);
            if(!fields.isEmpty()) {
                code.visitLabel(unequal);code.visitFrame(F_APPEND,1,new Object[]{caller.name},0,null);code.visitInsn(ICONST_0);code.visitInsn(IRETURN);
            }
            finish(code,3);
        } else if(site.name.equals("hashCode")) {
            code.visitInsn(ICONST_0);code.visitVarInsn(ISTORE,1);
            for(Handle field:fields) {
                code.visitVarInsn(ILOAD,1);code.visitIntInsn(BIPUSH,31);code.visitInsn(IMUL);recordField(code,field,0,caller);
                Type value=Type.getType(field.getDesc());
                if(reference(value))call(code,INVOKESTATIC,"java/util/Objects","hashCode","(Ljava/lang/Object;)I");
                else if(value.getSort()==Type.BOOLEAN || value.getSort()>=Type.FLOAT)
                    call(code,INVOKESTATIC,box(value),"hashCode","("+field.getDesc()+")I");
                code.visitInsn(IADD);code.visitVarInsn(ISTORE,1);
            }
            code.visitVarInsn(ILOAD,1);code.visitInsn(IRETURN);finish(code,2);
        } else {
            code.visitLdcInsn(Type.getObjectType(caller.name));call(code,INVOKEVIRTUAL,"java/lang/Class","getSimpleName","()Ljava/lang/String;");
            code.visitLdcInsn("[");append(code);
            for(int i=0;i<fields.size();i++) {
                code.visitLdcInsn((i==0?"":", ")+labels[i]+"=");append(code);recordField(code,fields.get(i),0,caller);
                convertString(code,Type.getType(fields.get(i).getDesc()));
                call(code,INVOKESTATIC,"javaw/runtime/ExceptionSupport","concatValue","(Ljava/lang/String;)Ljava/lang/String;");append(code);
            }
            code.visitLdcInsn("]");append(code);code.visitInsn(ARETURN);finish(code,1);
        }
    }
    private static void recordField(MethodVisitor code,Handle field,int slot,ClassNode caller) {
        MethodNode bridge=null;
        for(MethodNode candidate:caller.methods)if(candidate.name.startsWith("$javaw$component$") && candidate.desc.equals("()"+field.getDesc()) &&
                candidate.instructions.toArray().length==3 && candidate.instructions.get(1) instanceof FieldInsnNode access &&
                access.owner.equals(field.getOwner()) && access.name.equals(field.getName()) && access.desc.equals(field.getDesc()))bridge=candidate;
        if(bridge==null) {
            String name="$javaw$component$"+caller.methods.size();
            if(caller.methods.stream().anyMatch(m->m.name.equals(name)))throw new IllegalArgumentException("reserved component adapter name collision");
            bridge=method(caller,ACC_FINAL|ACC_SYNTHETIC,name,"()"+field.getDesc());
            bridge.visitVarInsn(ALOAD,0);bridge.visitFieldInsn(GETFIELD,field.getOwner(),field.getName(),field.getDesc());
            bridge.visitInsn(Type.getType(field.getDesc()).getOpcode(IRETURN));finish(bridge,1);
        }
        code.visitVarInsn(ALOAD,slot);call(code,INVOKEVIRTUAL,caller.name,bridge.name,bridge.desc);
    }

    private static int argsSlots(Type[] args){int size=0;for(Type arg:args)size+=arg.getSize();return size;}
    private static void append(MethodVisitor code){call(code,INVOKEVIRTUAL,"java/lang/String","concat","(Ljava/lang/String;)Ljava/lang/String;");}

    /** Known protocol errors link lazily and memoize their Java bootstrap failure per site. */
    private static void failed(ClassNode owner,String descriptor) {
        failed(owner,descriptor,"java/lang/invoke/LambdaConversionException");
    }
    private static void failed(ClassNode owner,String descriptor,String cause) {
        failed(owner,descriptor,cause,true);
    }
    private static void failed(ClassNode owner,String descriptor,String cause,boolean wrap) {
        String error=wrap?"java/lang/BootstrapMethodError":cause;
        owner.fields.add(new FieldNode(ACC_PRIVATE|ACC_STATIC,"failure","L"+error+";",null,null));
        MethodNode code=method(owner,ACC_PUBLIC|ACC_STATIC,"link",descriptor);
        Label ready=new Label();code.visitFieldInsn(GETSTATIC,owner.name,"failure","L"+error+";");
        code.visitInsn(DUP);code.visitJumpInsn(IFNONNULL,ready);code.visitInsn(POP);
        code.visitTypeInsn(NEW,error);code.visitInsn(DUP);
        if(wrap) {
            code.visitTypeInsn(NEW,cause);code.visitInsn(DUP);call(code,INVOKESPECIAL,cause,"<init>","()V");
            call(code,INVOKESPECIAL,error,"<init>","(Ljava/lang/Throwable;)V");
        } else call(code,INVOKESPECIAL,error,"<init>","()V");
        code.visitInsn(DUP);code.visitFieldInsn(PUTSTATIC,owner.name,"failure","L"+error+";");
        code.visitLabel(ready);code.visitFrame(F_SAME1,0,null,1,new Object[]{error});code.visitInsn(ATHROW);finish(code,argsSlots(Type.getArgumentTypes(descriptor)));
    }

    private static void constant(ClassNode owner,ClassNode caller,ConstantDynamic value,Map<String,ClassNode> linked) {
        Handle bootstrap=value.getBootstrapMethod();
        String prefix="(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/Class;";
        String name=bootstrap.getName(),expected;
        if(name.equals("nullConstant"))expected=prefix+")Ljava/lang/Object;";
        else if(name.equals("primitiveClass"))expected=prefix+")Ljava/lang/Class;";
        else if(name.equals("getStaticFinal"))expected=prefix+(value.getBootstrapMethodArgumentCount()==1?"Ljava/lang/Class;":"")+")Ljava/lang/Object;";
        else throw new IllegalArgumentException("unsupported constant bootstrap "+bootstrap);
        if(bootstrap.getTag()!=H_INVOKESTATIC || bootstrap.isInterface() ||
                !bootstrap.getOwner().equals("java/lang/invoke/ConstantBootstraps") || !bootstrap.getDesc().equals(expected))
            throw new IllegalArgumentException("unsupported constant bootstrap "+bootstrap);
        String descriptor="()"+value.getDescriptor();
        if(name.equals("getStaticFinal")) {
            Type declaration;
            if(value.getBootstrapMethodArgumentCount()==0) {
                Type type=Type.getType(value.getDescriptor());
                if(!reference(type))throw new IllegalArgumentException("implicit primitive getStaticFinal owner requires wrapper value fields");
                declaration=type;
            } else if(value.getBootstrapMethodArgumentCount()==1 && value.getBootstrapMethodArgument(0) instanceof Type type && type.getSort()==Type.OBJECT) declaration=type;
            else throw new IllegalArgumentException("getStaticFinal requires one linked declaring-class constant");
            FieldInsnNode field=new FieldInsnNode(GETSTATIC,declaration.getInternalName(),value.getName(),value.getDescriptor());
            String declaring=new BytecodeDispatch(linked,caller.name).fieldOwner(field);
            FieldNode target=linked.get(declaring).fields.stream().filter(f->f.name.equals(value.getName()) && f.desc.equals(value.getDescriptor())).findFirst().orElseThrow();
            if((target.access&ACC_FINAL)==0) {
                failed(owner,descriptor,"java/lang/IncompatibleClassChangeError",false);return;
            }
            nest(owner,caller,linked);
            cachedValue(owner,descriptor,code->code.instructions.add(field));return;
        }
        if(value.getBootstrapMethodArgumentCount()!=0)
            {failed(owner,descriptor,"java/lang/invoke/WrongMethodTypeException");return;}
        if(name.equals("nullConstant")?!reference(Type.getType(value.getDescriptor())):
                !value.getDescriptor().equals("Ljava/lang/Class;") || !BytecodeClasses.primitiveTokens().containsKey(value.getName())) {
            failed(owner,descriptor,"java/lang/IllegalArgumentException");return;
        }
        if(name.equals("nullConstant")) {
            MethodNode code=method(owner,ACC_PUBLIC|ACC_STATIC,"link",descriptor);
            code.visitInsn(ACONST_NULL);code.visitInsn(ARETURN);finish(code,0);
        } else cachedValue(owner,descriptor,code->{
            code.visitLdcInsn(BytecodeClasses.primitiveTokens().get(value.getName()));
            call(code,INVOKESTATIC,"javaw/runtime/ExceptionSupport","classMirror","(I)Ljava/lang/Class;");
        });
    }

    private static void cachedValue(ClassNode owner,String descriptor,java.util.function.Consumer<MethodNode> producer) {
        cachedValue(owner,"link",descriptor,producer);
    }
    private static void cachedValue(ClassNode owner,String name,String descriptor,java.util.function.Consumer<MethodNode> producer) {
        String failure="Ljava/lang/Throwable;",value=Type.getReturnType(descriptor).getDescriptor();
        boolean returnsValue=!value.equals("V");
        owner.fields.add(new FieldNode(ACC_PRIVATE|ACC_STATIC,"resolved","I",null,null));
        owner.fields.add(new FieldNode(ACC_PRIVATE|ACC_STATIC,"failure",failure,null,null));
        if(returnsValue)owner.fields.add(new FieldNode(ACC_PRIVATE|ACC_STATIC,"value",value,null,null));
        MethodNode code=method(owner,ACC_PUBLIC|ACC_STATIC,name,descriptor);
        Label start=new Label(),end=new Label(),handler=new Label(),keep=new Label(),ready=new Label(),success=new Label();
        code.visitTryCatchBlock(start,end,handler,"java/lang/Throwable");
        code.visitFieldInsn(GETSTATIC,owner.name,"resolved","I");code.visitJumpInsn(IFNE,ready);
        code.visitLabel(start);producer.accept(code);
        if(returnsValue)code.visitFieldInsn(PUTSTATIC,owner.name,"value",value);
        code.visitInsn(ICONST_1);code.visitFieldInsn(PUTSTATIC,owner.name,"resolved","I");
        code.visitLabel(end);code.visitJumpInsn(GOTO,ready);
        code.visitLabel(handler);code.visitFrame(F_SAME1,0,null,1,new Object[]{"java/lang/Throwable"});
        code.visitInsn(DUP);code.visitTypeInsn(INSTANCEOF,"java/lang/Error");code.visitJumpInsn(IFNE,keep);
        code.visitTypeInsn(NEW,"java/lang/BootstrapMethodError");code.visitInsn(DUP_X1);code.visitInsn(SWAP);
        call(code,INVOKESPECIAL,"java/lang/BootstrapMethodError","<init>","(Ljava/lang/Throwable;)V");
        code.visitLabel(keep);code.visitFrame(F_SAME1,0,null,1,new Object[]{"java/lang/Throwable"});
        code.visitFieldInsn(PUTSTATIC,owner.name,"failure",failure);
        code.visitInsn(ICONST_1);code.visitFieldInsn(PUTSTATIC,owner.name,"resolved","I");
        code.visitLabel(ready);code.visitFrame(F_SAME,0,null,0,null);
        code.visitFieldInsn(GETSTATIC,owner.name,"failure",failure);code.visitInsn(DUP);code.visitJumpInsn(IFNULL,success);code.visitInsn(ATHROW);
        code.visitLabel(success);code.visitFrame(F_SAME1,0,null,1,new Object[]{"java/lang/Throwable"});code.visitInsn(POP);
        if(returnsValue)code.visitFieldInsn(GETSTATIC,owner.name,"value",value);
        code.visitInsn(Type.getType(value).getOpcode(IRETURN));finish(code,0);
    }

    static void nest(ClassNode owner,ClassNode caller,Map<String,ClassNode> linked) {
        ClassNode host=caller.nestHostClass==null?caller:linked.get(caller.nestHostClass);
        if(host==null)throw new IllegalArgumentException("link bootstrap caller nest host "+caller.nestHostClass);
        if((host.version&0xffff)<V11) {
            if((host.version&0xffff)<V1_7)throw new IllegalArgumentException("legacy bootstrap nest host requires Java 7+ stack maps");
            host.version=V11;
        }
        owner.nestHostClass=host.name;if(host.nestMembers==null)host.nestMembers=new ArrayList<>();host.nestMembers.add(owner.name);
    }

    private static Handle specialBridge(ClassNode caller,Handle target) {
        if(target.getName().startsWith("<"))throw new IllegalArgumentException("special implementation handles cannot name initializers");
        String name="$javaw$special$"+caller.methods.size();
        if(caller.methods.stream().anyMatch(m->m.name.equals(name)))throw new IllegalArgumentException("reserved special adapter name collision");
        MethodNode bridge=method(caller,ACC_PRIVATE|ACC_SYNTHETIC,name,target.getDesc());
        bridge.visitVarInsn(ALOAD,0);int slot=1;
        for(Type argument:Type.getArgumentTypes(target.getDesc())){bridge.visitVarInsn(argument.getOpcode(ILOAD),slot);slot+=argument.getSize();}
        bridge.visitMethodInsn(INVOKESPECIAL,target.getOwner(),target.getName(),target.getDesc(),target.isInterface());
        bridge.visitInsn(Type.getReturnType(target.getDesc()).getOpcode(IRETURN));finish(bridge,slot);
        boolean iface=(caller.access&ACC_INTERFACE)!=0;
        return new Handle(iface?H_INVOKEINTERFACE:H_INVOKEVIRTUAL,caller.name,name,target.getDesc(),iface);
    }

    private static final String[] BOXES={"java/lang/Boolean","java/lang/Character","java/lang/Byte","java/lang/Short","java/lang/Integer","java/lang/Float","java/lang/Long","java/lang/Double"};
    private static String box(Type primitive){return BOXES[primitive.getSort()-Type.BOOLEAN];}
    private static Type unboxed(Type reference) {
        if(reference.getSort()!=Type.OBJECT)return null;
        for(int i=0;i<BOXES.length;i++)if(BOXES[i].equals(reference.getInternalName()))
            return Type.getType(new String[]{"Z","C","B","S","I","F","J","D"}[i]);
        return null;
    }
    private static boolean widens(Type from,Type to) {
        if(from.equals(to))return true;
        return switch(from.getSort()) {
            case Type.BYTE -> "SIJFD".contains(to.getDescriptor());
            case Type.SHORT,Type.CHAR -> "IJFD".contains(to.getDescriptor());
            case Type.INT -> "JFD".contains(to.getDescriptor());
            case Type.LONG -> "FD".contains(to.getDescriptor());
            case Type.FLOAT -> to.getSort()==Type.DOUBLE;
            default -> false;
        };
    }
    private static boolean subtype(Type from,Type to,BytecodeDispatch dispatch) {
        return from.equals(to) || reference(from)&&reference(to)&&dispatch.descriptorAssignable(from.getDescriptor(),to.getDescriptor());
    }
    private static boolean adaptable(Type from,Type to,boolean strict,BytecodeDispatch dispatch) {
        if(to.equals(Type.VOID_TYPE))return true;
        if(from.equals(Type.VOID_TYPE))return false;
        if(!reference(from))return !reference(to)?widens(from,to):subtype(Type.getObjectType(box(from)),to,dispatch);
        if(reference(to))return !strict || subtype(from,to,dispatch);
        Type primitive=unboxed(from);return primitive!=null?widens(primitive,to):!strict;
    }
    static void adapt(MethodVisitor code,Type from,Type to) {
        if(from.equals(to))return;
        if(reference(from)&&reference(to)) {if(!to.equals(Type.getType(Object.class)))code.visitTypeInsn(CHECKCAST,to.getInternalName());return;}
        if(!reference(from) && reference(to)) {
            String wrapper=box(from);call(code,INVOKESTATIC,wrapper,"valueOf","("+from.getDescriptor()+")L"+wrapper+";");
            if(!to.equals(Type.getType(Object.class)))code.visitTypeInsn(CHECKCAST,to.getInternalName());return;
        }
        if(reference(from) && !reference(to)) {
            Type primitive=unboxed(from);
            String wrapper;
            if(primitive!=null)wrapper=box(primitive);
            else {primitive=to;wrapper=to.getSort()==Type.BOOLEAN || to.getSort()==Type.CHAR?box(to):"java/lang/Number";}
            code.visitTypeInsn(CHECKCAST,wrapper);
            call(code,INVOKEVIRTUAL,wrapper,primitive.getClassName()+"Value","()"+primitive.getDescriptor());
            adapt(code,primitive,to);return;
        }
        int source=from.getSort(),target=to.getSort();
        if(widens(from,to) && source>=Type.CHAR && source<=Type.INT) {
            int opcode=switch(target){case Type.LONG->I2L;case Type.FLOAT->I2F;case Type.DOUBLE->I2D;default->-1;};
            if(opcode>=0)code.visitInsn(opcode);return;
        }
        if(source==Type.LONG && (target==Type.FLOAT || target==Type.DOUBLE)){code.visitInsn(target==Type.FLOAT?L2F:L2D);return;}
        if(source==Type.FLOAT && target==Type.DOUBLE){code.visitInsn(F2D);return;}
        throw new IllegalArgumentException("unsupported lambda descriptor adaptation "+from+" -> "+to+" (boxing/narrowing requires runtime support)");
    }
    private static void lambda(ClassNode owner,ClassNode caller,InvokeDynamicInsnNode site,Map<String,ClassNode> linked,List<ClassNode> generated,Set<String> names) {
        boolean alternate=site.bsm.getName().equals("altMetafactory");
        String expected="("+LOOKUP+(alternate?"[Ljava/lang/Object;":"Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;")+")"+CALLSITE;
        if((!alternate && !site.bsm.getName().equals("metafactory")) || !site.bsm.getDesc().equals(expected))
            throw new IllegalArgumentException("unsupported lambda bootstrap "+site.bsm);
        if(alternate?site.bsmArgs.length<4:site.bsmArgs.length!=3) {
            failed(owner,site.desc,alternate?"java/lang/IllegalArgumentException":"java/lang/invoke/WrongMethodTypeException");return;
        }
        if(!(site.bsmArgs[0] instanceof Type sam) || sam.getSort()!=Type.METHOD ||
                !(site.bsmArgs[1] instanceof Handle target) || !(site.bsmArgs[2] instanceof Type instantiated) || instantiated.getSort()!=Type.METHOD)
            {failed(owner,site.desc,alternate?"java/lang/IllegalArgumentException":"java/lang/ClassCastException");return;}
        if(!BytecodeDeclarations.validDescriptor(sam.getDescriptor(),true) || !BytecodeDeclarations.validDescriptor(instantiated.getDescriptor(),true) ||
                !BytecodeDeclarations.validDescriptor(target.getDesc(),true))
            throw new IllegalArgumentException("invalid lambda method descriptor");
        List<Type> markers=new ArrayList<>(),bridges=new ArrayList<>();boolean serializable=false;
        if(alternate) {
            if(!(site.bsmArgs[3] instanceof Integer flags)){failed(owner,site.desc,"java/lang/IllegalArgumentException");return;}
            serializable=(flags&1)!=0;
            if((flags&~7)!=0)throw new IllegalArgumentException("unsupported altMetafactory flags "+flags);
            int index=4;
            for(int flag:new int[]{2,4})if((flags&flag)!=0) {
                if(index>=site.bsmArgs.length || !(site.bsmArgs[index++] instanceof Integer count) || count<0 || count>site.bsmArgs.length-index)
                    {failed(owner,site.desc,"java/lang/IllegalArgumentException");return;}
                for(int i=0;i<count;i++) {
                    if(!(site.bsmArgs[index++] instanceof Type type) || (flag==2?type.getSort()!=Type.OBJECT:type.getSort()!=Type.METHOD))
                        {failed(owner,site.desc,"java/lang/IllegalArgumentException");return;}
                    if(flag==4 && !BytecodeDeclarations.validDescriptor(type.getDescriptor(),true))
                        throw new IllegalArgumentException("invalid lambda bridge descriptor "+type);
                    (flag==2?markers:bridges).add(type);
                }
            }
            if(index!=site.bsmArgs.length){failed(owner,site.desc,"java/lang/IllegalArgumentException");return;}
        }
        Type result=Type.getReturnType(site.desc);
        if(result.getSort()!=Type.OBJECT){failed(owner,site.desc);return;}
        ClassNode iface=linked.get(result.getInternalName());
        if(iface==null || (iface.access&ACC_INTERFACE)==0)throw new IllegalArgumentException("link lambda functional interface "+result);
        if(target.getTag()!=H_INVOKESTATIC && target.getTag()!=H_INVOKEVIRTUAL && target.getTag()!=H_INVOKEINTERFACE && target.getTag()!=H_INVOKESPECIAL && target.getTag()!=H_NEWINVOKESPECIAL)
            throw new IllegalArgumentException("unsupported lambda implementation handle "+target);
        Type[] captures=Type.getArgumentTypes(site.desc),parameters=sam.getArgumentTypes(),specialized=instantiated.getArgumentTypes();
        List<Type> inputs=new ArrayList<>();
        if(target.getTag()==H_INVOKEVIRTUAL || target.getTag()==H_INVOKEINTERFACE || target.getTag()==H_INVOKESPECIAL)
            inputs.add(Type.getObjectType(target.getTag()==H_INVOKESPECIAL?caller.name:target.getOwner()));
        inputs.addAll(List.of(Type.getArgumentTypes(target.getDesc())));
        if(parameters.length!=specialized.length || inputs.size()!=captures.length+parameters.length) {failed(owner,site.desc);return;}
        BytecodeDispatch adaptation=new BytecodeDispatch(linked,caller.name);
        Type returnedType=target.getTag()==H_NEWINVOKESPECIAL?Type.getObjectType(target.getOwner()):Type.getReturnType(target.getDesc());
        boolean valid=subtype(instantiated.getReturnType(),sam.getReturnType(),adaptation) &&
                adaptable(returnedType,instantiated.getReturnType(),false,adaptation);
        for(int i=0;i<captures.length;i++)valid&=i==0 && target.getTag()!=H_INVOKESTATIC && target.getTag()!=H_NEWINVOKESPECIAL?
                subtype(captures[i],inputs.get(i),adaptation):captures[i].equals(inputs.get(i));
        for(int i=0;i<parameters.length;i++)valid&=subtype(specialized[i],parameters[i],adaptation) &&
                adaptable(specialized[i],inputs.get(captures.length+i),true,adaptation);
        for(Type bridge:bridges) {
            valid&=bridge.getArgumentTypes().length==parameters.length && subtype(instantiated.getReturnType(),bridge.getReturnType(),adaptation);
            if(bridge.getArgumentTypes().length==parameters.length)for(int i=0;i<parameters.length;i++)
                valid&=subtype(specialized[i],bridge.getArgumentTypes()[i],adaptation);
        }
        if(!valid){failed(owner,site.desc);return;}
        for(Type marker:markers) {
            ClassNode declaration=linked.get(marker.getInternalName());
            if(declaration==null || (declaration.access&ACC_INTERFACE)==0)
                throw new IllegalArgumentException("link lambda marker interface "+marker);
        }
        for(Type bridge:bridges)if(bridge.getArgumentTypes().length!=parameters.length) {failed(owner,site.desc);return;}
        Handle invocation=target.getTag()==H_INVOKESPECIAL?specialBridge(caller,target):target;
        ClassNode linkage=owner;
        String implementationName=owner.name+"$lambda";
        if(!names.add(implementationName))throw new IllegalArgumentException("reserved lambda adapter name collision: "+implementationName);
        owner=type(implementationName);owner.sourceFile=caller.sourceFile;
        owner.interfaces.add(result.getInternalName());
        if(serializable && !owner.interfaces.contains("java/io/Serializable"))owner.interfaces.add("java/io/Serializable");
        for(Type marker:markers)if(!owner.interfaces.contains(marker.getInternalName()))owner.interfaces.add(marker.getInternalName());
        // Generated lambda is a real nestmate; original access checks still run on its target call.
        nest(owner,caller,linked);
        String constructor=Type.getMethodDescriptor(Type.VOID_TYPE,captures);
        MethodNode init=method(owner,ACC_PRIVATE,"<init>",constructor);init.visitVarInsn(ALOAD,0);
        call(init,INVOKESPECIAL,"java/lang/Object","<init>","()V");
        int slot=1;
        for(int i=0;i<captures.length;i++){
            Type capture=captures[i];owner.fields.add(new FieldNode(ACC_PRIVATE|ACC_FINAL,"capture"+i,capture.getDescriptor(),null,null));
            init.visitVarInsn(ALOAD,0);init.visitVarInsn(capture.getOpcode(ILOAD),slot);
            init.visitFieldInsn(PUTFIELD,owner.name,"capture"+i,capture.getDescriptor());slot+=capture.getSize();
        }
        init.visitInsn(RETURN);finish(init,slot);
        MethodNode factory=method(owner,ACC_PUBLIC|ACC_STATIC,"create",site.desc);
        factory.visitTypeInsn(NEW,owner.name);factory.visitInsn(DUP);slot=0;
        for(Type capture:captures){factory.visitVarInsn(capture.getOpcode(ILOAD),slot);slot+=capture.getSize();}
        call(factory,INVOKESPECIAL,owner.name,"<init>",constructor);factory.visitInsn(ARETURN);finish(factory,slot);
        MethodNode invoke=method(owner,ACC_PUBLIC|ACC_FINAL,site.name,sam.getDescriptor());
        boolean construct=invocation.getTag()==H_NEWINVOKESPECIAL;
        if(construct){invoke.visitTypeInsn(NEW,invocation.getOwner());invoke.visitInsn(DUP);}
        int input=0;
        for(int i=0;i<captures.length;i++){
            invoke.visitVarInsn(ALOAD,0);invoke.visitFieldInsn(GETFIELD,owner.name,"capture"+i,captures[i].getDescriptor());
            adapt(invoke,captures[i],inputs.get(input++));
        }
        slot=1;
        for(int i=0;i<parameters.length;i++){
            invoke.visitVarInsn(parameters[i].getOpcode(ILOAD),slot);slot+=parameters[i].getSize();
            adapt(invoke,parameters[i],specialized[i]);adapt(invoke,specialized[i],inputs.get(input++));
        }
        int opcode=switch(invocation.getTag()){case H_INVOKESTATIC->INVOKESTATIC;case H_INVOKEINTERFACE->INVOKEINTERFACE;case H_NEWINVOKESPECIAL->INVOKESPECIAL;default->INVOKEVIRTUAL;};
        invoke.visitMethodInsn(opcode,invocation.getOwner(),invocation.getName(),invocation.getDesc(),invocation.isInterface());
        Type returned=construct?Type.getObjectType(target.getOwner()):Type.getReturnType(target.getDesc());
        if(sam.getReturnType().equals(Type.VOID_TYPE)){
            if(!returned.equals(Type.VOID_TYPE))invoke.visitInsn(returned.getSize()==2?POP2:POP);
        } else {adapt(invoke,returned,instantiated.getReturnType());adapt(invoke,instantiated.getReturnType(),sam.getReturnType());}
        invoke.visitInsn(sam.getReturnType().getOpcode(IRETURN));finish(invoke,slot);
        Set<String> emitted=new HashSet<>();emitted.add(sam.getDescriptor());
        for(Type bridge:bridges) {
            if(!emitted.add(bridge.getDescriptor()))continue;
            MethodNode code=method(owner,ACC_PUBLIC|ACC_FINAL|ACC_BRIDGE|ACC_SYNTHETIC,site.name,bridge.getDescriptor());
            code.visitVarInsn(ALOAD,0);slot=1;Type[] arguments=bridge.getArgumentTypes();
            for(int i=0;i<arguments.length;i++) {
                code.visitVarInsn(arguments[i].getOpcode(ILOAD),slot);slot+=arguments[i].getSize();
                adapt(code,arguments[i],specialized[i]);adapt(code,specialized[i],parameters[i]);
            }
            call(code,INVOKEVIRTUAL,owner.name,site.name,sam.getDescriptor());
            if(bridge.getReturnType().equals(Type.VOID_TYPE)) {
                if(!sam.getReturnType().equals(Type.VOID_TYPE))code.visitInsn(sam.getReturnType().getSize()==2?POP2:POP);
            } else {adapt(code,sam.getReturnType(),instantiated.getReturnType());adapt(code,instantiated.getReturnType(),bridge.getReturnType());}
            code.visitInsn(bridge.getReturnType().getOpcode(IRETURN));finish(code,slot);
        }
        if(serializable) {
            MethodNode replace=method(owner,ACC_PRIVATE|ACC_FINAL,"writeReplace","()Ljava/lang/Object;");
            String serialized="java/lang/invoke/SerializedLambda";
            replace.visitTypeInsn(NEW,serialized);replace.visitInsn(DUP);replace.visitLdcInsn(Type.getObjectType(caller.name));
            replace.visitLdcInsn(result.getInternalName());replace.visitLdcInsn(site.name);replace.visitLdcInsn(sam.getDescriptor());
            replace.visitLdcInsn(target.getTag());replace.visitLdcInsn(target.getOwner());replace.visitLdcInsn(target.getName());
            replace.visitLdcInsn(target.getDesc());replace.visitLdcInsn(instantiated.getDescriptor());
            replace.visitLdcInsn(captures.length);replace.visitTypeInsn(ANEWARRAY,"java/lang/Object");
            for(int i=0;i<captures.length;i++) {
                replace.visitInsn(DUP);replace.visitLdcInsn(i);replace.visitVarInsn(ALOAD,0);
                replace.visitFieldInsn(GETFIELD,owner.name,"capture"+i,captures[i].getDescriptor());
                if(!reference(captures[i]))adapt(replace,captures[i],Type.getType(Object.class));
                replace.visitInsn(AASTORE);
            }
            call(replace,INVOKESPECIAL,serialized,"<init>","(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)V");
            replace.visitInsn(ARETURN);finish(replace,1);
        }
        MethodNode initialize=method(owner,ACC_PUBLIC|ACC_STATIC,"initialize","()V");
        initialize.visitInsn(RETURN);finish(initialize,0);
        // Linking the lambda class initializes default-method interfaces. Cache that linkage
        // failure separately from later instance allocations, which must remain retryable.
        MethodNode prepare=method(linkage,ACC_PRIVATE|ACC_STATIC,"prepare","()V");
        Label begin=new Label(),end=new Label(),handler=new Label();
        prepare.visitTryCatchBlock(begin,end,handler,"java/lang/Throwable");
        prepare.visitLabel(begin);call(prepare,INVOKESTATIC,implementationName,"initialize","()V");
        prepare.visitLabel(end);prepare.visitInsn(RETURN);
        prepare.visitLabel(handler);prepare.visitFrame(F_SAME1,0,null,1,new Object[]{"java/lang/Throwable"});
        prepare.visitTypeInsn(NEW,"java/lang/InternalError");prepare.visitInsn(DUP_X1);prepare.visitInsn(SWAP);
        call(prepare,INVOKESPECIAL,"java/lang/InternalError","<init>","(Ljava/lang/Throwable;)V");prepare.visitInsn(ATHROW);finish(prepare,0);
        cachedValue(linkage,"resolve","()V",code->call(code,INVOKESTATIC,linkage.name,"prepare","()V"));
        MethodNode link=method(linkage,ACC_PUBLIC|ACC_STATIC,"link",site.desc);
        call(link,INVOKESTATIC,linkage.name,"resolve","()V");slot=0;
        for(Type capture:captures){link.visitVarInsn(capture.getOpcode(ILOAD),slot);slot+=capture.getSize();}
        call(link,INVOKESTATIC,implementationName,"create",site.desc);link.visitInsn(ARETURN);finish(link,slot);
        generated.add(owner);
    }
}
