package org.e2immu.parser.java;

import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstimpl.runtime.RuntimeImpl;
import org.e2immu.parserapi.Context;
import org.e2immu.parserapi.PackagePrefix;
import org.e2immu.parserimpl.ContextImpl;
import org.e2immu.resourceapi.TypeMap;
import org.parsers.java.JavaParser;

import java.util.List;


public class CommonTestParse {

    static class TypeMapBuilder implements TypeMap.Builder {

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
        Runtime runtime = new RuntimeImpl();
        TypeMap.Builder typeMapBuilder = new TypeMapBuilder();
        Context rootContext = new ContextImpl(runtime, null, null, null,
                null, null, null, null);
        List<TypeInfo> types = new ParseCompilationUnit(typeMapBuilder, rootContext).parse(parser.CompilationUnit());
        return types.get(0);
    }
}
