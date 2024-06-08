package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.info.Info;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.parsers.java.Node;
import org.parsers.java.ast.CodeBlock;
import org.parsers.java.ast.Statement;

import java.util.List;

public class ParseBlock extends CommonParse {
    private final ParseStatement parseStatement;

    public ParseBlock(Runtime runtime) {
        super(runtime);
        parseStatement = new ParseStatement(runtime);
    }

    public Block parse(Info info, CodeBlock codeBlock) {
        List<Comment> comments = comments(codeBlock);
        Block.Builder builder = runtime.newBlockBuilder();
        for (Node child : codeBlock.children()) {
            if (child instanceof Statement s) {
                org.e2immu.cstapi.statement.Statement statement = parseStatement.parse(info, s);
                builder.addStatement(statement);
            }
        }
        return builder.addComments(comments).build();
    }
}
