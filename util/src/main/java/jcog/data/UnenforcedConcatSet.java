package jcog.data;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

/**
 * TODO make a caching subclass which
 * generates an arraylist copy during the first iteration for fast
 * subsequent iterations. this copy can be linked via SoftReference<>
 */
public class UnenforcedConcatSet<X> extends AbstractSet<X> {

    final Set<X> a, b;

    UnenforcedConcatSet(@NotNull Set<X> a, @NotNull Set<X> b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public boolean add(X x) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<X> iterator() {
        return Iterators.concat(a.iterator(), b.iterator());
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return Joiner.on(',').join(iterator());
    }


    /** if a or b are null, they are considered empty sets */
    @NotNull public static <X> Set<X> the(@Nullable Set<X> a, @Nullable Set<X> b) {
        Set<X> nextFree;
        boolean aEmpty = a == null || a.isEmpty();
        boolean bEmpty = b == null || b.isEmpty();
        if (bEmpty && aEmpty) {
            nextFree = Set.of();
        } else if (bEmpty) {
            nextFree = a;
        } else if (aEmpty) {
            nextFree = b;
        } else {
            nextFree = new UnenforcedConcatSet<>(a, b);
        }
        return nextFree;
    }

}