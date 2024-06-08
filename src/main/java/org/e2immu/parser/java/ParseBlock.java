package org.e2immu.parser.java;

import org.e2immu.cstapi.info.Info;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.cstimpl.statement.BlockImpl;
import org.parsers.java.ast.CodeBlock;

public class ParseBlock {
    public ParseBlock(Runtime runtime) {

    }

    public Block parse(Info info, CodeBlock codeBlock) {
        Block.Builder builder = new BlockImpl.Builder();

        return builder.build();
    }
}
