package org.e2immu.parser.java;

import org.e2immu.cstapi.expression.ConstructorCall;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.statement.ExpressionAsStatement;
import org.e2immu.cstapi.statement.LocalVariableCreation;
import org.e2immu.cstapi.type.ParameterizedType;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseConstructorCall extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public class C {
              int k;
              public static void main(String[] args) {
                C c = new C();
                System.out.println(c);
              }
              public C() {
                k = 1;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo constructor = typeInfo.findConstructor(0);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            assertSame(constructor, cc.constructor());
            assertSame(typeInfo, cc.parameterizedType().typeInfo());
        } else fail();
    }

    @Language("java")
    private static final String INPUT2 = """
            package a.b;
            public class C<K> {
              K k;
              public static void main(String[] args) {
                C<String> c = new C<>();
                System.out.println(c);
              }
            }
            """;

    @Test
    public void test2() {
        TypeInfo typeInfo = parse(INPUT2);
        MethodInfo constructor = typeInfo.findConstructor(0);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof LocalVariableCreation lvc
            && lvc.localVariable().assignmentExpression() instanceof ConstructorCall cc) {
            ParameterizedType pt = lvc.localVariable().parameterizedType();
            assertSame(typeInfo, pt.typeInfo());
            assertEquals(1, pt.parameters().size());
            assertSame(runtime.stringTypeInfo(), pt.parameters().get(0).typeInfo());

            assertSame(constructor, cc.constructor());
            assertSame(typeInfo, cc.parameterizedType().typeInfo());
            assertEquals("new C<>()", cc.toString());
            assertEquals("C<String> c=new C<>()", lvc.toString());
        } else fail();
    }
}
