package jcog.pri.bag.util;

import com.google.common.collect.Iterables;
import jcog.data.NumberX;
import jcog.math.FloatRange;
import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import jcog.pri.bag.Bag;
import jcog.pri.bag.impl.PLinkArrayBag;
import jcog.pri.op.PriMerge;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * a bag which wraps another bag, accepts its value as input but at a throttled rate
 * resulting in containing effectively the integrated / moving average values of the input bag
 * TODO make a PLink version of ArrayBag since quality is not used here
 */
public class Bagregate<X extends Prioritized> implements Iterable<PriReference<X>> {

    public final Bag<X, PriReference<X>> bag;
    private final Iterable<X> src;
    private final NumberX scale;
//    private final AtomicBoolean busy = new AtomicBoolean();


    public Bagregate(Stream<X> src, int capacity, float scale) {
        this(src::iterator, capacity, scale);
    }

    public Bagregate(Iterable<X> src, int capacity, float scale) {
        this.bag = new PLinkArrayBag(PriMerge.avg /*PriMerge.replace*/, capacity) {
            @Override
            public void onRemove(Object value) {
                Bagregate.this.onRemove((PriReference<X>) value);
            }
        };
        this.src = src;
        this.scale = new FloatRange(scale, 0f, 1f);
    }

    protected void onRemove(PriReference<X> value) {

    }

    public boolean commit() {
        if (src==null /*|| !busy.compareAndSet(false, true)*/)
            return false;

//        try {


                bag.commit();

                float scale = this.scale.floatValue();

                src.forEach(x -> {
                    if (include(x)) {
                        float pri = x.priElseZero();
                        bag.putAsync(new PLink<>(x, pri * scale));
                    }
                });



//        } finally {
//            busy.setRelease(false);
//        }
        return true;
    }

    /**
     * can be overridden to filter entry
     */
    private boolean include(X x) {
        return true;
    }

    @Override
    public final Iterator<PriReference<X>> iterator() {
            return bag.iterator();
    }

    @Override
    public final void forEach(Consumer<? super PriReference<X>> action) {
            bag.forEach(action);
    }

    public void clear() {
        bag.clear();
    }


    /** compose */
    public <Y> Iterable<Y> iterable(Function<X, Y> f) {
        return Iterables.transform(Iterables.filter(bag, Objects::nonNull) /* HACK */, (b)->f.apply(b.get()));
    }


    




}
