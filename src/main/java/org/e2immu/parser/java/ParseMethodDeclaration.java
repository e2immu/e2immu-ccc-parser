package org.e2immu.parser.java;

import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.ParameterInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.cstimpl.info.MethodInfoImpl;
import org.e2immu.cstimpl.type.ParameterizedTypeImpl;
import org.parsers.java.Node;
import org.parsers.java.ast.*;

public class ParseMethodDeclaration {
    private final Runtime runtime;
    private final ParseBlock parseBlock;
    private final ParseType parseType;

    public ParseMethodDeclaration(Runtime runtime) {
        this.runtime = runtime;
        parseBlock = new ParseBlock(runtime);
        parseType = new ParseType(runtime);
    }

    public MethodInfo parse(TypeInfo typeInfo, MethodDeclaration md) {
        int i = 0;
        if (md.children().get(i) instanceof Modifiers modifiers) {
            i++;
        }
        MethodInfo.MethodType methodType;
        ParameterizedType returnType;
        if (md.children().get(i) instanceof ReturnType rt) {
            // depending on the modifiers...
            methodType = MethodInfoImpl.MethodTypeEnum.METHOD;
            returnType = parseType.parse(rt.children());
            i++;
        } else if (md.children().get(i) instanceof Identifier) {
            methodType = MethodInfoImpl.MethodTypeEnum.CONSTRUCTOR;
            returnType = ParameterizedTypeImpl.RETURN_TYPE_OF_CONSTRUCTOR;
        } else throw new UnsupportedOperationException();
        String name;
        if (md.children().get(i) instanceof Identifier identifier) {
            name = identifier.getSource();
            i++;
        } else throw new UnsupportedOperationException();
        MethodInfo methodInfo = new MethodInfoImpl(methodType, name, typeInfo);
        MethodInfo.Builder builder = methodInfo.builder().setReturnType(returnType);
        if (md.children().get(i) instanceof FormalParameters fps) {
            for (Node child : fps.children()) {
                if (child instanceof FormalParameter fp) {
                    parseFormalParameter(builder, fp);
                }
            }
            i++;
        } else throw new UnsupportedOperationException("Node " + md.children().get(i).getClass());
        if (md.children().get(i) instanceof CodeBlock codeBlock) {
            Block block = parseBlock.parse(methodInfo, codeBlock);
            builder.setMethodBody(block);
        } else throw new UnsupportedOperationException("Node " + md.children().get(i).getClass());
        builder.commitParameters();
        return methodInfo;
    }

    private void parseFormalParameter(MethodInfo.Builder builder, FormalParameter fp) {
        int i = 0;
        ParameterizedType typeOfParameter;
        if (fp.children().get(i) instanceof ReferenceType referenceType) {
            typeOfParameter = parseType.parse(referenceType.children());
            i++;
        } else throw new UnsupportedOperationException();
        String parameterName;
        if (fp.children().get(i) instanceof Identifier identifier) {
            parameterName = identifier.getSource();
        } else throw new UnsupportedOperationException();
        ParameterInfo pi = builder.addParameter(parameterName, typeOfParameter);
        ParameterInfo.Builder piBuilder = pi.builder();
        // do not commit!
    }
}
