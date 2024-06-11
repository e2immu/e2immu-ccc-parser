package org.e2immu.parser.java;

import org.e2immu.cstapi.element.CompilationUnit;
import org.e2immu.cstapi.info.FieldInfo;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstimpl.runtime.RuntimeImpl;
import org.e2immu.parserapi.Context;
import org.e2immu.parserapi.PackagePrefix;
import org.e2immu.parserimpl.ContextImpl;
import org.e2immu.parserimpl.ResolverImpl;
import org.e2immu.resourceapi.TypeMap;
import org.parsers.java.JavaParser;

import java.util.List;


public class CommonTestParse {

    protected final Runtime runtime = new RuntimeImpl();
    protected final TypeInfo math;
    protected final TypeInfo system;
    protected final TypeInfo printStream;

    class TypeMapBuilder implements TypeMap.Builder {

        @Override
        public TypeInfo getOrCreate(String fqn, boolean complain) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void ensureInspection(TypeInfo typeInfo) {

        }

        @Override
        public TypeInfo get(Class<?> clazz) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TypeInfo get(String fullyQualifiedName) {
            if ("java.lang.String".equals(fullyQualifiedName)) return runtime.stringTypeInfo();
            if ("java.lang.System".equals(fullyQualifiedName)) {
                return system;
            }
            if ("java.lang.Math".equals(fullyQualifiedName)) {
                return math;
            }
            if ("java.io.PrintStream".equals(fullyQualifiedName)) {
                return printStream;
            }
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPackagePrefix(PackagePrefix packagePrefix) {
            return false;
        }
    }

    protected CommonTestParse() {
        CompilationUnit javaLang = runtime.newCompilationUnitBuilder().setPackageName("java.lang").build();
        CompilationUnit javaIo = runtime.newCompilationUnitBuilder().setPackageName("java.io").build();

        math = runtime.newTypeInfo(javaLang, "Math");
        printStream = runtime.newTypeInfo(javaIo, "PrintStream");
        system = runtime.newTypeInfo(javaLang, "System");

        MethodInfo pow = runtime.newMethod(math, "pow", runtime.newMethodTypeStaticMethod());
        pow.builder().addParameter("base", runtime.doubleParameterizedType());
        pow.builder().addParameter("exponent", runtime.doubleParameterizedType());
        pow.builder().setReturnType(runtime.doubleParameterizedType());
        pow.builder().commit();
        math.builder().addMethod(pow);
        math.builder().commit();

        MethodInfo println = runtime.newMethod(printStream, "println", runtime.newMethodTypeMethod());
        println.builder().addParameter("string", runtime.stringParameterizedType());
        println.builder().commit();
        printStream.builder().addMethod(println);
        printStream.builder().commit();

        FieldInfo out = runtime.newFieldInfo("out", true, printStream.asSimpleParameterizedType(), system);
        system.builder().addField(out);
        system.builder().commit();
    }

    protected TypeInfo parse(String input) {
        JavaParser parser = new JavaParser(input);
        parser.setParserTolerant(false);
        TypeMap.Builder typeMapBuilder = new TypeMapBuilder();
        ResolverImpl resolver = new ResolverImpl(runtime);
        Context rootContext = new ContextImpl(runtime, resolver, null, null, null,
                null, null, null, null);
        List<TypeInfo> types = new ParseCompilationUnit(typeMapBuilder, rootContext).parse(parser.CompilationUnit());
        resolver.resolve();
        return types.get(0);
    }
}
