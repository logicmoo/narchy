package jcog.version;

import jcog.list.FasterList;
import org.jetbrains.annotations.Nullable;

/**
 * Maintains a versioned snapshot history (stack) of a changing value.
 * Managed by a Versioning context
 */
public class Versioned<X> extends FasterList<X> {


    protected final Versioning context;


    public Versioned(Versioning sharedContext, int initialCap) {
        super(initialCap);
        this.context = sharedContext;
    }

    public Versioned(Versioning sharedContext, X[] emptyArray) {
        super(0, emptyArray);
        this.context = sharedContext;
    }

    @Override
    public final boolean equals(Object otherVersioned) {
        return this == otherVersioned;
    }


    /**
     * gets the latest value
     */
    @Nullable
    public X get() {
        int s = this.size;
        return s > 0 ? this.items[s - 1] : null;
    }


    /**
     * sets thens commits
     * returns null if the capacity was hit, or some other error
     */
    @Nullable
    public Versioned<X> set(X nextValue) {
        if (context.add(this)) {
            add(nextValue);
            return this;
        } else {
            return null;
        }
    }


    @Override
    public final String toString() {
        X v = get();
        if (v != null)
            return v.toString();
        return "null";
    }

    public final String toStackString() {
        StringBuilder sb = new StringBuilder("(");
        int s = size();
        for (int i = 0; i < s; i++) {

            sb.append(get(i));

            if (i < s - 1)
                sb.append(", ");
        }
        return sb.append(')').toString();

    }


    public void pop() {


        items[--size] = null;


    }


}
