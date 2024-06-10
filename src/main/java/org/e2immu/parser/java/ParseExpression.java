package org.e2immu.parser.java;

import org.e2immu.cstapi.expression.Expression;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.variable.Variable;
import org.e2immu.parserapi.Context;
import org.parsers.java.Node;
import org.parsers.java.ast.*;

import static org.parsers.java.Token.TokenType.*;

public class ParseExpression extends CommonParse {
    public ParseExpression(Runtime runtime) {
        super(runtime);
    }

    public Expression parse(Context context, Node node) {
        if (node instanceof MethodCall mc) {
            return parseMethodCall(context, mc);
        }
        if (node instanceof LiteralExpression le) {
            return parseLiteral(context, le);
        }
        if (node instanceof MultiplicativeExpression me) {
            return parseMultiplicative(context, me);
        }
        if (node instanceof AdditiveExpression ae) {
            return parseAdditive(context, ae);
        }
        if (node instanceof Name name) {
            String nameAsString = name.getAsString();
            if (nameAsString.endsWith(".length")) {
                Variable array = parseVariable(context, nameAsString.substring(0, nameAsString.length() - 7));
                assert array != null;
                return runtime.newArrayLength(runtime.newVariableExpression(array));
            }
            Variable v = parseVariable(context, nameAsString);
            return runtime.newVariableExpression(v);
        }
        throw new UnsupportedOperationException("node " + node.getClass());
    }

    private Variable parseVariable(Context context, String name) {
        return context.variableContext().get(name, true);
    }

    private Expression parseAdditive(Context context, AdditiveExpression ae) {
        Expression lhs = parse(context, ae.get(0));
        Expression rhs = parse(context, ae.get(2));
        Node.NodeType token = ae.get(1).getType();
        MethodInfo operator;
        if (token.equals(PLUS)) {
            operator = runtime.plusOperatorInt();
        } else if (token.equals(MINUS)) {
            operator = runtime.minusOperatorInt();
        } else {
            throw new UnsupportedOperationException();
        }
        return runtime.newBinaryOperator(lhs, operator, rhs, runtime.precedenceADDITIVE());
    }

    private Expression parseMultiplicative(Context context, MultiplicativeExpression me) {
        Expression lhs = parse(context, me.get(0));
        Expression rhs = parse(context, me.get(2));
        Node.NodeType token = me.get(1).getType();
        MethodInfo operator;
        if (token.equals(STAR)) {
            operator = runtime.multiplyOperatorInt();
        } else if (token.equals(SLASH)) {
            operator = runtime.divideOperatorInt();
        } else if (token.equals(REM)) {
            operator = runtime.remainderOperatorInt();
        } else {
            throw new UnsupportedOperationException();
        }
        return runtime.newBinaryOperator(lhs, operator, rhs, runtime.precedenceMULTIPLICATIVE());
    }

    private Expression parseLiteral(Context context, LiteralExpression le) {
        Node child = le.children().get(0);
        if (child instanceof IntegerLiteral il) {
            return runtime.newInt(il.getValue());
        }
        if (child instanceof BooleanLiteral bl) {
            return runtime.newBooleanConstant("true".equals(bl.getSource()));
        }
        throw new UnsupportedOperationException("literal expression " + le.getClass());
    }

    private org.e2immu.cstapi.expression.MethodCall parseMethodCall(Context context, MethodCall mc) {
        return runtime.newMethodCall(null, null, null);
    }
}
