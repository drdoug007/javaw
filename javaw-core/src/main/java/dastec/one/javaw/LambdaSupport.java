package dastec.one.javaw;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.*;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import java.util.*;

/** Closure conversion: each lambda is an ordinary object with fields for captured values. */
final class LambdaSupport {
    private final CompilationUnit cu;
    private ClassRegistry registry;
    private int sequence;
    private LambdaSupport(CompilationUnit cu) { this.cu = cu; }
    static void lower(CompilationUnit cu) { new LambdaSupport(cu).lower(); }
    static void lowerLoops(CompilationUnit cu) {
        LambdaSupport support = new LambdaSupport(cu);
        support.registry = ClassRegistry.build(cu);
        var loops = new ArrayList<>(cu.findAll(ForEachStmt.class));
        Collections.reverse(loops);
        int index=0;
        for (ForEachStmt loop : loops) {
            String iterable;
            try { iterable = support.type(loop.getIterable(),support.locals(loop)); }
            catch (IllegalArgumentException ignored) { continue; } // Existing array lowering infers other array expressions.
            if (iterable.endsWith("[]")) continue;
            String parent=support.registry.asSuperType(iterable,"Iterable");
            if (parent==null) throw JavaSourcePreparation.error(loop,"Enhanced for requires an array or Iterable");
            var args=GenericTypes.arguments(parent);
            String element=args.isEmpty() ? "Object" : args.get(0);
            String iterator="__javaw_iterator"+index++;
            var variable=loop.getVariable().clone();
            if(variable.getVariable(0).getTypeAsString().equals("var")) variable.getVariable(0).setType(element);
            variable.getVariable(0).setInitializer(new MethodCallExpr(new NameExpr(iterator),"next"));
            BlockStmt body=new BlockStmt().addStatement(variable).addStatement(loop.getBody().clone());
            BlockStmt replacement=new BlockStmt();
            replacement.addStatement(StaticJavaParser.parseStatement("Iterator<"+element+"> "+iterator+" = "+loop.getIterable()+".iterator();"));
            replacement.addStatement(new WhileStmt(new MethodCallExpr(new NameExpr(iterator),"hasNext"),body));
            loop.replace(replacement);
        }
    }
    private void lower() {
        registry=ClassRegistry.build(cu);
        inferEmptyFactories();
        // Outer callbacks establish parameter types needed by nested callbacks.
        while (true) {
            Expression callback = cu.findAll(Expression.class).stream()
                .filter(e -> e.isLambdaExpr() || e.isMethodReferenceExpr())
                // The existing logging facade treats listener registration as a host/no-op API.
                .filter(e -> !(e.getParentNode().orElse(null) instanceof MethodCallExpr call
                    && call.getNameAsString().equals("addListener")
                    && call.getScope().map(Object::toString).orElse("").equals("SimpleLogger")))
                .findFirst().orElse(null);
            if (callback == null) return;
            registry = ClassRegistry.build(cu);
            lower(callback);
        }
    }
    /** Supply context to factories whose arguments contribute no type constraints. */
    private void inferEmptyFactories() {
        for(MethodCallExpr call:cu.findAll(MethodCallExpr.class)) {
            if(call.getTypeArguments().isPresent() || call.getArguments().stream().anyMatch(a->!a.isNullLiteralExpr())) continue;
            Map<String,String> variables;
            try { variables=locals(call); }
            catch(IllegalArgumentException unsupported) { continue; }
            String expected;
            try { expected=contextType(call,variables); }
            catch(IllegalArgumentException unsupported) { continue; }
            if(expected==null || expected.equals("var") || expected.contains("?")) continue;
            String owner;
            try { owner=call.getScope().map(e->type(e,variables)).orElseGet(()->call.findAncestor(ClassOrInterfaceDeclaration.class).map(c->c.getNameAsString()).orElse("")); }
            catch(IllegalArgumentException unsupported) { continue; }
            var methods=registry.methodCandidates(owner,call.getNameAsString()).stream()
                .filter(m->m.getParamTypeNames().size()==call.getArguments().size()).toList();
            if(methods.size()!=1 || methods.get(0).getDeclaration()==null) continue;
            MethodModel method=methods.get(0);
            var params=GenericTypes.parameters(method.getDeclaration());
            if(params.isEmpty()) continue;
            Set<String> names=new LinkedHashSet<>(); params.forEach(p->names.add(p.getNameAsString()));
            Map<String,String> bindings=new LinkedHashMap<>();
            GenericTypes.constrain(method.getReturnTypeName(),expected,names,bindings,registry);
            if(!bindings.keySet().containsAll(names)) continue;
            // Captures/intersections cannot be expressed as explicit source witnesses.
            if(bindings.values().stream().anyMatch(t->t.contains("?") || t.contains("&"))) continue;
            NodeList<com.github.javaparser.ast.type.Type> witnesses=new NodeList<>();
            params.forEach(p->witnesses.add(StaticJavaParser.parseType(bindings.get(p.getNameAsString()))));
            call.setTypeArguments(witnesses);
        }
    }
    private String contextType(Expression expression,Map<String,String> variables) {
        Node parent=expression.getParentNode().orElse(null);
        if(parent instanceof VariableDeclarator v) return v.getTypeAsString();
        if(parent instanceof AssignExpr a) return type(a.getTarget(),variables);
        if(parent instanceof ReturnStmt) return expression.findAncestor(MethodDeclaration.class).map(m->m.getTypeAsString()).orElse(null);
        if(parent instanceof CastExpr c) return c.getTypeAsString();
        if(parent instanceof EnclosedExpr e) return contextType(e,variables);
        if(parent instanceof ConditionalExpr c) return contextType(c,variables);
        return null;
    }
    private Map<String,String> locals(Node node) {
        Map<String,String> result = new LinkedHashMap<>();
        var callable = node.findAncestor(CallableDeclaration.class).orElse(null);
        if (callable != null) {
            for (Object p : callable.getParameters()) { Parameter parameter = (Parameter)p; result.put(parameter.getNameAsString(), parameter.getTypeAsString()); }
            for (VariableDeclarator v : callable.findAll(VariableDeclarator.class)) {
                if (v.getInitializer().isPresent() && v.getInitializer().get() == node) continue;
                result.put(v.getNameAsString(), v.getTypeAsString().equals("var") && v.getInitializer().isPresent()
                    ? type(v.getInitializer().get(), result) : v.getTypeAsString());
            }
        }
        return result;
    }
    private String type(Expression e, Map<String,String> variables) {
        if (e.isNameExpr()) {
            if (variables.containsKey(e.toString())) return variables.get(e.toString());
            var owner=e.findAncestor(ClassOrInterfaceDeclaration.class);
            if(owner.isPresent() && registry.getClass(owner.get().getNameAsString()).getFields().containsKey(e.toString()))
                return registry.fieldType(owner.get().getNameAsString(),e.toString());
            return e.toString();
        }
        if (e.isThisExpr()) return e.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow().getNameAsString();
        if (e.isTypeExpr()) return variables.getOrDefault(e.asTypeExpr().getTypeAsString(),e.asTypeExpr().getTypeAsString());
        if (e.isObjectCreationExpr()) return e.asObjectCreationExpr().getTypeAsString();
        if (e.isStringLiteralExpr()) return "String";
        if (e.isIntegerLiteralExpr()) return "int";
        if (e.isLongLiteralExpr()) return "long";
        if (e.isBooleanLiteralExpr()) return "boolean";
        if (e.isNullLiteralExpr()) return "null";
        if (e.isCastExpr()) return e.asCastExpr().getTypeAsString();
        if (e.isFieldAccessExpr()) {
            var f=e.asFieldAccessExpr(); return registry.fieldType(type(f.getScope(),variables),f.getNameAsString());
        }
        if (e.isArrayCreationExpr()) return e.asArrayCreationExpr().getElementType()+"[]";
        if (e.isMethodCallExpr()) {
            MethodCallExpr call = e.asMethodCallExpr();
            String owner = call.getScope().map(s -> type(s, variables)).orElseGet(() -> e.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow().getNameAsString());
            var candidates = registry.methodCandidates(owner, call.getNameAsString());
            if (!candidates.isEmpty()) return registry.resolveOverload(candidates, call.getArguments().stream().map(a -> type(a, variables)).toList(),
                call.getTypeArguments().map(a -> a.stream().map(Object::toString).toList()).orElse(List.of())).getReturnTypeName();
        }
        throw JavaSourcePreparation.error(e, "Cannot infer callback receiver type; use an explicitly typed local");
    }
    private String target(Expression callback, Map<String,String> variables) {
        Node parent = callback.getParentNode().orElseThrow();
        if (parent instanceof VariableDeclarator v) return v.getTypeAsString();
        if (parent instanceof CastExpr c) return c.getTypeAsString();
        if (parent instanceof ReturnStmt) return callback.findAncestor(MethodDeclaration.class).orElseThrow().getTypeAsString();
        if (parent instanceof AssignExpr a) return type(a.getTarget(), variables);
        if (parent instanceof MethodCallExpr call) {
            String owner = call.getScope().map(e -> type(e, variables)).orElseGet(() -> call.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow().getNameAsString());
            var candidates = registry.methodCandidates(owner, call.getNameAsString()).stream()
                .filter(m -> m.getParamTypeNames().size() == call.getArguments().size()).toList();
            if (candidates.size() != 1) throw JavaSourcePreparation.error(callback, "Ambiguous callback target; assign to an explicitly typed functional interface first");
            MethodModel method = candidates.get(0);
            Map<String,String> bindings = new LinkedHashMap<>();
            Set<String> names = new HashSet<>();
            if (method.getDeclaration() != null) GenericTypes.parameters(method.getDeclaration()).forEach(t -> names.add(t.getNameAsString()));
            if (call.getTypeArguments().isPresent()) {
                var params = GenericTypes.parameters(method.getDeclaration());
                for (int i=0; i<params.size(); i++) bindings.put(params.get(i).getNameAsString(), call.getTypeArguments().get().get(i).asString());
            }
            for (int i=0; i<call.getArguments().size(); i++) {
                Expression a = call.getArgument(i);
                if (!a.isLambdaExpr() && !a.isMethodReferenceExpr()) GenericTypes.constrain(method.getParamTypeNames().get(i), type(a,variables), names, bindings, registry);
            }
            // Assignment context supplies generic result variables (e.g. Optional<R>).
            if (call.getParentNode().orElse(null) instanceof VariableDeclarator v)
                GenericTypes.constrain(method.getReturnTypeName(), v.getTypeAsString(), names, bindings, registry);
            names.forEach(n -> bindings.putIfAbsent(n,"Object"));
            return GenericTypes.substitute(method.getParamTypeNames().get(call.getArguments().indexOf(callback)), bindings);
        }
        throw JavaSourcePreparation.error(callback, "Callback requires a functional-interface target type");
    }
    private void lower(Expression callback) {
        Map<String,String> variables = locals(callback);
        String target = target(callback, variables);
        // Ground a wildcard functional target before generating its implementation.
        if(target.contains("?")) {
            var grounded=StaticJavaParser.parseClassOrInterfaceType(target);
            for(var wildcard:new ArrayList<>(grounded.findAll(com.github.javaparser.ast.type.WildcardType.class))) {
                var bound=wildcard.getExtendedType().orElseGet(()->wildcard.getSuperType().orElse(StaticJavaParser.parseClassOrInterfaceType("Object")));
                wildcard.replace(bound.clone());
            }
            target=grounded.asString();
        }
        ClassModel iface = registry.getClass(target);
        if (iface == null || !iface.isInterface()) throw JavaSourcePreparation.error(callback,"Callback target is not a source functional interface: " + target);
        MethodModel sam;
        try { sam = registry.functionalMethod(target); }
        catch (IllegalArgumentException error) { throw JavaSourcePreparation.error(callback,error.getMessage()); }
        String name = "__JavawLambda" + sequence++;
        ClassOrInterfaceDeclaration generated = new ClassOrInterfaceDeclaration().setName(name);
        generated.addImplementedType(target);
        var owner = callback.findAncestor(ClassOrInterfaceDeclaration.class).orElseThrow();
        generated.setData(JavaSourcePreparation.LEXICAL_OWNER,owner.containsData(JavaSourcePreparation.LEXICAL_OWNER)
            ? owner.getData(JavaSourcePreparation.LEXICAL_OWNER) : owner.getNameAsString());
        if (owner.containsData(JavaSourcePreparation.PACKAGE)) generated.setData(JavaSourcePreparation.PACKAGE,owner.getData(JavaSourcePreparation.PACKAGE));
        MethodDeclaration implementation = generated.addMethod(sam.getName(), Modifier.Keyword.PUBLIC).setType(sam.getReturnTypeName());
        if (callback.isLambdaExpr() && callback.asLambdaExpr().getParameters().size() != sam.getParamTypeNames().size())
            throw JavaSourcePreparation.error(callback,"Callback arity does not match target");
        List<String> parameters = new ArrayList<>();
        for (int i=0; i<sam.getParamTypeNames().size(); i++) {
            String parameter = callback.isLambdaExpr() ? callback.asLambdaExpr().getParameter(i).getNameAsString() : "__arg" + i;
            parameters.add(parameter); implementation.addParameter(sam.getParamTypeNames().get(i),parameter);
        }
        BlockStmt body;
        if (callback.isLambdaExpr()) {
            LambdaExpr lambda = callback.asLambdaExpr();
            if (lambda.getParameters().size()!=parameters.size()) throw JavaSourcePreparation.error(callback,"Callback arity does not match target");
            body = lambda.getBody().isBlockStmt() ? lambda.getBody().asBlockStmt().clone() : new BlockStmt().addStatement(
                sam.getReturnTypeName().equals("void") ? new ExpressionStmt(lambda.getExpressionBody().orElseThrow().clone()) : new ReturnStmt(lambda.getExpressionBody().orElseThrow().clone()));
        } else {
            MethodReferenceExpr ref = callback.asMethodReferenceExpr();
            Expression scope = ref.getScope().clone();
            if (scope.isTypeExpr() && variables.containsKey(scope.toString())) scope = new NameExpr(scope.toString());
            boolean unbound = scope.isTypeExpr() && registry.methodCandidates(type(scope,variables),ref.getIdentifier()).stream().noneMatch(MethodModel::isStatic);
            if (!scope.isTypeExpr() && !(scope.isNameExpr() && !variables.containsKey(scope.toString()) && registry.hasClass(scope.toString()))) {
                String boundType = type(scope,variables);
                generated.addField(boundType,"__bound",Modifier.Keyword.PRIVATE);
                scope = new NameExpr("__bound");
            }
            Expression invocation;
            if (ref.getIdentifier().equals("new")) invocation = new ObjectCreationExpr().setType(type(scope,variables));
            else invocation = new MethodCallExpr(unbound ? new NameExpr(parameters.remove(0)) : scope,ref.getIdentifier());
            for (String p : parameters) {
                if (invocation.isMethodCallExpr()) invocation.asMethodCallExpr().addArgument(p);
                else invocation.asObjectCreationExpr().addArgument(p);
            }
            body = new BlockStmt().addStatement(sam.getReturnTypeName().equals("void") ? new ExpressionStmt(invocation) : new ReturnStmt(invocation));
        }
        implementation.setBody(body);
        ConstructorDeclaration constructor = generated.addConstructor(Modifier.Keyword.PUBLIC);
        ObjectCreationExpr creation = new ObjectCreationExpr().setType(name);
        if (callback.isMethodReferenceExpr() && generated.getFieldByName("__bound").isPresent()) {
            Expression bound = callback.asMethodReferenceExpr().getScope();
            constructor.addParameter(type(bound,variables),"__bound");
            constructor.getBody().addStatement("if (__bound == null) throw new NullPointerException();");
            constructor.getBody().addStatement("this.__bound = __bound;"); creation.addArgument(bound.isTypeExpr() && variables.containsKey(bound.toString()) ? new NameExpr(bound.toString()) : bound.clone());
        }
        Set<String> parameterNames = new HashSet<>(); implementation.getParameters().forEach(p -> parameterNames.add(p.getNameAsString()));
        body.findAll(VariableDeclarator.class).forEach(v -> parameterNames.add(v.getNameAsString()));
        Set<String> captures = new LinkedHashSet<>();
        for (NameExpr use : body.findAll(NameExpr.class)) if (variables.containsKey(use.toString()) && !parameterNames.contains(use.toString())) captures.add(use.toString());
        var callable = callback.findAncestor(CallableDeclaration.class).orElseThrow();
        for (String capture : captures) {
            boolean mutated = callable.findAll(AssignExpr.class).stream().anyMatch(a -> a.getTarget().toString().equals(capture))
                || callable.findAll(UnaryExpr.class).stream().anyMatch(u -> u.getExpression().toString().equals(capture) && Set.of(UnaryExpr.Operator.POSTFIX_INCREMENT,UnaryExpr.Operator.POSTFIX_DECREMENT,UnaryExpr.Operator.PREFIX_INCREMENT,UnaryExpr.Operator.PREFIX_DECREMENT).contains(u.getOperator()));
            if (mutated) throw JavaSourcePreparation.error(callback,"Captured local must be effectively final: " + capture);
            generated.addField(variables.get(capture),capture,Modifier.Keyword.PRIVATE);
            constructor.addParameter(variables.get(capture),capture);
            constructor.getBody().addStatement("this."+capture+" = "+capture+";");
            creation.addArgument(capture);
        }
        // Lexical this and unqualified owner members keep their original receiver.
        boolean needsThis = !body.findAll(ThisExpr.class).isEmpty();
        for (NameExpr use : new ArrayList<>(body.findAll(NameExpr.class))) {
            String field = use.getNameAsString();
            if (captures.contains(field) || parameterNames.contains(field)) continue;
            var declaration = owner.getFields().stream().filter(f -> f.getVariables().stream().anyMatch(v -> v.getNameAsString().equals(field))).findFirst();
            if (declaration.isPresent()) {
                boolean statik = declaration.get().isStatic(); needsThis |= !statik;
                use.replace(new FieldAccessExpr(new NameExpr(statik ? owner.getNameAsString() : "__owner"),field));
            }
        }
        for (MethodCallExpr call : body.findAll(MethodCallExpr.class)) if (call.getScope().isEmpty() && !owner.getMethodsByName(call.getNameAsString()).isEmpty()) {
            boolean statik = owner.getMethodsByName(call.getNameAsString()).stream().allMatch(MethodDeclaration::isStatic);
            needsThis |= !statik; call.setScope(new NameExpr(statik ? owner.getNameAsString() : "__owner"));
        }
        if (needsThis) {
            body.findAll(ThisExpr.class).forEach(t -> t.replace(new NameExpr("__owner")));
            generated.addField(owner.getNameAsString(),"__owner",Modifier.Keyword.PRIVATE);
            constructor.addParameter(owner.getNameAsString(),"__owner");
            constructor.getBody().addStatement("this.__owner = __owner;"); creation.addArgument(new ThisExpr());
        }
        callback.replace(creation); cu.getTypes().add(generated);
    }
}
