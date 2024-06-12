package org.e2immu.parser.java;

import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.statement.ExpressionAsStatement;
import org.e2immu.cstapi.statement.IfElseStatement;
import org.e2immu.cstapi.statement.TryStatement;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseTryCatch extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              public static void main(String[] args) {
                try {
                   System.out.println(args[0]);
                } catch(Exception e) {
                   System.out.println("exception");
                } finally {
                   System.out.println("bye");
                }
              }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
        MethodInfo main = typeInfo.findUniqueMethod("main", 1);
        if (main.methodBody().statements().get(0) instanceof TryStatement tryStatement) {

        } else fail();
    }
}
