package org.e2immu.parser.java;

import org.e2immu.cstapi.expression.Expression;
import org.e2immu.cstapi.info.Info;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;

import static org.parsers.java.Token.TokenType.*;

public class ParseExpression extends CommonParse {
    public ParseExpression(Runtime runtime) {
        super(runtime);
    }

    public Expression parse(Info info, Node node) {
        if (node instanceof MethodCall mc) {
            return parseMethodCall(info, mc);
        }
        if (node instanceof LiteralExpression le) {
            return parseLiteral(info, le);
        }
        if (node instanceof MultiplicativeExpression me) {
            return parseMultiplicative(info, me);
        }
        if (node instanceof AdditiveExpression ae) {
            return parseAdditive(info, ae);
        }
        if (node instanceof Name name) {
            // the name is unresolved at the moment!!!
            return runtime.newVariableExpression(runtime.newLocalVariable(name.getAsString(),
                    runtime.parameterizedTypeRETURN_TYPE_OF_CONSTRUCTOR()));
        }
        throw new UnsupportedOperationException("node " + node.getClass());
    }


    private Expression parseAdditive(Info info, AdditiveExpression ae) {
        Expression lhs = parse(info, ae.get(0));
        Expression rhs = parse(info, ae.get(2));
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

    private Expression parseMultiplicative(Info info, MultiplicativeExpression me) {
        Expression lhs = parse(info, me.get(0));
        Expression rhs = parse(info, me.get(2));
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

    private Expression parseLiteral(Info info, LiteralExpression le) {
        Node child = le.children().get(0);
        if (child instanceof IntegerLiteral il) {
            return runtime.newInt(il.getValue());
        }
        if (child instanceof BooleanLiteral bl) {
            return runtime.newBooleanConstant("true".equals(bl.getSource()));
        }
        throw new UnsupportedOperationException("literal expression " + le.getClass());
    }

    private org.e2immu.cstapi.expression.MethodCall parseMethodCall(Info info, MethodCall mc) {
        return runtime.newMethodCall(null, null, null);
    }
}
