package org.e2immu.parser.java;

import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstimpl.runtime.RuntimeImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.parsers.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        MethodInfo methodInfo = typeInfo.methods().get(0);
        assertEquals("main", methodInfo.name());
        assertEquals("a.b.C.main()", methodInfo.fullyQualifiedName());
    }
}
