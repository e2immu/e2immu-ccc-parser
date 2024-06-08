package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstimpl.element.SingleLineComment;
import org.e2immu.cstimpl.element.SourceImpl;
import org.e2immu.cstimpl.runtime.RuntimeImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.parsers.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestParse {

    @Language("java")
    String input = """
            package a.b;
            // some comment
            class C {
              public static void main(String[] args) {
                System.out.println("hello");
              }
            }
            """;

    @Test
    public void test() {
        JavaParser parser = new JavaParser(input);
        parser.setParserTolerant(false);
        Runtime runtime = new RuntimeImpl();
        List<TypeInfo> types = new ParseCompilationUnit(runtime).parse(parser.CompilationUnit());
        assertEquals(1, types.size());
        TypeInfo typeInfo = types.get(0);
        assertEquals("C", typeInfo.simpleName());
        assertEquals("a.b.C", typeInfo.fullyQualifiedName());
        assertEquals(1, typeInfo.methods().size());
        assertNotNull(typeInfo.comments());
        assertEquals(1, typeInfo.comments().size());
        Comment comment = typeInfo.comments().get(0);
        if (comment instanceof SingleLineComment slc) {
            assertEquals("// some comment\n", slc.print(null).toString());
        } else fail();
        if (typeInfo.source() instanceof SourceImpl source) {
            assertSame(typeInfo, source.info());
            assertEquals(3, source.beginLine());
            assertEquals(7, source.endLine());
        }

        MethodInfo methodInfo = typeInfo.methods().get(0);
        assertEquals("main", methodInfo.name());
        assertEquals("a.b.C.main()", methodInfo.fullyQualifiedName());
        if (methodInfo.source() instanceof SourceImpl source) {
            assertSame(methodInfo, source.info());
            assertEquals(4, source.beginLine());
            assertEquals(6, source.endLine());
        }
    }
}
