package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.element.Source;
import org.e2immu.cstapi.expression.Expression;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.parserapi.Context;
import org.parsers.java.ast.ExpressionStatement;
import org.parsers.java.ast.ReturnStatement;
import org.parsers.java.ast.Statement;
import org.parsers.java.ast.StatementExpression;


import java.util.List;


public class ParseStatement extends CommonParse {
    private final ParseExpression parseExpression;

    public ParseStatement(Runtime runtime) {
        super(runtime);
        parseExpression = new ParseExpression(runtime);
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
        throw new UnsupportedOperationException("Node " + statement.getClass());
    }
}
