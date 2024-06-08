package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.cstapi.statement.ReturnStatement;
import org.e2immu.cstimpl.element.SingleLineComment;
import org.e2immu.cstimpl.element.SourceImpl;
import org.e2immu.cstimpl.runtime.RuntimeImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.parsers.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseBinaryOperator {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            // some comment
            class C {
              int times(int i, int j) {
                /* return comment
                   the product of i and j */
                return i*j;
              }
            }
            """;

    @Test
    public void test() {
        JavaParser parser = new JavaParser(INPUT);
        parser.setParserTolerant(false);
        Runtime runtime = new RuntimeImpl();
        List<TypeInfo> types = new ParseCompilationUnit(runtime).parse(parser.CompilationUnit());
        assertEquals(1, types.size());
        TypeInfo typeInfo = types.get(0);

        MethodInfo methodInfo = typeInfo.methods().get(0);
        assertEquals("times", methodInfo.name());
        assertEquals("a.b.C.times(int,int)", methodInfo.fullyQualifiedName());

        Block block = methodInfo.methodBody();
        ReturnStatement returnStatement = (ReturnStatement) block.statements().get(0);
        assertEquals("i*j", returnStatement.expression().toString());
    }
}
