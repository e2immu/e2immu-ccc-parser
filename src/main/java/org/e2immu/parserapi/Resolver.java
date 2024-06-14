package org.e2immu.parserapi;

import org.e2immu.cstapi.info.Info;
import org.e2immu.cstapi.info.TypeInfo;
import org.parsers.java.Node;

public interface Resolver {

    void add(Info.Builder<?> infoBuilder, ForwardType forwardType, Node expression, Context context);

    void add(TypeInfo.Builder typeInfoBuilder);

    void resolve();
}
