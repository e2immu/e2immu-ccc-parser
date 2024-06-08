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

public class TestParseAnnotation {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            public @interface Annot {
              String value() default "";
              int n();
              String k() default "k";
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
        assertEquals("Annot", typeInfo.simpleName());
        assertEquals("a.b.Annot", typeInfo.fullyQualifiedName());
        assertTrue(typeInfo.typeNature().isAnnotation());

        assertEquals(1, typeInfo.methods().size());
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
