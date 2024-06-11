package org.e2immu.parser.java;

import org.e2immu.cstapi.expression.*;
import org.e2immu.cstapi.expression.Expression;
import org.e2immu.cstapi.info.FieldInfo;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.cstapi.variable.FieldReference;
import org.e2immu.cstapi.variable.Variable;
import org.e2immu.parserapi.Context;
import org.parsers.java.Node;
import org.parsers.java.ast.*;
import org.parsers.java.ast.MethodCall;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.parsers.java.Token.TokenType.*;

public class ParseExpression extends CommonParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseExpression.class);
    private final ParseMethodCall parseMethodCall;
    private final ParseType parseType;

    public ParseExpression(Runtime runtime) {
        super(runtime);
        parseType = new ParseType(runtime);
        parseMethodCall = new ParseMethodCall(runtime, this);
    }

    public Expression parse(Context context, Node node) {
        try {
            return internalParse(context, node);
        } catch (Throwable t) {
            LOGGER.error("Caught exception parsing expression at line {}, pos {}", node.getBeginLine(), node.getBeginColumn());
            throw t;
        }
    }

    private Expression internalParse(Context context, Node node) {
        if (node instanceof DotName dotName) {
            return parseDotName(context, dotName);
        }
        if (node instanceof MethodCall mc) {
            return parseMethodCall.parse(context, mc);
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
        if (node instanceof UnaryExpression ue) {
            return parseUnaryExpression(context, ue);
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
        if (node instanceof CastExpression castExpression) {
            return parseCast(context, castExpression);
        }
        if (node instanceof AssignmentExpression assignmentExpression) {
            return parseAssignment(context, assignmentExpression);
        }
        if (node instanceof Parentheses p) {
            return parseParentheses(context, p);
        }
        throw new UnsupportedOperationException("node " + node.getClass());
    }

    private Expression parseParentheses(Context context, Parentheses p) {
        Expression e = parse(context, p.getNestedExpression());
        return runtime.newEnclosedExpression(e);
    }

    private Expression parseUnaryExpression(Context context, UnaryExpression ue) {
        MethodInfo methodInfo;
        if (ue.get(0) instanceof Operator operator) {
            methodInfo = switch (operator.getType()) {
                case PLUS -> null; // ignore!
                case MINUS -> runtime.minusOperatorInt();
                case BANG -> runtime.logicalNotOperatorBool();
                case TILDE -> runtime.bitWiseNotOperatorInt();
                default -> throw new UnsupportedOperationException();
            };
        } else throw new UnsupportedOperationException();
        Expression expression = parse(context, ue.get(1));
        if (methodInfo == null) {
            return expression;
        }
        return runtime.newUnaryOperator(methodInfo, expression, runtime.precedenceUNARY());
    }

    private VariableExpression parseDotName(Context context, DotName dotName) {
        String name = dotName.get(2).getSource();
        Expression scope;
        FieldInfo fieldInfo;
        Node n0 = dotName.get(0);
        if (n0 instanceof LiteralExpression le) {
            if ("this".equals(le.getAsString())) {
                scope = runtime.newVariableExpression(runtime.newThis(context.enclosingType()));
                fieldInfo = context.enclosingType().getFieldByName(name, true);
            } else throw new UnsupportedOperationException("NYI");
        } else {
            scope = parse(context, n0);
            throw new UnsupportedOperationException();
        }
        FieldReference fr = runtime.newFieldReference(fieldInfo, scope, fieldInfo.type()); // FIXME generics
        return runtime.newVariableExpression(fr);
    }

    private Assignment parseAssignment(Context context, AssignmentExpression assignmentExpression) {
        Expression target = parse(context, assignmentExpression.get(0));
        Expression value = parse(context, assignmentExpression.get(2));
        return runtime.newAssignment(target, value);
    }

    private Cast parseCast(Context context, CastExpression castExpression) {
        // 0 = '(', 2 = ')'
        ParameterizedType pt = parseType.parse(context, castExpression.get(1));
        Expression expression = parse(context, castExpression.get(3));
        return runtime.newCast(expression, pt);
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
        if (child instanceof CharacterLiteral cl) {
            char c = cl.charAt(1);
            if (c == '\\') {
                char c2 = cl.charAt(2);
                c = switch (c2) {
                    case 'b' -> '\b';
                    case 'r' -> '\r';
                    case 't' -> '\t';
                    case 'n' -> '\n';
                    case 'f' -> '\f';
                    case '\'' -> '\'';
                    case '\\' -> '\\';
                    default -> throw new UnsupportedOperationException();
                };
            }
            return runtime.newChar(c);
        }
        if(child instanceof StringLiteral sl) {
            return runtime.newStringConstant(sl.getString());
        }
        throw new UnsupportedOperationException("literal expression " + le.getClass());
    }
}
