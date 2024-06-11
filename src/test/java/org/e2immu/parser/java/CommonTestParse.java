package org.e2immu.parser.java;

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
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isPackagePrefix(PackagePrefix packagePrefix) {
            return false;
        }
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
