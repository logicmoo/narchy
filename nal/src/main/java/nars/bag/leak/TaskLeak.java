package nars.bag.leak;

import jcog.pri.PLink;
import jcog.pri.PriReference;
import jcog.pri.bag.Bag;
import jcog.pri.bag.Sampler;
import jcog.pri.bag.impl.PLinkArrayBag;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.exe.Causable;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;


/**
 * interface for controlled draining of a bag
 * "leaky bucket" model
 */
public abstract class TaskLeak extends Causable {

    protected final Leak<Task, PriReference<Task>> queue;

    @Nullable Consumer<PriReference<Task>> bagUpdateFn = null;
    /*
            float forgetRate = nar.forgetRate.floatValue();
        bag.forget(forgetRate)
     */

    /**
     * if empty, listens for all
     */
    private final byte[] puncs;

    protected TaskLeak(int capacity, @Nullable NAR n, byte... puncs) {
        this(
                new PLinkArrayBag<>(Param.taskMerge, capacity)
                , n, puncs
        );
    }

    /**
     * an adjusted priority of the task for its insertion to the leak bag
     */
    protected float pri(Task t) {
        return t.priElseZero();
    }

    protected boolean filter(Task next) {
        return true;
    }

    /**
     * returns how much of the input was consumed; 0 means nothing, 1 means 100%
     */
    abstract protected float leak(Task next);

    TaskLeak(Bag<Task, PriReference<Task>> bag, @Nullable NAR n, byte... puncs) {
        super();

        this.puncs = puncs;

        this.queue = new Leak<>(bag);

        if (n!=null)
            n.on(this);
    }


    @Override
    protected void starting(NAR nar) {
        on(nar.onTask(this::accept, puncs));
    }


    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    protected void next(NAR nar, BooleanSupplier kontinue) {

        if (queue == null /* HACK */)
            return;

        Bag<Task, PriReference<Task>> bag = queue.bag;

        if (!bag.commit(bagUpdateFn).isEmpty()) {
            leak(nar, kontinue, bag);
        } else {
//        if (bag.isEmpty())
//            sleepRemainderOfCycle();
        }

    }

    protected void leak(NAR nar, BooleanSupplier kontinue, Bag<Task, PriReference<Task>> bag) {
        Random rng = nar.random();
        bag.sample(rng, (PriReference<Task> v) -> {
            Task t = v.get();
            if (t.isDeleted())
                return Sampler.SampleReaction.Remove;

            float cost = this.leak(t);

            return kontinue.getAsBoolean() ? Sampler.SampleReaction.Remove : Sampler.SampleReaction.RemoveAndStop;
        });
    }


    public final void accept(Task t) {
        if (filter(t)) {
            float p = pri(t);
            if (p == p)
                queue.put(new PLink<>(t, p));
        }
    }

}
