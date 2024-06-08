package org.e2immu.parser.java;

import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.parsers.java.Node;
import org.parsers.java.ast.CompilationUnit;
import org.parsers.java.ast.PackageDeclaration;
import org.parsers.java.ast.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

public class ParseCompilationUnit {

    private final ParseTypeDeclaration parseTypeDeclaration;

    public ParseCompilationUnit(Runtime runtime) {
        parseTypeDeclaration = new ParseTypeDeclaration(runtime);
    }

    public List<TypeInfo> parse(CompilationUnit compilationUnit) {
        PackageDeclaration packageDeclaration = compilationUnit.getPackageDeclaration();
        String packageName = packageDeclaration.getName();
        List<TypeInfo> types = new ArrayList<>();
        for (Node child : compilationUnit.children()) {
            if (child instanceof TypeDeclaration cd) {
                TypeInfo typeInfo = parseTypeDeclaration.parse(packageName, cd);
                types.add(typeInfo);
            }
        }
        return types;
    }
}
