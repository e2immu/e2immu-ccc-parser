package org.e2immu.parser.java;

import org.e2immu.cstapi.info.*;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.cstimpl.info.MethodInfoImpl;
import org.e2immu.cstimpl.type.ParameterizedTypeImpl;
import org.e2immu.parserapi.Context;
import org.e2immu.parserimpl.ForwardTypeImpl;
import org.parsers.java.Node;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.List;

public class ParseMethodDeclaration extends CommonParse {
    private final ParseType parseType;

    public ParseMethodDeclaration(Runtime runtime) {
        super(runtime);
        parseType = new ParseType(runtime);
    }

    public MethodInfo parse(Context context, MethodDeclaration md) {
        int i = 0;
        List<MethodModifier> methodModifiers = new ArrayList<>();
        if (md.get(i) instanceof Modifiers modifiers) {
            for (Node node : modifiers.children()) {
                if (node instanceof KeyWord keyWord) {
                    methodModifiers.add(modifier(keyWord));
                }
            }
            i++;
        } else if (md.get(i) instanceof KeyWord keyWord) {
            methodModifiers.add(modifier(keyWord));
            i++;
        }

        MethodInfo.MethodType methodType;
        ParameterizedType returnType;
        if (md.children().get(i) instanceof ReturnType rt) {
            // depending on the modifiers...
            methodType = runtime.newMethodTypeMethod();
            returnType = parseType.parse(context, rt.children());
            i++;
        } else if (md.children().get(i) instanceof Identifier) {
            methodType = runtime.newMethodTypeConstructor();
            returnType = runtime.parameterizedTypeRETURN_TYPE_OF_CONSTRUCTOR();
        } else throw new UnsupportedOperationException();
        String name;
        if (md.children().get(i) instanceof Identifier identifier) {
            name = identifier.getSource();
            i++;
        } else throw new UnsupportedOperationException();
        MethodInfo methodInfo = runtime.newMethod(context.enclosingType(), name, methodType);
        MethodInfo.Builder builder = methodInfo.builder().setReturnType(returnType);
        if (md.children().get(i) instanceof FormalParameters fps) {
            for (Node child : fps.children()) {
                if (child instanceof FormalParameter fp) {
                    parseFormalParameter(context, builder, fp);
                }
            }
            i++;
        } else throw new UnsupportedOperationException("Node " + md.children().get(i).getClass());
        while (i < md.children().size() && md.children().get(i) instanceof Delimiter) i++;
        if (i < md.children().size() && md.children().get(i) instanceof CodeBlock codeBlock) {
            ForwardTypeImpl forwardType = new ForwardTypeImpl(returnType);
            Context newContext = context.newVariableContextForMethodBlock(methodInfo, forwardType);
            /*
             delay the parsing of the code-block for a second phase, when all methods are known so that they can
             be resolved
             */
            context.resolver().add(builder, codeBlock, newContext);
        } else {
            builder.setMethodBody(runtime.emptyBlock());
        }
        builder.commitParameters();
        methodModifiers.forEach(builder::addMethodModifier);
        Access access = access(methodModifiers);
        Access accessCombined = context.enclosingType().access().combine(access);
        builder.setAccess(accessCombined);
        builder.addComments(comments(md));
        builder.setSource(source(methodInfo, md));
        return methodInfo;
    }

    private void parseFormalParameter(Context context, MethodInfo.Builder builder, FormalParameter fp) {
        ParameterizedType typeOfParameter;
        Node node0 = fp.children().get(0);
        if (node0 instanceof Type type) {
            typeOfParameter = parseType.parse(context, type);
        }  else{
            throw new UnsupportedOperationException();
        }
        String parameterName;
        Node node1 = fp.children().get(1);
        if (node1 instanceof Identifier identifier) {
            parameterName = identifier.getSource();
        } else throw new UnsupportedOperationException();
        ParameterInfo pi = builder.addParameter(parameterName, typeOfParameter);
        ParameterInfo.Builder piBuilder = pi.builder();
        // do not commit yet!
        context.variableContext().add(pi);
    }


    private Access access(List<MethodModifier> methodModifiers) {
        for (MethodModifier methodModifier : methodModifiers) {
            if (methodModifier.isPublic()) return runtime.newAccessPublic();
            if (methodModifier.isPrivate()) return runtime.newAccessPrivate();
            if (methodModifier.isProtected()) return runtime.newAccessProtected();
        }
        return runtime.newAccessPackage();
    }

    private MethodModifier modifier(KeyWord keyWord) {
        return switch (keyWord.getType()) {
            case FINAL -> runtime.newMethodModifierFinal();
            case PRIVATE -> runtime.newMethodModifierPrivate();
            case PROTECTED -> runtime.newMethodModifierProtected();
            case PUBLIC -> runtime.newMethodModifierPublic();
            case STATIC -> runtime.newMethodModifierStatic();
            case SYNCHRONIZED -> runtime.newMethodModifierSynchronized();
            case ABSTRACT -> runtime.newMethodModifierAbstract();
            case _DEFAULT -> runtime.newMethodModifierDefault();
            default -> throw new UnsupportedOperationException("Have " + keyWord.getType());
        };
    }
}
