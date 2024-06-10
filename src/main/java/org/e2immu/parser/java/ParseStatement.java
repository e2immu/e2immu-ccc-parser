package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.element.Source;
import org.e2immu.cstapi.expression.Expression;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.LocalVariableCreation;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.cstapi.variable.LocalVariable;
import org.e2immu.parserapi.Context;
import org.e2immu.parserapi.TypeContext;
import org.parsers.java.ast.*;


import java.util.List;


public class ParseStatement extends CommonParse {
    private final ParseExpression parseExpression;
    private final ParseType parseType;

    public ParseStatement(Runtime runtime) {
        super(runtime);
        parseExpression = new ParseExpression(runtime);
        parseType = new ParseType(runtime);
    }

    public org.e2immu.cstapi.statement.Statement parse(Context context, Statement statement) {
        List<Comment> comments = comments(statement);
        Source source = source(context.enclosingMethod(), statement);

        if (statement instanceof ExpressionStatement es) {
            StatementExpression se = (StatementExpression) es.children().get(0);
            Expression e = parseExpression.parse(context, se.get(0));
            return runtime.newExpressionAsStatement(e);
        }
        if (statement instanceof ReturnStatement rs) {
            org.e2immu.cstapi.expression.Expression e = parseExpression.parse(context, rs.get(1));
            return runtime.newReturnStatementBuilder()
                    .setExpression(e).setSource(source).addComments(comments)
                    .build();
        }
        if (statement instanceof NoVarDeclaration nvd) {
            ParameterizedType type = parseType.parse(context, nvd.get(0));
            if (nvd.get(1) instanceof VariableDeclarator vd) {
                Identifier identifier = (Identifier) vd.get(0);
                Expression expression = parseExpression.parse(context, vd.get(2));
                String variableName = identifier.getSource();
                LocalVariable lv = runtime.newLocalVariable(variableName, type, expression);
                context.variableContext().add(lv);
                return runtime.newLocalVariableCreation(lv);
            }
        }
        throw new UnsupportedOperationException("Node " + statement.getClass());
    }
}
