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

    private TypeInfo predefined(String fullyQualifiedName, boolean complain) {
        return switch (fullyQualifiedName) {
            case "java.lang.Class" -> clazz;
            case "java.lang.String" -> runtime.stringTypeInfo();
            case "java.lang.Integer" -> runtime.integerTypeInfo();
            case "java.lang.System" -> system;
            case "java.lang.Math" -> math;
            case "java.lang.Exception" -> exception;
            case "java.io.PrintStream" -> printStream;
            case "java.util.function.Function" -> function;
            default -> {
                if (complain) throw new UnsupportedOperationException("Type " + fullyQualifiedName);
                yield null;
            }
        };
    }

    protected final Runtime runtime = new RuntimeImpl() {
        @Override
        public TypeInfo getFullyQualified(String name, boolean complain) {
            return predefined(name, complain);
        }

        @Override
        public TypeInfo syntheticFunctionalType(int inputParameters, boolean hasReturnValue) {
            if (inputParameters == 1 && hasReturnValue) return function;
            throw new UnsupportedOperationException();
        }
    };

    protected final TypeInfo clazz;
    protected final TypeInfo math;
    protected final TypeInfo system;
    protected final TypeInfo exception;
    protected final TypeInfo printStream;
    protected final TypeInfo function;


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
            return runtime.getFullyQualified(fullyQualifiedName, true);
        }

        @Override
        public boolean isPackagePrefix(PackagePrefix packagePrefix) {
            return false;
        }
    }

    protected CommonTestParse() {
        CompilationUnit javaLang = runtime.newCompilationUnitBuilder().setPackageName("java.lang").build();
        CompilationUnit javaIo = runtime.newCompilationUnitBuilder().setPackageName("java.io").build();
        CompilationUnit javaUtilFunction = runtime.newCompilationUnitBuilder().setPackageName("java.util.function").build();

        clazz = runtime.newTypeInfo(javaLang, "Class");
        math = runtime.newTypeInfo(javaLang, "Math");
        printStream = runtime.newTypeInfo(javaIo, "PrintStream");
        system = runtime.newTypeInfo(javaLang, "System");
        exception = runtime.newTypeInfo(javaLang, "Exception");
        function = runtime.newTypeInfo(javaUtilFunction, "Function");

        clazz.builder().addTypeParameter(runtime.newTypeParameter(0, "C", clazz));

        MethodInfo pow = runtime.newMethod(math, "pow", runtime.methodTypeStaticMethod());
        pow.builder().addParameter("base", runtime.doubleParameterizedType());
        pow.builder().addParameter("exponent", runtime.doubleParameterizedType());
        pow.builder().setReturnType(runtime.doubleParameterizedType());
        pow.builder().commit();
        math.builder().addMethod(pow);
        math.builder().commit();

        MethodInfo println = runtime.newMethod(printStream, "println", runtime.methodTypeMethod());
        println.builder().addParameter("string", runtime.stringParameterizedType());
        println.builder().setReturnType(runtime.voidParameterizedType());
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
