package org.e2immu.parser.java;

import org.e2immu.cstapi.expression.VariableExpression;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.statement.ForEachStatement;
import org.e2immu.cstapi.statement.WhileStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseWhile extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            // some comment
            class C {
              public static void main(String[] args) {
                int i=0;
                while(i<args.length) {
                  System.out.println(i+"="+args[i]);
                  i++;
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof WhileStatement w) {

        } else fail();
    }
}
