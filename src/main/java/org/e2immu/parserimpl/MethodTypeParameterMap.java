package org.e2immu.parserimpl;

import org.e2immu.annotation.NotNull;
import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.ParameterInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.translate.TranslationMap;
import org.e2immu.cstapi.type.NamedType;
import org.e2immu.cstapi.type.ParameterizedType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MethodTypeParameterMap {

    public final MethodInfo methodInfo;
    @NotNull
    public final Map<NamedType, ParameterizedType> concreteTypes;

    public MethodTypeParameterMap(MethodInfo methodInfo, @NotNull Map<NamedType, ParameterizedType> concreteTypes) {
        this.methodInfo = methodInfo; // can be null, for SAMs
        this.concreteTypes = Map.copyOf(concreteTypes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        return o instanceof MethodTypeParameterMap m && methodInfo.equals(m.methodInfo);
    }

    @Override
    public int hashCode() {
        return methodInfo.hashCode();
    }

    public boolean isSingleAbstractMethod() {
        return methodInfo != null;
    }

    public ParameterizedType getConcreteReturnType(Runtime runtime) {
        if (!isSingleAbstractMethod())
            throw new UnsupportedOperationException("Can only be called on a single abstract method");
        ParameterizedType returnType = methodInfo.returnType();
        return returnType.applyTranslation(runtime, concreteTypes);
    }

    public ParameterizedType getConcreteTypeOfParameter(Runtime runtime, int i) {
        if (!isSingleAbstractMethod())
            throw new UnsupportedOperationException("Can only be called on a single abstract method");
        int n = methodInfo.parameters().size();
        int index;
        if (i >= n) {
            // varargs
            index = n - 1;
        } else {
            index = i;
        }

        ParameterizedType parameterizedType = methodInfo.parameters().get(index).parameterizedType();
        return parameterizedType.applyTranslation(runtime, concreteTypes);
    }

    public MethodTypeParameterMap expand(InspectionProvider inspectionProvider,
                                         TypeInfo primaryType,
                                         Map<NamedType, ParameterizedType> mapExpansion) {
        Map<NamedType, ParameterizedType> join = new HashMap<>(concreteTypes);
        mapExpansion.forEach((k, v) -> join.merge(k, v, (v1, v2) -> v1.mostSpecific(inspectionProvider, primaryType, v2)));
        return new MethodTypeParameterMap(methodInfo, Map.copyOf(join));
    }


    @Override
    public String toString() {
        return (isSingleAbstractMethod()
                ? ("method " + methodInfo.fullyQualifiedName())
                : "No method") + ", map " + concreteTypes;
    }

    public ParameterizedType inferFunctionalType(Runtime runtime,
                                                 List<ParameterizedType> types,
                                                 ParameterizedType inferredReturnType) {
        Objects.requireNonNull(inferredReturnType);
        Objects.requireNonNull(types);
        if (!isSingleAbstractMethod())
            throw new UnsupportedOperationException("Can only be called on a single abstract method");

        List<ParameterizedType> parameters = typeParametersComputed(runtime, methodInfo, types, inferredReturnType);
        return runtime.newParameterizedType(methodInfo.typeInfo(), parameters);
    }

    /**
     * Example: methodInfo = R apply(T t); typeInfo = Function&lt;T, R&gt;; types: one value: the concrete type for
     * parameter #0 in apply; inferredReturnType: the concrete type for R, the return type.
     *
     * @param runtime            to access inspection
     * @param methodInfo         the SAM (e.g. accept, test, apply)
     * @param types              as provided by ParseMethodReference, or ParseLambdaExpr. They represent the concrete
     *                           types of the SAM
     * @param inferredReturnType the return type of the real method
     * @return a list of type parameters for the functional type
     */


    private static List<ParameterizedType> typeParametersComputed(
            Runtime runtime,
            MethodInfo methodInfo,
            List<ParameterizedType> types,
            ParameterizedType inferredReturnType) {
        TypeInfo typeInfo = methodInfo.typeInfo();
        if (typeInfo.typeParameters().isEmpty()) return List.of();
        // Function<T, R> -> loop over T and R, and see where they appear in the apply method.
        // If they appear as a parameter, then take the type from "types" which agrees with that parameter
        // If it appears as the return type, then return "inferredReturnType"
        return typeInfo.typeParameters().stream()
                .map(typeParameter -> {
                    int cnt = 0;
                    for (ParameterInfo parameterInfo : methodInfo.parameters()) {
                        if (parameterInfo.parameterizedType().typeParameter() == typeParameter) {
                            return types.get(cnt); // this is one we know!
                        }
                        cnt++;
                    }
                    if (methodInfo.returnType().typeParameter() == typeParameter)
                        return inferredReturnType;
                    return runtime.newParameterizedType(typeParameter, 0, null);
                })
                .map(pt -> pt.ensureBoxed(runtime))
                .collect(Collectors.toList());
    }


    public boolean isAssignableFrom(MethodTypeParameterMap other) {
        if (!isSingleAbstractMethod() || !other.isSingleAbstractMethod()) throw new UnsupportedOperationException();
        if (methodInfo.equals(other.methodInfo)) return true;
        if (methodInfo.parameters().size() != other.methodInfo.parameters().size())
            return false;
        /*
        int i = 0;
        for (ParameterInfo pi : methodInspection.getParameters()) {
            ParameterInfo piOther = other.methodInspection.getParameters().get(i);
            i++;
        }
        // TODO
         */
        return methodInfo.returnType().isVoidOrJavaLangVoid() ==
               other.methodInfo.returnType().isVoidOrJavaLangVoid();
    }

    // used in TypeInfo.convertMethodReferenceIntoLambda
    public MethodInfo.Builder buildCopy(Identifier identifier,
                                        Runtime runtime,
                                        TypeInfo typeInfo) {
        String methodName = methodInfo.name();
        MethodInfo copy = runtime.newMethod(identifier, typeInfo, methodName);
        MethodInfo.Builder copyBuilder = copy.builder();
        copyBuilder.addMethodModifier(runtime.methodModifierPUBLIC());

        for (ParameterInfo p : methodInfo.parameters()) {
            ParameterInspection.Builder newParameterBuilder = copy.newParameterInspectionBuilder(
                    p.identifier,
                    getConcreteTypeOfParameter(runtime.getPrimitives(), p.index), p.name, p.index);
            if (p.parameterInspection.get().isVarArgs()) {
                newParameterBuilder.setVarArgs(true);
            }
            copy.addParameter(newParameterBuilder);
        }
        copy.setReturnType(getConcreteReturnType(runtime.getPrimitives()));
        copy.readyToComputeFQN(runtime);
        return copy;
    }

    public MethodTypeParameterMap translate(TranslationMap translationMap) {
        return new MethodTypeParameterMap(methodInfo, concreteTypes.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> translationMap.translateType(e.getValue()))));
    }

    public ParameterizedType parameterizedType(int pos) {
        List<ParameterInfo> parameters = methodInfo.parameters();
        if (pos < parameters.size()) return parameters.get(pos).parameterizedType();
        ParameterInfo lastOne = parameters.get(parameters.size() - 1);
        if (!lastOne.isVarArgs()) throw new UnsupportedOperationException();
        return lastOne.parameterizedType().copyWithOneFewerArrays();
    }

    /*
    CT = concreteTypes

    CT:  T in Function -> AL<LL<S>>
    F2C: T in Function -> Coll<E>
    result: E in Coll -> LL<S>

    CT:  R in Function -> Stream<? R in flatMap>
    F2C: R in Function -> Stream<E in Coll>
    result: E in Coll = R in flatMap (is of little value here)
     */
    public Map<NamedType, ParameterizedType> formalOfSamToConcreteTypes(MethodInspection actualMethodInspection, InspectionProvider inspectionProvider) {
        Map<NamedType, ParameterizedType> result = new HashMap<>(concreteTypes);

        TypeInspection functionalTypeInspection = inspectionProvider.getTypeInspection(this.methodInfo.getMethodInfo().typeInfo);
        MethodInspection sam = functionalTypeInspection.getSingleAbstractMethod();
        // match types of actual method inspection to type parameters of sam
        if (sam.getReturnType().isTypeParameter()) {
            NamedType f2cFrom = sam.getReturnType().typeParameter;
            ParameterizedType f2cTo = actualMethodInspection.getReturnType();
            ParameterizedType ctTo = concreteTypes.get(f2cFrom);
            match(inspectionProvider, f2cFrom, f2cTo, ctTo, result);
        }
        if (!actualMethodInspection.getMethodInfo().isStatic() && !functionalTypeInspection.typeParameters().isEmpty()) {
            NamedType f2cFrom = functionalTypeInspection.typeParameters().get(0);
            ParameterizedType f2cTo = actualMethodInspection.getMethodInfo().typeInfo.asParameterizedType(inspectionProvider);
            ParameterizedType ctTo = concreteTypes.get(f2cFrom);
            match(inspectionProvider, f2cFrom, f2cTo, ctTo, result);
        }
        // TODO for-loop: make an equivalent with more type parameters MethodReference_2
        return result;
    }

    /*
    f2cFrom = T in function
    fc2To = Coll<E>
    ctTo = ArrayList<LinkedList<String>>

     */
    private void match(Runtime runtime, NamedType f2cFrom, ParameterizedType f2cTo,
                       ParameterizedType ctTo, Map<NamedType, ParameterizedType> result) {
        if (f2cTo.isAssignableFrom(runtime, ctTo)) {
            ParameterizedType concreteSuperType = ctTo.concreteSuperType(f2cTo);
            int i = 0;
            for (ParameterizedType pt : f2cTo.parameters()) {
                if (pt.isTypeParameter()) {
                    result.put(pt.typeParameter(), concreteSuperType.parameters().get(i));
                }
                i++;
            }
        }
    }
}
