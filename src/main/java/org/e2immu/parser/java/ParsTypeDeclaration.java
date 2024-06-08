package org.e2immu.parser.java;

import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.TypeNature;
import org.e2immu.cstimpl.info.TypeInfoImpl;
import org.parsers.java.Node;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ParsTypeDeclaration {
    private final ParseMethodDeclaration parseMethodDeclaration;
    private final Runtime runtime;

    public ParsTypeDeclaration(Runtime runtime) {
        parseMethodDeclaration = new ParseMethodDeclaration(runtime);
        this.runtime = runtime;
    }

    public TypeInfo parse(String packageName, TypeDeclaration td) {
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
        if (td.children().get(i) instanceof ClassOrInterfaceBody bd) {
            for (Node child : bd.children()) {
                if (child instanceof MethodDeclaration md) {
                    MethodInfo methodInfo = parseMethodDeclaration.parse(typeInfo, md);
                    builder.addMethod(methodInfo);
                }
            }
        } else throw new UnsupportedOperationException("node " + td.children().get(i).getClass());

        return typeInfo;
    }
}
