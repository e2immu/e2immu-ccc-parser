package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.element.Source;
import org.e2immu.cstapi.expression.Expression;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.cstapi.statement.LocalVariableCreation;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.cstapi.variable.LocalVariable;
import org.e2immu.parserapi.Context;
import org.parsers.java.ast.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;


public class ParseStatement extends CommonParse {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParseStatement.class);
    private final ParseBlock parseBlock;
    private final ParseExpression parseExpression;
    private final ParseType parseType;

    private static final String FIRST_BLOCK = ".0";

    public ParseStatement(Runtime runtime) {
        super(runtime);
        parseExpression = new ParseExpression(runtime);
        parseType = new ParseType(runtime);
        parseBlock = new ParseBlock(runtime, this);
    }

    public org.e2immu.cstapi.statement.Statement parse(Context context, String index, Statement statement) {
        try {
            return internalParse(context, index, statement);
        } catch (Throwable t) {
            LOGGER.error("Caught exception parsing statement at line {}", statement.getBeginLine());
            throw t;
        }
    }

    private org.e2immu.cstapi.statement.Statement internalParse(Context context, String index, Statement statement) {
        List<Comment> comments = comments(statement);
        Source source = source(context.enclosingMethod(), index, statement);

        if (statement instanceof ExpressionStatement es) {
            StatementExpression se = (StatementExpression) es.children().get(0);
            Expression e = parseExpression.parse(context, index, se.get(0));
            return runtime.newExpressionAsStatement(e);
        }
        if (statement instanceof ReturnStatement rs) {
            org.e2immu.cstapi.expression.Expression e = parseExpression.parse(context, index, rs.get(1));
            assert e != null;
            return runtime.newReturnStatementBuilder()
                    .setExpression(e).setSource(source).addComments(comments)
                    .build();
        }
        if (statement instanceof NoVarDeclaration nvd) {
            ParameterizedType type = parseType.parse(context, nvd.get(0));
            if (nvd.get(1) instanceof VariableDeclarator vd) {
                Identifier identifier = (Identifier) vd.get(0);
                Expression expression;
                if (vd.size() > 2) {
                    expression = parseExpression.parse(context, index, vd.get(2));
                } else {
                    expression = runtime.newEmptyExpression();
                }
                String variableName = identifier.getSource();
                LocalVariable lv = runtime.newLocalVariable(variableName, type, expression);
                context.variableContext().add(lv);
                return runtime.newLocalVariableCreation(lv);
            }
        }
        if (statement instanceof EnhancedForStatement enhancedFor) {
            // kw, del, noVarDecl (objectType, vardcl), operator, name (Name), del, code block
            LocalVariableCreation loopVariableCreation;
            Expression expression = parseExpression.parse(context, index, enhancedFor.get(4));
            Context newContext = context.newVariableContext("forEach");
            if (enhancedFor.get(2) instanceof NoVarDeclaration nvd) {
                loopVariableCreation = (LocalVariableCreation) parse(newContext, index, nvd);
            } else throw new UnsupportedOperationException();
            Block block;
            if (enhancedFor.get(6) instanceof CodeBlock codeBlock) {
                block = parseBlock.parse(newContext, index + FIRST_BLOCK, codeBlock);
            } else throw new UnsupportedOperationException();
            return runtime.newForEachBuilder().setInitializer(loopVariableCreation)
                    .setBlock(block).setExpression(expression).build();
        }
        if (statement instanceof WhileStatement whileStatement) {
            Expression expression = parseExpression.parse(context, index, whileStatement.get(2));
            Block block;
            if (whileStatement.get(4) instanceof CodeBlock codeBlock) {
                block = parseBlock.parse(context, index + FIRST_BLOCK, codeBlock);
            } else throw new UnsupportedOperationException();
            return runtime.newWhileStatementBuilder().setExpression(expression).setBlock(block)
                    .setSource(source).addComments(comments).build();
        }
        throw new UnsupportedOperationException("Node " + statement.getClass());
    }
}
