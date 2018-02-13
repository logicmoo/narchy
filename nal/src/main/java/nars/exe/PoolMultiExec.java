package nars.exe;

import jcog.Util;
import jcog.decide.Roulette;
import jcog.exe.BusyPool;
import jcog.math.MutableInteger;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.ITask;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

/**
 * uses a common forkjoin pool for execution
 */
public class PoolMultiExec extends AbstractExec {

    static final int IDLE_PERIOD_MS = 50;

    private final int qSize;
    BusyPool pool;

    private final Revaluator revaluator;

    /**
     * TODO make this adjust in realtime
     */
    private final MutableInteger threads = new MutableInteger();
    final List<Thread> activeThreads = $.newArrayList();
    LongSet activeThreadIds = new LongHashSet();
    LongPredicate isActiveThreadId = (x) -> false;


    private Focus focus;
    private Consumer exe;


    public PoolMultiExec(Revaluator revaluator, int conceptCapacity, int qSize) {
        this(revaluator, Runtime.getRuntime().availableProcessors() - 1, conceptCapacity, qSize);
    }

    public PoolMultiExec(Revaluator r, int threads, int conceptCapacity, int qSize) {
        super(conceptCapacity);
        this.revaluator = r;
        this.threads.set(threads);
        this.qSize = qSize;
        this.exe = this::executeInline;
    }

    @Override
    public void execute(Consumer<NAR> r) {
        execute((Object) r);
    }

    @Override
    public void execute(Runnable r) {
        execute((Object) r);
    }


    @Override
    public void execute(Object t) {

        if (t instanceof Task || isWorker(Thread.currentThread())) {
            executeInline(t);
        } else {
            exe.accept(t);
        }

    }


    private final Consumer immediate = this::executeInline;

    private final Consumer deferred = x -> {
        if (x instanceof Task)
            executeInline(x);
        else
            exe.accept(x);
    };

    /**
     * the input procedure according to the current thread
     */
    private Consumer add() {
        return isWorker(Thread.currentThread()) ? immediate : deferred;
    }

    @Override
    public void execute(Stream<? extends ITask> input) {
        input.forEach(add());
    }

    @Override
    public void execute(Iterator<? extends ITask> input) {
        input.forEachRemaining(add());
    }

    private boolean isWorker(Thread t) {
        return isActiveThreadId.test(t.getId());
    }


    /**
     * to be called in initWorkers() impl for each thread constructed
     */
    private void register(Thread t) {
        activeThreads.add(t);
        activeThreadIds = LongSets.mutable.ofAll(activeThreadIds).with(t.getId()).toImmutable();
        long max = activeThreadIds.max();
        long min = activeThreadIds.min();
        if (max - min == activeThreadIds.size() - 1) {
            //contiguous id's, use fast id tester
            isActiveThreadId = (x) -> x >= min && x <= max;
        } else {
            isActiveThreadId = activeThreadIds::contains;
        }
    }

    @Override
    protected void clear() {
        synchronized (this) {
            super.clear();
            activeThreads.forEach(Thread::interrupt);
            activeThreads.clear();
            activeThreadIds = new LongHashSet();
            isActiveThreadId = (x) -> false;
        }
    }


    @Override
    public void start(NAR nar) {
        synchronized (this) {
            this.focus = new Focus(nar, revaluator);

            super.start(nar);

            this.pool = new BusyPool(threads.intValue(),
                    Util.blockingQueue(qSize)
                    //new ArrayBlockingQueue(qSize)
            ) {
                @Override
                protected WorkLoop newWorkLoop(Queue<Runnable> q) {
                    return new MyWorkLoop(q, nar);
                }
            };
            pool.workers.forEach(this::register);
            this.exe = pool::queue;
        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            exe = this::executeInline;
            super.stop();
            pool.shutdownNow();
            pool = null;
            focus = null;
        }
    }

    @Override
    public final boolean concurrent() {
        return true;
    }


    private class MyWorkLoop extends BusyPool.WorkLoop {

        /** dummy Causable each worker schedules at the 0th position of the process table,
         * in which a worker will attempt to drain some or all of the queued work before returning to playing */

        /**
         * TODO use non-atomic version of this, slightly faster
         */
        final Random rng;
        private final NAR nar;

        public MyWorkLoop(Queue<Runnable> q, NAR nar) {
            super(q);
            this.nar = nar;

            rng = new XoRoShiRo128PlusRandom(System.nanoTime());
        }

        @Override
        protected void run(Object next) {
            executeInline(next);
        }

        int idles = 0;

        protected void idle() {
            Util.pauseNext(idles++);
        }

        @Override
        public void run() {

            final float[][] W = {null};


            Roulette.decideRouletteWhile(() -> (W[0] = focus.weight).length, c -> W[0][c], rng, (IntPredicate) x -> {
                if (x<=1 /*-1 or 0 */ || focus.active.isAllOff()) {
                    //idle(); //everything's off; gradually get bored and fall sleep
                    while (pollNext()); //WORK: special 'kernel' process
                    return true;
                }

                Causable cx = PoolMultiExec.this.focus.can.getSafe(x);
                if (cx == null || cx.id!=x)
                    return true; //the process at this position is not consistent with its id; may be in a changing state

                boolean singleton = cx.singleton();
                if (singleton) {
                    if (focus.active.compareAndSet(x, true, false)) {
                        //acquired the singleton access
                    } else {
                        return true; //try another
                    }
                } else {
                    if (!focus.active.get(x)) {
                        return true; //this is a busy singleton; try again
                    } else {
                        //its good
                    }
                }


                double donePrevMean = focus.doneMean[x];
                if (!Double.isFinite(donePrevMean))
                    donePrevMean = 0;

                double timePrev = focus.timeMean[x];
                if (!Double.isFinite(timePrev))
                    timePrev = 0;

                double timeslice =
                        //0.006; //6ms
                        0.01; //10ms

                //TODO this growth limit value should decrease throughout the cycle as each execution accumulates the total work it is being compared to
                //this will require doneMax to be an atomic accmulator for accuracy
                int itersNext = (int) Math.max(1,
                        Math.round(Math.min(donePrevMean * timeslice / timePrev, focus.doneMax[x] * focus.IterGrowthRateLimit))
                );

                //System.out.println(cx + " x " + iters + " @ " + n4(iterPerSecond[x]) + "iter/sec in " + Texts.timeStr(subTime*1E9));

                idles = 0; //reset idle count, get ready to actually do something

                int completed = -1;
                try {
                    completed = cx.run(nar, itersNext);
                } finally {
                    if (singleton) {

                        if (completed >= 0) {
                            focus.active.set(x); //release for another usage
                        } else {
                            //leave inactive
                        }
                    } else {
                        if (completed < 0) {
                            focus.active.clear(x); //set inactive
                        }
                    }
                }

                return true;
            });


        }


//                        protected long next() {
//
//                            int loopMS = nar.loop.periodMS.intValue();
//                            if (loopMS < 0) {
//                                loopMS = IDLE_PERIOD_MS;
//                            }
//                            long dutyMS =
//                                    Math.round(nar.loop.throttle.floatValue() * loopMS);
//
//                            //if (rng.nextInt(100) == 0)
//                            //    System.out.println(this + " " + Texts.timeStr(timeSinceLastBusyNS) + " since busy, " + Texts.timeStr(dutyMS*1E6) + " loop time" );
//
//                            if (dutyMS > 0) {
//                                return Math.round(nar.loop.jiffy.doubleValue() * dutyMS * 1E6);
//                            } else {
//                                return 0; //empty batch
//                            }
//
//                        }


    }
}
