package org.e2immu.parser.java;

import org.e2immu.cstapi.info.TypeInfo;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;

public class TestParseSplitExpression0 extends CommonTestParse {

    @Language("java")
    private static final String INPUT = """
            public abstract class SplitExpression_0 {

                private final int base;
                private int j;

                public SplitExpression_0(int base) {
                    this.base = base;
                }

                protected abstract int method(int i1, int i2, int i3, int i4);

                private int compute(int i) {
                    return (int) Math.pow(base, i);
                }

                public int same1(int k) {
                    return method(compute(3), compute(k), j = k + 2, compute(j));
                }
                
                public int same2(int k) {
                    int c3 = compute(3);
                    int ck = compute(k);
                    int cj = j = k + 2;
                    int c2 = compute(j);
                    return method(c3, ck, cj, c2);
                }

                public int same3(int k) {
                    int ck = compute(k);
                    int a = j = k + 2;
                    int c2 = compute(j);
                    int c3 = compute(3);
                    return method(c3, ck, a, c2);
                }

                public int same5(int k) {
                    int ck = compute(k);
                    int c3 = compute(3);
                    int a = j = k + 2;
                    int cj = compute(j);
                    // note: the + in +a gets evaluated away
                    return method(c3, ck, +a, cj);
                }

                public int same6(int k) {
                    // note: 2+1 gets evaluated to 3
                    int c3 = compute(2+1);
                    return method(c3, compute(k), (j = k + 2), compute(j));
                }
            }
            """;

    @Test
    public void test() {
        TypeInfo typeInfo = parse(INPUT);
    }
}
