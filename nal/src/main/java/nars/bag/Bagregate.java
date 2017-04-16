package nars.bag;

import jcog.bag.PLink;
import jcog.data.FloatParam;
import nars.bag.impl.ArrayBag;
import nars.budget.BudgetMerge;
import nars.budget.RawBLink;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 *  a bag which wraps another bag, accepts its value as input but at a throttled rate
 *  resulting in containing effectively the integrated / moving average values of the input bag
 *  TODO make a PLink version of ArrayBag since quality is not used here
 */
public class Bagregate<X> extends ArrayBag<X> {

    private final Iterable<X> src;
    private final MutableFloat scale;
    final AtomicBoolean busy = new AtomicBoolean();

    public Bagregate(@NotNull Iterable<X> src, int capacity, float scale) {
        super(capacity, BudgetMerge.maxBlend, new ConcurrentHashMap<>(capacity));

        this.src = src;
        this.scale = new FloatParam(scale);

        update();
    }

    protected void update() {
        if (!busy.compareAndSet(false, true))
            return;

        try {

            commit();

            float scale = this.scale.floatValue();

            src.forEach(x -> {

                float pri;
                if (x instanceof PLink) { //HACK
                    PLink p = (PLink) x;
                    x = (X) p.get();
                    pri = p.pri();
                } else {
                    pri = 1f;
                }

                if (x != null && include(x)) {
                    put(new RawBLink(x, pri), scale, null);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

        busy.set(false);
    }

    /** can be overridden to filter entry */
    protected boolean include(X x) {
        return true;
    }

    @Override
    public void forEach(Consumer<? super PLink<X>> action) {
        forEach(size(), action);
    }

    @Override
    public void forEach(int max, Consumer<? super PLink<X>> action) {
        update();
        super.forEach(max, action);
    }

}
