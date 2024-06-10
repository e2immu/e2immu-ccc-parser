package org.e2immu.parser.java;

import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.parserapi.Context;
import org.e2immu.parserimpl.ContextImpl;
import org.e2immu.resourceapi.TypeMap;
import org.e2immu.support.Either;
import org.parsers.java.Node;
import org.parsers.java.ast.CompilationUnit;
import org.parsers.java.ast.PackageDeclaration;
import org.parsers.java.ast.TypeDeclaration;

import java.util.ArrayList;
import java.util.List;

public class ParseCompilationUnit extends CommonParse {
    private final Context rootContext;
    private final TypeMap.Builder typeMap;
    private final ParseTypeDeclaration parseTypeDeclaration;

    public ParseCompilationUnit(TypeMap.Builder typeMap, Context rootContext) {
        super(rootContext.runtime());
        this.rootContext = rootContext;
        this.typeMap = typeMap;
        parseTypeDeclaration = new ParseTypeDeclaration(runtime);
    }

    public List<TypeInfo> parse(CompilationUnit compilationUnit) {
        PackageDeclaration packageDeclaration = compilationUnit.getPackageDeclaration();
        String packageName = packageDeclaration.getName();
        org.e2immu.cstapi.element.CompilationUnit cu = runtime.newCompilationUnitBuilder()
                .setPackageName(packageName).build();
        Context newContext = rootContext.newCompilationUnit(typeMap, cu);
        List<TypeInfo> types = new ArrayList<>();
        for (Node child : compilationUnit.children()) {
            if (child instanceof TypeDeclaration cd) {
                TypeInfo typeInfo = parseTypeDeclaration.parse(newContext, Either.left(cu), cd);
                types.add(typeInfo);
            }
        }
        return types;
    }
}
