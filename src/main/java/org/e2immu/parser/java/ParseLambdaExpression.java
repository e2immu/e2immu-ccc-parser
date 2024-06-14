package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.element.Source;
import org.e2immu.cstapi.expression.Expression;
import org.e2immu.cstapi.expression.Lambda;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.ParameterInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.cstapi.statement.Statement;
import org.e2immu.cstapi.type.NamedType;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.parserapi.Context;
import org.parsers.java.ast.Identifier;
import org.parsers.java.ast.LambdaExpression;
import org.parsers.java.ast.LambdaLHS;

import java.util.ArrayList;
import java.util.List;

public class ParseLambdaExpression extends CommonParse {
    private final ParseExpression parseExpression;

    public ParseLambdaExpression(Runtime runtime, ParseExpression parseExpression) {
        super(runtime);
        this.parseExpression = parseExpression;
    }

    private record NameType(String name, ParameterizedType type) {
    }

    public Expression parse(Context context,
                            List<Comment> comments,
                            Source source,
                            String index,
                            LambdaExpression le) {
        Lambda.Builder builder = runtime.newLambdaBuilder();
        List<NameType> params = new ArrayList<>();

        Context newContext = context.newVariableContext("lambda");
        List<Lambda.OutputVariant> outputVariants = new ArrayList<>();

        if (le.get(0) instanceof LambdaLHS lhs) {
            if (lhs.get(0) instanceof Identifier identifier) {
                // single variable
                params.add(new NameType(identifier.getSource(), null));
                outputVariants.add(runtime.lambdaOutputVariantEmpty());
            } else throw new UnsupportedOperationException();

        } else throw new UnsupportedOperationException();


        int typeIndex = context.anonymousTypeCounters().newIndex(context.enclosingType());
        TypeInfo anonymousType = runtime.newAnonymousType(context.enclosingType(), typeIndex);

        String samName = "?";
        List<ParameterizedType> typesOfSamParameters = new ArrayList<>();
        ParameterizedType concreteReturnType;
        Block methodBody;
        if (le.get(1) instanceof org.parsers.java.ast.Expression e) {
            // simple function or supplier
            Expression expression = parseExpression.parse(newContext, index, e);
            concreteReturnType = expression.parameterizedType();
            Statement returnStatement = runtime.newReturnStatement(expression);
            methodBody = runtime.newBlockBuilder().addStatement(returnStatement).build();
            // returns either java.util.function.Function<T,R> or java.util.function.Supplier<R>
            TypeInfo abstractFunctionalType = runtime.syntheticFunctionalType(params.size(), true);
            for (NameType nameType : params) {
                // add type parameter
            }
           // anonymousType.builder().addInterfaceImplemented(interfaceImplemented);
        } else throw new UnsupportedOperationException();

        MethodInfo methodInfo = runtime.newMethod(anonymousType, samName, runtime.methodTypeMethod());
        MethodInfo.Builder miBuilder = methodInfo.builder();
        miBuilder.setAccess(runtime.accessPrivate());
        miBuilder.setSynthetic(true);
        miBuilder.setMethodBody(methodBody);
        miBuilder.setReturnType(concreteReturnType);
        for (int i = 0; i < typesOfSamParameters.size(); i++) {
            miBuilder.addParameter("p" + i, typesOfSamParameters.get(i)).builder().commit();
        }
        miBuilder.commit();
        anonymousType.builder().addMethod(methodInfo);
        anonymousType.builder().commit();

        return builder
                .setMethodInfo(methodInfo)
                .setOutputVariants(outputVariants)
                .addComments(comments)
                .setSource(source)
                .build();
    }
}
