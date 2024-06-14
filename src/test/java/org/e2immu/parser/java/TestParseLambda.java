package org.e2immu.parser.java;

import org.e2immu.cstapi.expression.Lambda;
import org.e2immu.cstapi.expression.MethodReference;
import org.e2immu.cstapi.expression.TypeExpression;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.statement.ReturnStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseLambda extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            import java.util.function.Function;
            class C {
              String s;
              Function<C, String> mapper() {
                 return t -> t+s;
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo mapper = typeInfo.findUniqueMethod("mapper", 0);
        if (mapper.methodBody().statements().get(0) instanceof ReturnStatement rs
            && rs.expression() instanceof Lambda lambda) {
            assertEquals("I::map", lambda.toString());

        } else fail();
    }
}
