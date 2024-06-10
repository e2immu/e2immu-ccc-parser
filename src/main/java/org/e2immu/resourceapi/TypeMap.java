package org.e2immu.resourceapi;

import org.e2immu.annotation.Modified;
import org.e2immu.annotation.NotNull;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.parserapi.PackagePrefix;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

/*
Connects to the Resources maps.
 */
public interface TypeMap {

    TypeInfo get(Class<?> clazz);

    TypeInfo get(String fullyQualifiedName);

    boolean isPackagePrefix(PackagePrefix packagePrefix);

    void visit(String[] prefix, BiConsumer<String[], List<TypeInfo>> consumer);

   // E2ImmuAnnotationExpressions getE2ImmuAnnotationExpressions();

    void ensureInspection(TypeInfo typeInfo);

    record InspectionAndState(TypeInspection typeInspection, InspectionState state) {
    }

    interface Builder extends TypeMap {
        // generic, could be from source, could be from byte code; used in direct type access in source code
        TypeInfo getOrCreate(String fqn, boolean complain);

        /*
        main entry point to start building types
         */
        @Modified
        @NotNull
        TypeInfo getOrCreate(String packageName, String name, Identifier identifier, InspectionState triggerJavaParser);

        /*
        convenience method for getOrCreate, first calling the classPath to obtain the identifier from the source
         */
        @Modified
        @NotNull
        TypeInfo getOrCreateByteCode(String packageName, String simpleName);

        /*
        NOTE: this method should not be used by the ASM visitors or bytecode inspection implementation!!!
         */
        @Modified
        @NotNull
        TypeInspection.Builder getOrCreateFromClassPathEnsureEnclosing(Source source,
                                                                       InspectionState startingBytecode);

        TypeInfo addToTrie(TypeInfo typeInfo);

        /*
                lowest level: a call on get() will return null, called by getOrCreate
                 */
        @Modified
        @NotNull
        TypeInspection.Builder add(TypeInfo typeInfo, InspectionState triggerJavaParser);

        /*
        for use inside byte code inspection
         */
        InspectionAndState typeInspectionSituation(String fqName);

        MethodInspection getMethodInspectionDoNotTrigger(TypeInfo typeInfo, String distinguishingName);

        @Modified
        void setByteCodeInspector(ByteCodeInspector byteCodeInspector);

        @Modified
        void setInspectWithJavaParser(InspectWithJavaParser onDemandSourceInspection);

        @Modified
        void makeParametersImmutable();

        // can be called only one, freezes
        @Modified
        TypeMap build();

        @Modified
        void registerFieldInspection(FieldInfo fieldInfo, FieldInspection.Builder fieldBuilder);

        @Modified
        void registerMethodInspection(MethodInspection.Builder builder);

        @NotNull
        InspectionState getInspectionState(TypeInfo inMap);

        @Modified
        @NotNull
        TypeInfo syntheticFunction(int parameters, boolean isVoid);

        @NotNull
        Stream<TypeInfo> streamTypesStartingByteCode();

        /*
        Convenience method for local class declarations and anonymous types; calls `add` to add them
         */
        @NotNull
        TypeInspector newTypeInspector(TypeInfo typeInfo, boolean b, boolean b1);

        TypeInspection getTypeInspectionToStartResolving(TypeInfo typeInfo);

        // from byteCodeInspection into my typeMap
        TypeInspection.Builder copyIntoTypeMap(TypeInfo start, List<TypeData> localTypeData);

        void addToByteCodeQueue(String fqn);
    }
}
