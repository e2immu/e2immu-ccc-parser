package org.e2immu.parser.java;

import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.cstimpl.type.ParameterizedTypeImpl;
import org.parsers.java.Node;
import org.parsers.java.ast.KeyWord;

import java.util.List;

public class ParseType {
    private final Runtime runtime;

    public ParseType(Runtime runtime) {
        this.runtime = runtime;
    }

    public ParameterizedType parse(List<Node> nodes) {
        if (nodes.get(0) instanceof KeyWord keyWord && nodes.size() == 1) {
            ParameterizedType primitive = switch (keyWord.getType()) {
                case INT -> runtime.intParameterizedType();
                case DOUBLE -> runtime.doubleParameterizedType();
                case BYTE -> runtime.byteParameterizedType();
                case BOOLEAN -> runtime.booleanParameterizedType();
                case FLOAT -> runtime.floatParameterizedType();
                case CHAR -> runtime.charParameterizedType();
                case VOID -> runtime.voidParameterizedType();
                case SHORT -> runtime.shortParameterizedType();
                case LONG -> runtime.longParameterizedType();
                default -> null;
            };
            if (primitive != null) {
                return primitive;
            }
        }
        return new ParameterizedTypeImpl();
    }
}
