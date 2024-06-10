package org.e2immu.parser.java;

import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.ParameterInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.cstimpl.info.MethodInfoImpl;
import org.e2immu.cstimpl.type.ParameterizedTypeImpl;
import org.e2immu.parserapi.Context;
import org.e2immu.parserimpl.ForwardTypeImpl;
import org.parsers.java.Node;
import org.parsers.java.ast.*;

public class ParseMethodDeclaration extends CommonParse {
    private final ParseBlock parseBlock;
    private final ParseType parseType;

    public ParseMethodDeclaration(Runtime runtime) {
        super(runtime);
        parseBlock = new ParseBlock(runtime);
        parseType = new ParseType(runtime);
    }

    public MethodInfo parse(Context context, MethodDeclaration md) {
        int i = 0;
        if (md.children().get(i) instanceof Modifiers modifiers) {
            i++;
        }
        MethodInfo.MethodType methodType;
        ParameterizedType returnType;
        if (md.children().get(i) instanceof ReturnType rt) {
            // depending on the modifiers...
            methodType = runtime.newMethodTypeMethod();
            returnType = parseType.parse(rt.children());
            i++;
        } else if (md.children().get(i) instanceof Identifier) {
            methodType = MethodInfoImpl.MethodTypeEnum.CONSTRUCTOR; // FIXME
            returnType = ParameterizedTypeImpl.RETURN_TYPE_OF_CONSTRUCTOR; // FIXME
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
                    parseFormalParameter(builder, fp);
                }
            }
            i++;
        } else throw new UnsupportedOperationException("Node " + md.children().get(i).getClass());
        while (i < md.children().size() && md.children().get(i) instanceof Delimiter) i++;
        if (i < md.children().size() && md.children().get(i) instanceof CodeBlock codeBlock) {
            ForwardTypeImpl forwardType = new ForwardTypeImpl(returnType);
            Context newContext = context.newVariableContextForMethodBlock(methodInfo, forwardType);
            Block block = parseBlock.parse(newContext, codeBlock);
            builder.setMethodBody(block);
        }
        builder.commitParameters();

        builder.addComments(comments(md));
        builder.setSource(source(methodInfo, md));
        return methodInfo;
    }

    private void parseFormalParameter(MethodInfo.Builder builder, FormalParameter fp) {
        ParameterizedType typeOfParameter;
        Node node0 = fp.children().get(0);
        if (node0 instanceof PrimitiveType primitiveType) {
            typeOfParameter = parseType.parse(primitiveType);
        } else if (node0 instanceof ReferenceType referenceType) {
            typeOfParameter = parseType.parse(referenceType.children());
        } else throw new UnsupportedOperationException();
        String parameterName;
        Node node1 = fp.children().get(1);
        if (node1 instanceof Identifier identifier) {
            parameterName = identifier.getSource();
        } else throw new UnsupportedOperationException();
        ParameterInfo pi = builder.addParameter(parameterName, typeOfParameter);
        ParameterInfo.Builder piBuilder = pi.builder();
        // do not commit!
    }
}
