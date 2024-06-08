package org.e2immu.parser.java;

import org.e2immu.cstapi.info.Info;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.cstimpl.statement.BlockImpl;
import org.parsers.java.Node;
import org.parsers.java.ast.CodeBlock;
import org.parsers.java.ast.Statement;

public class ParseBlock {
    private final ParseStatement parseStatement;

    public ParseBlock(Runtime runtime) {
        parseStatement = new ParseStatement(runtime);
    }

    public Block parse(Info info, CodeBlock codeBlock) {
        Block.Builder builder = new BlockImpl.Builder();
        for (Node child : codeBlock.children()) {
            if (child instanceof Statement s) {
                org.e2immu.cstapi.statement.Statement statement = parseStatement.parse(info, s);
                builder.addStatement(statement);
            }
        }
        return builder.build();
    }
}
