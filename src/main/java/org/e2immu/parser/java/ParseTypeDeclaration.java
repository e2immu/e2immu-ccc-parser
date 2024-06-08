package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.TypeNature;
import org.e2immu.cstimpl.info.TypeInfoImpl;
import org.parsers.java.Node;
import org.parsers.java.ast.*;

import java.util.List;
import java.util.Objects;

public class ParseTypeDeclaration {
    private final ParseMethodDeclaration parseMethodDeclaration;
    private final Runtime runtime;

    public ParseTypeDeclaration(Runtime runtime) {
        parseMethodDeclaration = new ParseMethodDeclaration(runtime);
        this.runtime = runtime;
    }

    public TypeInfo parse(String packageName, TypeDeclaration td) {
        List<Comment> comments = td.getAllTokens(true).stream().map(t -> {
            if (t instanceof SingleLineComment slc) {
                return runtime.newSingleLineComment(slc.getSource());
            }
            return null;
        }).filter(Objects::nonNull).toList();

        TypeNature typeNature = null;
        int i = 0;
        while (td.children().get(i) instanceof KeyWord) {
            i++;
        }
        String simpleName;
        if (td.children().get(i) instanceof Identifier identifier) {
            simpleName = identifier.getSource();
            i++;
        } else throw new UnsupportedOperationException();
        TypeInfo typeInfo = new TypeInfoImpl(packageName, simpleName);
        TypeInfo.Builder builder = typeInfo.builder();
        builder.addComments(comments);
        if (td.children().get(i) instanceof ClassOrInterfaceBody bd) {
            for (Node child : bd.children()) {
                if (child instanceof MethodDeclaration md) {
                    MethodInfo methodInfo = parseMethodDeclaration.parse(typeInfo, md);
                    builder.addMethod(methodInfo);
                }
            }
        } else throw new UnsupportedOperationException("node " + td.children().get(i).getClass());

        builder.setSource(runtime.newParserSource(typeInfo, td.getBeginLine(), td.getBeginColumn(), td.getEndLine(),
                td.getEndColumn()));
        return typeInfo;
    }
}
