package org.e2immu.parserapi;

import org.e2immu.cstapi.info.Info;
import org.e2immu.cstapi.info.TypeInfo;
import org.e2immu.cstapi.statement.ExplicitConstructorInvocation;
import org.parsers.java.Node;

public interface Resolver {
    /*
    we must add the ECI here, because CongoCC does not see the ECI as a separate statement
     */
    void add(Info.Builder<?> infoBuilder, ForwardType forwardType,
             ExplicitConstructorInvocation eci,
             Node expression, Context context);

    void add(TypeInfo.Builder typeInfoBuilder);

    void resolve();
}
