package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.parserapi.Context;
import org.parsers.java.Node;
import org.parsers.java.ast.CodeBlock;
import org.parsers.java.ast.Statement;

import java.util.List;

public class ParseBlock extends CommonParse {
    private final ParseStatement parseStatement;

    public ParseBlock(Runtime runtime, ParseStatement parseStatement) {
        super(runtime);
        this.parseStatement = parseStatement;
    }

    public Block parse(Context context, CodeBlock codeBlock) {
        List<Comment> comments = comments(codeBlock);
        Block.Builder builder = runtime.newBlockBuilder();
        for (Node child : codeBlock.children()) {
            if (child instanceof Statement s) {
                org.e2immu.cstapi.statement.Statement statement = parseStatement.parse(context, s);
                builder.addStatement(statement);
            }
        }
        return builder.addComments(comments).build();
    }
}
