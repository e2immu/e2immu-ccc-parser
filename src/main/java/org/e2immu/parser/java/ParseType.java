package org.e2immu.parser.java;

import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.NamedType;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.parserapi.Context;
import org.parsers.java.Node;
import org.parsers.java.Token;
import org.parsers.java.ast.*;

import java.util.ArrayList;
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
            pt = nt.asSimpleParameterizedType();
        } else if (n0 instanceof ObjectType ot && ot.get(0) instanceof Identifier id) {
            NamedType nt = context.typeContext().get(id.getSource(), true);
            pt = nt.asParameterizedType(context.runtime());
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
        assert pt != null;
        ParameterizedType pt2;
        if (nodes.size() > 1 && nodes.get(1) instanceof TypeArguments tas) {
            List<ParameterizedType> typeArguments = new ArrayList<>();
            int j = 1;
            while (j < tas.size()) {
                if (tas.get(j) instanceof TypeArgument ta) {
                    ParameterizedType arg = parse(context, ta);
                    typeArguments.add(arg);
                } else if (tas.get(j) instanceof Operator o && Token.TokenType.HOOK.equals(o.getType())) {
                    typeArguments.add(runtime.parameterizedTypeWildcard());
                } else if(tas.get(j) instanceof Type type) {
                    ParameterizedType arg = parse(context, type);
                    typeArguments.add(arg);
                } else throw new UnsupportedOperationException();
                j += 2;

            }
            if (!typeArguments.isEmpty()) {
                pt2 = pt.withParameters(List.copyOf(typeArguments));
            } else {
                pt2 = pt;
            }
        } else {
            pt2 = pt;
        }
        int arrays = countArrays(nodes);
        if (arrays == 0) {
            return pt2;
        } else {
            return pt2.copyWithArrays(arrays);
        }
    }

    private int countArrays(List<Node> nodes) {
        int arrays = 0;
        int i = 1;
        while (i < nodes.size() && nodes.get(i) instanceof Delimiter d && "[".equals(d.getSource())) {
            i += 2;
            arrays++;
        }
        return arrays;
    }
}
