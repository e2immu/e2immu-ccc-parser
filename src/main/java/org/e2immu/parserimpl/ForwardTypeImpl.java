package org.e2immu.parserimpl;

import org.e2immu.cstapi.info.MethodInfo;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.runtime.Runtime;
import org.e2immu.cstapi.type.ParameterizedType;
import org.e2immu.parserapi.ForwardType;
import org.e2immu.parserapi.TypeContext;

public record ForwardTypeImpl(ParameterizedType type, boolean erasure, TypeParameterMap extra)
        implements ForwardType {

    public ForwardTypeImpl() {
        this(null, false, TypeParameterMap.EMPTY);
    }

    public ForwardTypeImpl(ParameterizedType type) {
        this(type, false, TypeParameterMap.EMPTY);
    }

    public ForwardTypeImpl(ParameterizedType type, boolean erasure) {
        this(type, erasure, TypeParameterMap.EMPTY);
    }

    // we'd rather have java.lang.Boolean, because as soon as type parameters are involved, primitives
    // are boxed
    public static ForwardType expectBoolean(Runtime runtime) {
        return new ForwardTypeImpl(runtime.boxedBooleanTypeInfo().asSimpleParameterizedType(), false);
    }

    public MethodTypeParameterMap computeSAM(InspectionProvider inspectionProvider, TypeInfo primaryType) {
        if (type == null || type.isVoid()) return null;
        MethodTypeParameterMap sam = type.findSingleAbstractMethodOfInterface(inspectionProvider, false);
        if (sam != null) {
            return sam.expand(inspectionProvider, primaryType, type.initialTypeParameterMap(inspectionProvider));
        }
        return null;
    }

    public boolean isVoid(Runtime runtime) {
        if (type == null || type.typeInfo() == null) return false;
        if (type.isVoid()) return true;
        MethodInfo sam = type.typeInfo().singleAbstractMethod();
        if (sam == null) return false;
        MethodTypeParameterMap samMap = type.findSingleAbstractMethodOfInterface(runtime, true);
        return samMap.getConcreteReturnType(runtime).isVoid();
    }

    @Override
    public String toString() {
        return "[FWD: " + (type == null ? "null" : type.detailedString()) + ", erasure: " + erasure + "]";
    }
}
