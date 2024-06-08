package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.element.Source;
import org.e2immu.cstapi.info.Info;
import org.e2immu.cstapi.runtime.Runtime;
import org.parsers.java.Node;
import org.parsers.java.ast.MultiLineComment;
import org.parsers.java.ast.SingleLineComment;

import java.util.List;
import java.util.Objects;

public abstract class CommonParse {
    protected final Runtime runtime;

    protected CommonParse(Runtime runtime) {
        this.runtime = runtime;
    }

    protected List<Comment> comments(Node node) {
        return node.getAllTokens(true).stream().map(t -> {
            if (t instanceof SingleLineComment slc) {
                return runtime.newSingleLineComment(slc.getSource());
            }
            if (t instanceof MultiLineComment multiLineComment) {
                return runtime.newMultilineComment(multiLineComment.getSource());
            }
            return null;
        }).filter(Objects::nonNull).toList();
    }

    protected Source source(Info info, Node node) {
        return runtime.newParserSource(info, node.getBeginLine(), node.getBeginColumn(), node.getEndLine(),
                node.getEndColumn());
    }
}
