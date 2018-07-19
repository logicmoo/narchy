package jcog.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/** atomic switching double buffer */
public class Flip<X> extends AtomicInteger {

    private final X a;
    private final X b;

    public Flip(Supplier<X> builder) {
        super(0);
        this.a = builder.get();
        this.b = builder.get();
    }

    public X write() { return (getOpaque() & 1) == 0 ? a : b;   }

    public X read() {
        return read(getOpaque());
    }
    public X read(int v) {
        return (v & 1) == 0 ? b : a;
    }

    public X commitAndGet() {
        return read(commit());
    }

    public int commit() {
        return incrementAndGet();
    }

}
