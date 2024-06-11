package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.element.CompilationUnit;
import org.e2immu.cstapi.info.*;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.TypeNature;
import org.e2immu.cstimpl.info.InspectionImpl;
import org.e2immu.cstimpl.info.TypeInfoImpl;
import org.e2immu.cstimpl.info.TypeModifierEnum;
import org.e2immu.cstimpl.info.TypeNatureEnum;
import org.e2immu.parserapi.Context;
import org.e2immu.support.Either;
import org.jetbrains.annotations.Nullable;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ParseTypeDeclaration extends CommonParse {
    private final ParseConstructorDeclaration parseConstructorDeclaration;
    private final ParseMethodDeclaration parseMethodDeclaration;
    private final ParseAnnotationMethodDeclaration parseAnnotationMethodDeclaration;
    private final ParseFieldDeclaration parseFieldDeclaration;

    public ParseTypeDeclaration(Runtime runtime) {
        super(runtime);
        parseMethodDeclaration = new ParseMethodDeclaration(runtime);
        parseAnnotationMethodDeclaration = new ParseAnnotationMethodDeclaration(runtime);
        parseFieldDeclaration = new ParseFieldDeclaration(runtime);
        parseConstructorDeclaration = new ParseConstructorDeclaration(runtime);
    }

    public TypeInfo parse(Context context,
                          Either<CompilationUnit, TypeInfo> packageNameOrEnclosing,
                          TypeDeclaration td) {
        List<Comment> comments = comments(td);

        int i = 0;
        TypeNature typeNature = null;
        List<TypeModifier> typeModifiers = new ArrayList<>();
        if (td.get(i) instanceof Modifiers modifiers) {
            for (Node node : modifiers.children()) {
                if (node instanceof KeyWord keyWord) {
                    typeModifiers.add(getTypeModifier(keyWord.getType()));
                }
            }
            i++;
        }
        while (td.get(i) instanceof Delimiter) i++; // @ in @interface
        while (td.get(i) instanceof KeyWord keyWord) {
            TypeModifier tm = getTypeModifier(keyWord.getType());
            if (tm != null) typeModifiers.add(tm);
            TypeNature tn = getTypeNature(td, keyWord.getType());
            if (tn != null) {
                assert typeNature == null;
                typeNature = tn;
            }
            i++;
            while (td.get(i) instanceof Delimiter) i++; // @ in @interface
        }
        if (typeNature == null) throw new UnsupportedOperationException("Have not determined type nature");
        String simpleName;
        if (td.get(i) instanceof Identifier identifier) {
            simpleName = identifier.getSource();
            i++;
        } else throw new UnsupportedOperationException();
        TypeInfo typeInfo;
        if (packageNameOrEnclosing.isLeft()) {
            typeInfo = runtime.newTypeInfo(packageNameOrEnclosing.getLeft(), simpleName);
        } else {
            typeInfo = runtime.newTypeInfo(packageNameOrEnclosing.getRight(), simpleName);
        }
        TypeInfo.Builder builder = typeInfo.builder();
        builder.addComments(comments);
        typeModifiers.forEach(builder::addTypeModifier);
        Access access = access(typeModifiers);
        Access accessCombined = packageNameOrEnclosing.isLeft() ? access
                : packageNameOrEnclosing.getRight().access().combine(access);
        builder.setAccess(accessCombined);
        builder.setTypeNature(typeNature);
        builder.setSource(source(typeInfo, td));

        if (td.get(i) instanceof ExtendsList extendsList) {
            for (Node child : extendsList.children()) {

            }
            i++;
        }

        if (td.get(i) instanceof ImplementsList implementsList) {
            for (Node child : implementsList.children()) {

            }
            i++;
        }

        Context newContext = context.newSubType(typeInfo);


        Node body = td.get(i);
        if (body instanceof ClassOrInterfaceBody) {
            List<TypeDeclaration> typeDeclarations = new ArrayList<>();
            List<FieldDeclaration> fieldDeclarations = new ArrayList<>();
            int countCompactConstructors = 0;
            int countNormalConstructors = 0;

            for (Node child : body.children()) {
                if (child instanceof TypeDeclaration cid) typeDeclarations.add(cid);
                else if (child instanceof CompactConstructorDeclaration) ++countCompactConstructors;
                else if (child instanceof ConstructorDeclaration) ++countNormalConstructors;
                else if (child instanceof FieldDeclaration fd) fieldDeclarations.add(fd);
            }

            // FIRST, do subtypes

            for (TypeDeclaration typeDeclaration : typeDeclarations) {
                TypeInfo subTypeInfo = parse(newContext, Either.right(typeInfo), typeDeclaration);
                builder.addSubType(subTypeInfo);
            }

            // THEN, all sorts of methods and constructors

            for (Node child : body.children()) {
                if (child instanceof MethodDeclaration md) {
                    MethodInfo methodInfo = parseMethodDeclaration.parse(newContext, md);
                    builder.addMethod(methodInfo);
                } else if (child instanceof ConstructorDeclaration cd) {
                    MethodInfo constructor = parseConstructorDeclaration.parse(newContext, cd);
                    builder.addConstructor(constructor);
                }
            }

            // FINALLY, do the fields
            for (FieldDeclaration fieldDeclaration : fieldDeclarations) {
                FieldInfo field = parseFieldDeclaration.parse(newContext, fieldDeclaration);
                builder.addField(field);
            }
        } else if (body instanceof AnnotationTypeBody) {
            for (Node child : body.children()) {
                if (child instanceof AnnotationMethodDeclaration amd) {
                    MethodInfo methodInfo = parseAnnotationMethodDeclaration.parse(newContext, amd);
                    builder.addMethod(methodInfo);
                }
            }
        } else throw new UnsupportedOperationException("node " + td.get(i).getClass());

        // finally we do the fields

        context.resolver().add(builder);
        return typeInfo;
    }

    private TypeNature getTypeNature(TypeDeclaration td, Token.TokenType tt) {
        return switch (tt) {
            case CLASS -> runtime.newTypeNatureClass();
            case INTERFACE -> td instanceof AnnotationTypeDeclaration
                    ? runtime.newTypeNatureAnnotation() : runtime.newTypeNatureInterface();
            case ENUM -> runtime.newTypeNatureEnum();
            case RECORD -> runtime.newTypeNatureRecord();
            default -> null;
        };
    }

    private Access access(List<TypeModifier> typeModifiers) {
        for (TypeModifier typeModifier : typeModifiers) {
            if (typeModifier.isPublic()) return runtime.newAccessPublic();
            if (typeModifier.isPrivate()) return runtime.newAccessPrivate();
            if (typeModifier.isProtected()) return runtime.newAccessProtected();
        }
        return runtime.newAccessPackage();
    }

    private TypeModifier getTypeModifier(Token.TokenType tt) {
        return switch (tt) {
            case PUBLIC -> runtime.newTypeModifierPublic();
            case PRIVATE -> runtime.newTypeModifierPrivate();
            case PROTECTED -> runtime.newTypeModifierProtected();
            case FINAL -> runtime.newTypeModifierFinal();
            case SEALED -> runtime.newTypeModifierSealed();
            case ABSTRACT -> runtime.newTypeModifierAbstract();
            case NON_SEALED -> runtime.newTypeModifierNonSealed();
            case STATIC -> runtime.newTypeModifierStatic();
            default -> null;
        };
    }
}
