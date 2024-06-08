package org.e2immu.parser.java;

import org.e2immu.cstapi.element.Comment;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.statement.Block;
import org.e2immu.cstimpl.element.SingleLineComment;
import org.e2immu.cstimpl.element.SourceImpl;
import org.e2immu.cstimpl.runtime.RuntimeImpl;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.parsers.java.JavaParser;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestParseSubType {

    @Language("java")
    private static final String INPUT = """
            package a.b;
            class C {
              interface I {
                 int i();
              }
              private static class II implements I {
                 int i() {
                   return 3;
                 }
              }
              interface J {
                 boolean j();
              }
              private static class JJ extends II implements I, J {
                 int j() {
                   return true;
                 }
              }
              public static void main(String[] args) {
                System.out.println("hello");
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
        assertEquals("C", typeInfo.simpleName());
        assertEquals("a.b.C", typeInfo.fullyQualifiedName());
        assertEquals(4, typeInfo.subTypes().size());

        TypeInfo subType1 = typeInfo.subTypes().get(0);
        assertTrue(subType1.typeNature().isInterface());
        assertEquals(1, subType1.methods().size());
        MethodInfo i = subType1.methods().get(0);
        assertEquals("i", i.name());
        assertNull(i.methodBody());

        TypeInfo subType2 = typeInfo.subTypes().get(1);
        assertTrue(subType2.typeNature().isClass());
        assertTrue(subType2.isStatic());
        assertFalse(subType2.isPublic());
        assertEquals(1, subType2.methods().size());
        MethodInfo i2 = subType2.methods().get(0);
        assertEquals("i", i.name());
        Block block = i2.methodBody();
        assertEquals(1, block.statements().size());
    }
}
