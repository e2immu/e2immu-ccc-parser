package org.e2immu.parserimpl;

import org.e2immu.cstapi.element.Element;
import org.e2immu.cstapi.info.FieldInfo;
import org.e2immu.cstapi.info.Info;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.cstapi.statement.Statement;
import org.e2immu.parser.java.ParseBlock;
import org.e2immu.parser.java.ParseExpression;
import org.e2immu.parser.java.ParseStatement;
import org.e2immu.parserapi.Context;
import org.e2immu.parserapi.Resolver;
import org.parsers.java.Node;
import org.parsers.java.ast.CodeBlock;
import org.parsers.java.ast.Expression;
import org.parsers.java.ast.ExpressionStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class ResolverImpl implements Resolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResolverImpl.class);

    private final ParseStatement parseStatement;
    private final ParseExpression parseExpression;
    private final ParseBlock parseBlock;
    private final Runtime runtime;

    public ResolverImpl(Runtime runtime) {
        this.parseExpression = new ParseExpression(runtime);
        this.parseStatement = new ParseStatement(runtime);
        this.parseBlock = new ParseBlock(runtime, parseStatement);
        this.runtime = runtime;
    }

    record Todo(Info.Builder<?> info, Node expression, Context context) {
    }

    private final List<Todo> todos = new LinkedList<>();
    private final List<TypeInfo.Builder> types = new LinkedList<>();

    @Override
    public void add(Info.Builder<?> info, Node expression, Context context) {
        todos.add(new Todo(info, expression, context));
    }

    @Override
    public void add(TypeInfo.Builder typeInfoBuilder) {
        types.add(typeInfoBuilder);
    }

    private static final String START_INDEX = "";

    @Override
    public void resolve() {
        LOGGER.info("Start resolving {} type(s), {} field(s)/method(s)", types.size(), todos.size());

        for (Todo todo : todos) {
            if (todo.info instanceof FieldInfo.Builder builder) {
                org.e2immu.cstapi.expression.Expression e = parseExpression.parse(todo.context, START_INDEX, todo.expression);
                builder.setInitializer(e);
                builder.commit();
            } else if (todo.info instanceof MethodInfo.Builder builder) {
                Element e;
                if (todo.expression instanceof ExpressionStatement est) {
                    e = parseStatement.parse(todo.context, START_INDEX, est);
                } else if (todo.expression instanceof CodeBlock codeBlock) {
                    e = parseBlock.parse(todo.context, START_INDEX, codeBlock);
                } else {
                    e = parseExpression.parse(todo.context, START_INDEX, todo.expression);
                }
                if (e instanceof Block b) {
                    builder.setMethodBody(b);
                } else if (e instanceof Statement s) {
                    builder.setMethodBody(runtime.newBlockBuilder().addStatement(s).build());
                } else {
                    // in Java, we must have a block
                    throw new UnsupportedOperationException();
                }
                builder.commit();
            } else throw new UnsupportedOperationException("In java, we cannot have expressions in other places");
        }
        for (TypeInfo.Builder builder : types) {
            builder.commit();
        }
    }
}
