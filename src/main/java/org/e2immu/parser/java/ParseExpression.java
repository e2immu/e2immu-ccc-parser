package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.element.Source;
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

import java.util.List;

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

    public Expression parse(Context context, String index, Node node) {
        try {
            return internalParse(context, index, node);
        } catch (Throwable t) {
            LOGGER.error("Caught exception parsing expression at line {}, pos {}", node.getBeginLine(), node.getBeginColumn());
            throw t;
        }
    }

    private Expression internalParse(Context context, String index, Node node) {
        Source source = source(context.info(), index, node);
        List<Comment> comments = comments(node);

        if (node instanceof DotName dotName) {
            return parseDotName(context, index, dotName);
        }
        if (node instanceof MethodCall mc) {
            return parseMethodCall.parse(context, index, mc);
        }
        if (node instanceof LiteralExpression le) {
            return parseLiteral(context, le);
        }
        if (node instanceof MultiplicativeExpression me) {
            return parseMultiplicative(context, index, me);
        }
        if (node instanceof AdditiveExpression ae) {
            return parseAdditive(context, index, ae);
        }
        if (node instanceof RelationalExpression re) {
            return parseRelational(context, index, re);
        }
        if (node instanceof EqualityExpression eq) {
            return parseEquality(context, index, eq);
        }
        if (node instanceof UnaryExpression ue) {
            return parseUnaryExpression(context, index, ue);
        }
        if (node instanceof Name name) {
            String nameAsString = name.getAsString();
            if (nameAsString.endsWith(".length")) {
                Variable array = parseVariable(context, nameAsString.substring(0, nameAsString.length() - 7));
                assert array != null;
                return runtime.newArrayLength(runtime.newVariableExpression(array));
            }
            Variable v = parseVariable(context, nameAsString);
            return runtime.newVariableExpressionBuilder().setVariable(v).setSource(source).addComments(comments).build();
        }
        if (node instanceof CastExpression castExpression) {
            return parseCast(context, index, castExpression);
        }
        if (node instanceof AssignmentExpression assignmentExpression) {
            return parseAssignment(context, index, assignmentExpression);
        }
        if (node instanceof Parentheses p) {
            return parseParentheses(context, index, p);
        }
        throw new UnsupportedOperationException("node " + node.getClass());
    }

    private Expression parseParentheses(Context context, String index, Parentheses p) {
        Expression e = parse(context, index, p.getNestedExpression());
        return runtime.newEnclosedExpression(e);
    }

    private Expression parseUnaryExpression(Context context, String index, UnaryExpression ue) {
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
        Expression expression = parse(context, index, ue.get(1));
        if (methodInfo == null) {
            return expression;
        }
        return runtime.newUnaryOperator(methodInfo, expression, runtime.precedenceUnary());
    }

    private VariableExpression parseDotName(Context context, String index, DotName dotName) {
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
            scope = parse(context, index, n0);
            throw new UnsupportedOperationException();
        }
        FieldReference fr = runtime.newFieldReference(fieldInfo, scope, fieldInfo.type()); // FIXME generics
        return runtime.newVariableExpression(fr);
    }

    private Assignment parseAssignment(Context context, String index, AssignmentExpression assignmentExpression) {
        Expression target = parse(context, index, assignmentExpression.get(0));
        Expression value = parse(context, index, assignmentExpression.get(2));
        return runtime.newAssignment(target, value);
    }

    private Cast parseCast(Context context, String index, CastExpression castExpression) {
        // 0 = '(', 2 = ')'
        ParameterizedType pt = parseType.parse(context, castExpression.get(1));
        Expression expression = parse(context, index, castExpression.get(3));
        return runtime.newCast(expression, pt);
    }

    private Variable parseVariable(Context context, String name) {
        return context.variableContext().get(name, true);
    }

    private Expression parseAdditive(Context context, String index, AdditiveExpression ae) {
        Expression lhs = parse(context, index, ae.get(0));
        Expression rhs = parse(context, index, ae.get(2));
        Node.NodeType token = ae.get(1).getType();
        MethodInfo operator;
        if (token.equals(PLUS)) {
            operator = runtime.plusOperatorInt();
        } else if (token.equals(MINUS)) {
            operator = runtime.minusOperatorInt();
        } else {
            throw new UnsupportedOperationException();
        }
        ParameterizedType pt = runtime.widestType(lhs.parameterizedType(), rhs.parameterizedType());
        return runtime.newBinaryOperatorBuilder()
                .setOperator(operator)
                .setLhs(lhs).setRhs(rhs)
                .setParameterizedType(pt)
                .setPrecedence(runtime.precedenceAdditive())
                .setSource(source(context.info(), index, ae))
                .addComments(comments(ae))
                .build();
    }

    private Expression parseMultiplicative(Context context, String index, MultiplicativeExpression me) {
        Expression lhs = parse(context, index, me.get(0));
        Expression rhs = parse(context, index, me.get(2));
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
        ParameterizedType pt = runtime.widestType(lhs.parameterizedType(), rhs.parameterizedType());
        return runtime.newBinaryOperatorBuilder()
                .setOperator(operator)
                .setLhs(lhs).setRhs(rhs)
                .setParameterizedType(pt)
                .setPrecedence(runtime.precedenceMultiplicative())
                .setSource(source(context.info(), index, me))
                .addComments(comments(me))
                .build();
    }

    private Expression parseRelational(Context context, String index, RelationalExpression re) {
        Expression lhs = parse(context, index, re.get(0));
        Expression rhs = parse(context, index, re.get(2));
        Node.NodeType token = re.get(1).getType();
        MethodInfo operator;
        if (token.equals(LE)) {
            operator = runtime.lessEqualsOperatorInt();
        } else if (token.equals(LT)) {
            operator = runtime.lessOperatorInt();
        } else if (token.equals(GE)) {
            operator = runtime.greaterEqualsOperatorInt();
        } else if (token.equals(GT)) {
            operator = runtime.greaterOperatorInt();
        } else {
            throw new UnsupportedOperationException();
        }
        return runtime.newBinaryOperatorBuilder()
                .setOperator(operator)
                .setLhs(lhs).setRhs(rhs)
                .setParameterizedType(runtime.booleanParameterizedType())
                .setPrecedence(runtime.precedenceMultiplicative())
                .setSource(source(context.info(), index, re))
                .addComments(comments(re))
                .build();
    }

    private Expression parseEquality(Context context, String index, EqualityExpression eq) {
        Expression lhs = parse(context, index, eq.get(0));
        Expression rhs = parse(context, index, eq.get(2));
        Node.NodeType token = eq.get(1).getType();
        MethodInfo operator;
        boolean isNumeric = lhs.isNumeric();
        if (token.equals(EQ)) {
            operator = isNumeric ? runtime.equalsOperatorInt() : runtime.equalsOperatorObject();
        } else if (token.equals(NE)) {
            operator = isNumeric ? runtime.notEqualsOperatorInt() : runtime.notEqualsOperatorObject();
        } else throw new UnsupportedOperationException();
        return runtime.newBinaryOperatorBuilder()
                .setOperator(operator)
                .setLhs(lhs).setRhs(rhs)
                .setParameterizedType(runtime.booleanParameterizedType())
                .setPrecedence(runtime.precedenceMultiplicative())
                .setSource(source(context.info(), index, eq))
                .addComments(comments(eq))
                .build();
    }

    private Expression parseLiteral(Context context, LiteralExpression le) {
        Node child = le.children().get(0);
        if (child instanceof IntegerLiteral il) {
            return runtime.newInt(il.getValue());
        }
        if (child instanceof BooleanLiteral bl) {
            return runtime.newBoolean("true".equals(bl.getSource()));
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
        if (child instanceof StringLiteral sl) {
            return runtime.newStringConstant(sl.getString());
        }
        throw new UnsupportedOperationException("literal expression " + le.getClass());
    }
}
