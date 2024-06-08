package org.e2immu.parser.java;

import org.e2immu.cstapi.info.Info;
import org.e2immu.cstapi.runtime.Runtime;
import org.parsers.java.ast.ExpressionStatement;
import org.parsers.java.ast.Statement;

public class ParseStatement {

    public ParseStatement(Runtime runtime) {

    }

    public org.e2immu.cstapi.statement.Statement parse(Info info, Statement statement) {
        if (statement instanceof ExpressionStatement es) {
            return null;
        } else throw new UnsupportedOperationException("Node " + statement.getClass());
    }
}
