package org.e2immu.parser.java;

import org.e2immu.cstapi.expression.Expression;
import org.e2immu.cstapi.expression.MethodCall;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.NamedType;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.parserapi.Context;
import org.parsers.java.ast.Identifier;
import org.parsers.java.ast.InvocationArguments;
import org.parsers.java.ast.Name;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ParseMethodCall extends CommonParse {
    private final static Logger LOGGER = LoggerFactory.getLogger(ParseMethodCall.class);

    private final ParseExpression parseExpression;

    protected ParseMethodCall(Runtime runtime, ParseExpression parseExpression) {
        super(runtime);
        this.parseExpression = parseExpression;
    }

    public MethodCall parse(Context context, org.parsers.java.ast.MethodCall mc) {
        MethodCall.Builder builder = runtime.newMethodCallBuilder();
        Expression object = runtime.newVariableExpression(runtime.newThis(context.enclosingType()));

        int i = 0;
        String methodName;
        if (mc.get(i) instanceof Name name) {
            // TypeExpression?
            if (name.size() > 1 && name.get(0) instanceof Identifier id) {
                String typeName = id.getSource();
                NamedType nt = context.typeContext().get(typeName, false);
                if (nt instanceof TypeInfo ti) {
                    object = runtime.newTypeExpression(ti.asSimpleParameterizedType(), runtime.diamondNO());
                }
            }
            // name and possibly scope: System.out.println id, del, id, del, id
            int nameIndex = name.size() - 1;
            if (name.get(nameIndex) instanceof Identifier nameId) {
                methodName = nameId.getSource();
            } else throw new UnsupportedOperationException();
            i++;
        } else throw new UnsupportedOperationException();
        int numArguments;
        if (mc.get(i) instanceof InvocationArguments ia) {
            numArguments = (ia.size() - 1) / 2;
        } else throw new UnsupportedOperationException();

        // now we have scope, methodName, and the number of arguments
        // find a list of candidates
        // choose the correct candidate, and evaluate arguments
        // re-evaluate scope, and determine concrete return type
        TypeInfo methodOwner = object.parameterizedType().typeInfo();
        MethodInfo methodInfo = methodOwner.findUniqueMethod(methodName, numArguments);

        ParameterizedType concreteReturnType = methodInfo.returnType();

        // parse arguments
        List<Expression> expressions;

        // (, lit expr, )  or  del mc del mc, del expr del expr, del
        expressions = new ArrayList<>();
        for (int k = 1; k < ia.size(); k += 2) {
            Expression e = parseExpression.parse(context, ia.get(k));
            expressions.add(e);
        }

        return builder.setObject(object)
                .setParameterExpressions(expressions)
                .setMethodInfo(methodInfo)
                .setConcreteReturnType(concreteReturnType)
                .setSource(source(context.info(), mc))
                .addComments(comments(mc))
                .build();
    }
}
