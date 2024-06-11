package org.e2immu.parser.java;

import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.NamedType;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.cstimpl.type.ParameterizedTypeImpl;
import org.e2immu.parserapi.Context;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;

import java.util.List;

public class ParseType extends CommonParse {

    public ParseType(Runtime runtime) {
        super(runtime);
    }

    public ParameterizedType parse(Context context, List<Node> nodes) {
        Token.TokenType tt;
        ParameterizedType pt;
        Node n0 = nodes.get(0);
        if (n0 instanceof Identifier identifier) {
            NamedType nt = context.typeContext().get(identifier.getSource(), true);
            pt = ((TypeInfo) nt).asParameterizedType(context.runtime());
        } else if (n0 instanceof ObjectType ot && ot.get(0) instanceof Identifier id) {
            NamedType nt = context.typeContext().get(id.getSource(), true);
            pt = ((TypeInfo) nt).asParameterizedType(context.runtime());
        } else {
            if (n0 instanceof PrimitiveType primitive && primitive.get(0) instanceof Primitive p) {
                tt = p.getType();
            } else if (n0 instanceof KeyWord keyWord && nodes.size() == 1) {
                tt = keyWord.getType();
            } else tt = null;
            if (tt != null) {
                pt = switch (tt) {
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
            } else {
                pt = null;
            }
        }
        if (pt != null) {
            int arrays = countArrays(nodes, 1);
            if (arrays == 0) {
                return pt;
            } else {
                return new ParameterizedTypeImpl(pt.typeInfo(), arrays);
            }
        }
        return new ParameterizedTypeImpl();
    }

    private int countArrays(List<Node> nodes, int i) {
        int arrays = 0;
        while (i < nodes.size()) {
            if (nodes.get(i) instanceof Delimiter d && "[".equals(d.getSource())) {
                i += 2;
                arrays++;
            }
        }
        return arrays;
    }
}
