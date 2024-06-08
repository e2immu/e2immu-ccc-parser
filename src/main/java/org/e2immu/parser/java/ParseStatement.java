package org.e2immu.parser.java;

import org.e2immu.cstapi.info.Info;
import org.e2immu.cstapi.runtime.Runtime;
import org.parsers.java.Node;
import org.parsers.java.ast.ExpressionStatement;
import org.parsers.java.ast.MethodCall;
import org.parsers.java.ast.Statement;
import org.parsers.java.ast.StatementExpression;

public class ParseStatement {
    private final Runtime runtime;

    public ParseStatement(Runtime runtime) {
        this.runtime = runtime;
    }

    public org.e2immu.cstapi.statement.Statement parse(Info info, Statement statement) {
        if (statement instanceof ExpressionStatement es) {
            for (Node child : es.children()) {
                if (child instanceof StatementExpression se) {
                    return parseStatementExpression(info, se);
                }
            }
            return null;
        }
        throw new UnsupportedOperationException("Node " + statement.getClass());
    }

    private org.e2immu.cstapi.statement.Statement parseStatementExpression(Info info, StatementExpression se) {
        for (Node child : se.children()) {
            if (child instanceof MethodCall mc) {
                org.e2immu.cstapi.expression.MethodCall methodCall = parseMethodCall(info, mc);
                return runtime.newExpressionAsStatement(methodCall);
            }
        }
        throw new UnsupportedOperationException();
    }

    private org.e2immu.cstapi.expression.MethodCall parseMethodCall(Info info, MethodCall mc) {
        return runtime.newMethodCall(null, null, null);
    }
}
