package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.element.CompilationUnit;
import org.e2immu.cstapi.info.Access;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.info.TypeModifier;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.TypeNature;
import org.e2immu.cstimpl.info.InspectionImpl;
import org.e2immu.cstimpl.info.TypeInfoImpl;
import org.e2immu.cstimpl.info.TypeModifierEnum;
import org.e2immu.cstimpl.info.TypeNatureEnum;
import org.e2immu.parserapi.Context;
import org.e2immu.support.Either;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.List;

public class ParseTypeDeclaration extends CommonParse {
    private final ParseMethodDeclaration parseMethodDeclaration;
    private final ParseAnnotationMethodDeclaration parseAnnotationMethodDeclaration;

    public ParseTypeDeclaration(Runtime runtime) {
        super(runtime);
        parseMethodDeclaration = new ParseMethodDeclaration(runtime);
        parseAnnotationMethodDeclaration = new ParseAnnotationMethodDeclaration(runtime);
    }

    public TypeInfo parse(Context context,
                          Either<CompilationUnit, TypeInfo> packageNameOrEnclosing,
                          TypeDeclaration td) {
        List<Comment> comments = comments(td);

        InspectionImpl.AccessEnum access = InspectionImpl.AccessEnum.PACKAGE;
        TypeNature typeNature = null;
        int i = 0;
        List<TypeModifier> typeModifiers = new ArrayList<>();
        while (!(td.children().get(i) instanceof Identifier)) {
            if (td.children().get(i) instanceof KeyWord keyWord) {
                Token.TokenType tt = keyWord.getType();
                TypeModifier typeModifier = switch (tt) {
                    case PUBLIC -> TypeModifierEnum.PUBLIC;
                    case PRIVATE -> TypeModifierEnum.PRIVATE;
                    case PROTECTED -> TypeModifierEnum.PROTECTED;
                    case FINAL -> TypeModifierEnum.FINAL;
                    case SEALED -> TypeModifierEnum.SEALED;
                    case ABSTRACT -> TypeModifierEnum.ABSTRACT;
                    case NON_SEALED -> TypeModifierEnum.NON_SEALED;
                    default -> null;
                };
                if (typeModifier != null) {
                    typeModifiers.add(typeModifier);
                    switch (tt) {
                        case PUBLIC -> access = InspectionImpl.AccessEnum.PUBLIC;
                        case PRIVATE -> access = InspectionImpl.AccessEnum.PRIVATE;
                        case PROTECTED -> access = InspectionImpl.AccessEnum.PROTECTED;
                    }
                }
                TypeNature tn = switch (tt) {
                    case CLASS -> TypeNatureEnum.CLASS;
                    case INTERFACE -> td instanceof AnnotationTypeDeclaration
                            ? TypeNatureEnum.ANNOTATION : TypeNatureEnum.INTERFACE;
                    case ENUM -> TypeNatureEnum.ENUM;
                    case RECORD -> TypeNatureEnum.RECORD;
                    default -> null;
                };
                if (tn != null) {
                    assert typeNature == null;
                    typeNature = tn;
                }
            }
            i++;
        }
        if (typeNature == null) throw new UnsupportedOperationException("Have not determined type nature");
        String simpleName;
        if (td.children().get(i) instanceof Identifier identifier) {
            simpleName = identifier.getSource();
            i++;
        } else throw new UnsupportedOperationException();
        TypeInfo typeInfo;
        if (packageNameOrEnclosing.isLeft()) {
            typeInfo = new TypeInfoImpl(packageNameOrEnclosing.getLeft(), simpleName);
        } else {
            typeInfo = new TypeInfoImpl(packageNameOrEnclosing.getRight(), simpleName);
        }
        TypeInfo.Builder builder = typeInfo.builder();
        builder.addComments(comments);
        typeModifiers.forEach(builder::addTypeModifier);
        Access accessCombined = packageNameOrEnclosing.isLeft() ? access
                : packageNameOrEnclosing.getRight().access().combine(access);
        builder.setAccess(accessCombined);
        builder.setTypeNature(typeNature);
        builder.setSource(source(typeInfo, td));

        if (td.children().get(i) instanceof ExtendsList extendsList) {
            for (Node child : extendsList.children()) {

            }
            i++;
        }

        if (td.children().get(i) instanceof ImplementsList implementsList) {
            for (Node child : implementsList.children()) {

            }
            i++;
        }

        Context newContext = context.newSubType(typeInfo);

        Node body = td.children().get(i);
        if (body instanceof ClassOrInterfaceBody) {
            for (Node child : body.children()) {
                if (child instanceof MethodDeclaration md) {
                    MethodInfo methodInfo = parseMethodDeclaration.parse(newContext, md);
                    builder.addMethod(methodInfo);
                }
                if (child instanceof TypeDeclaration subType) {
                    TypeInfo subTypeInfo = parse(newContext, Either.right(typeInfo), subType);
                    builder.addSubType(subTypeInfo);
                }
            }
        } else if (body instanceof AnnotationTypeBody) {
            for (Node child : body.children()) {
                if (child instanceof AnnotationMethodDeclaration amd) {
                       MethodInfo methodInfo = parseAnnotationMethodDeclaration.parse(newContext, amd);
                       builder.addMethod(methodInfo);
                }
            }
        } else throw new UnsupportedOperationException("node " + td.children().get(i).getClass());

        return typeInfo;
    }
}
