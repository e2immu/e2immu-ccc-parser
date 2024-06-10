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

public class ParseAnnotationMethodDeclaration extends CommonParse {
    private final ParseType parseType;

    public ParseAnnotationMethodDeclaration(Runtime runtime) {
        super(runtime);
        parseType = new ParseType(runtime);
    }

    public MethodInfo parse(Context context, AnnotationMethodDeclaration amd) {
        assert context.enclosingType() != null;

        int i = 0;
        if (amd.children().get(i) instanceof Modifiers) {
            i++;
        }
        MethodInfo.MethodType methodType;
        ParameterizedType returnType;
        if (amd.children().get(i) instanceof Type type) {
            // depending on the modifiers...
            methodType = runtime.newMethodTypeMethod();
            returnType = parseType.parse(type);
            i++;
        } else throw new UnsupportedOperationException();
        String name;
        if (amd.children().get(i) instanceof Identifier identifier) {
            name = identifier.getSource();
            i++;
        } else throw new UnsupportedOperationException();
        MethodInfo methodInfo = runtime.newMethod(context.enclosingType(), name, methodType);
        MethodInfo.Builder builder = methodInfo.builder().setReturnType(returnType);


        builder.commitParameters();

        builder.addComments(comments(amd));
        builder.setSource(source(methodInfo, amd));
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
