package org.e2immu.parser.java;

import org.e2immu.cstapi.expression.VariableExpression;
import org.e2immu.cstapi.info.FieldInfo;
import org.e2immu.cstapi.info.FieldModifier;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.cstapi.variable.FieldReference;
import org.e2immu.cstimpl.info.FieldInfoImpl;
import org.e2immu.parserapi.Context;
import org.parsers.java.Node;
import org.parsers.java.ast.*;

import java.util.ArrayList;
import java.util.List;

public class ParseFieldDeclaration extends CommonParse {
    private final ParseType parseType;

    public ParseFieldDeclaration(Runtime runtime) {
        super(runtime);
        parseType = new ParseType(runtime);
    }

    public FieldInfo parse(Context context, FieldDeclaration fd) {
        int i = 0;
        List<FieldModifier> fieldModifiers = new ArrayList<>();
        if (fd.get(i) instanceof Modifiers modifiers) {
            for (Node node : modifiers.children()) {
                if (node instanceof KeyWord keyWord) {
                    fieldModifiers.add(modifier(keyWord));
                }
            }
            i++;
        } else if (fd.get(i) instanceof KeyWord keyWord) {
            fieldModifiers.add(modifier(keyWord));
            i++;
        }
        boolean isStatic = fieldModifiers.stream().anyMatch(FieldModifier::isStatic);
        ParameterizedType parameterizedType;
        if (fd.get(i) instanceof Type type) {
            parameterizedType = parseType.parse(context, type);
            i++;
        } else throw new UnsupportedOperationException();
        String name;
        Expression expression = null;
        if (fd.get(i) instanceof VariableDeclarator vd) {
            int j = 0;
            if (vd.get(0) instanceof Identifier identifier) {
                name = identifier.getSource();
                j++;
            } else throw new UnsupportedOperationException();
        } else throw new UnsupportedOperationException();


        TypeInfo owner = context.enclosingType();
        FieldInfo fieldInfo = new FieldInfoImpl(name, isStatic, parameterizedType, owner);
        FieldInfo.Builder builder = fieldInfo.builder();

        fieldModifiers.forEach(builder::addFieldModifier);
        VariableExpression scope = runtime.newVariableExpression(runtime.newThis(fieldInfo.owner()));
        FieldReference fieldReference = runtime.newFieldReference(fieldInfo, scope);
        context.variableContext().add(fieldReference);
        if (expression != null) {
            context.resolver().add(fieldInfo.builder(), expression, context);
        }
        return fieldInfo;
    }

    private FieldModifier modifier(KeyWord keyWord) {
        return switch (keyWord.getType()) {
            case FINAL -> runtime.newFieldModifierFinal();
            case PRIVATE -> runtime.newFieldModifierPrivate();
            case PROTECTED -> runtime.newFieldModifierProtected();
            case PUBLIC -> runtime.newFieldModifierPublic();
            case STATIC -> runtime.newFieldModifierStatic();
            case TRANSIENT -> runtime.newFieldModifierTransient();
            case VOLATILE -> runtime.newFieldModifierVolatile();
            default -> throw new UnsupportedOperationException("Have " + keyWord.getType());
        };
    }
}
