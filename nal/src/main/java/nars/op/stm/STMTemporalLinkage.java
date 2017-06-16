package nars.op.stm;

import jcog.data.FloatParam;
import jcog.data.MutableInteger;
import jcog.pri.PLink;
import jcog.pri.Pri;
import jcog.pri.mix.PSink;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.task.BinaryTask;
import nars.task.ITask;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;



/**
 * Short-term Memory Belief Event Induction.
 * Creates links between sequences of perceived events
 * Empties task buffer when plugin is (re)started.
 */
public final class STMTemporalLinkage extends STM {

    @NotNull
    public final Deque<Task> stm;

    final FloatParam strength = new FloatParam(1f, 0f, 1f);
    //private final PSink<Object, ITask> in;


    public STMTemporalLinkage(@NotNull NAR nar, int capacity) {
        this(nar, capacity, false);
    }

    public STMTemporalLinkage(@NotNull NAR nar, int capacity, boolean allowNonInput) {
        super(nar, new MutableInteger(capacity));

        this.allowNonInput = allowNonInput;
        strength.setValue(1f / capacity);

        //stm = Global.THREADS == 1 ? new ArrayDeque(this.capacity.intValue()) : new ConcurrentLinkedDeque<>();
        stm = new ArrayDeque<>(capacity);
        //stm = new ConcurrentLinkedDeque<>();

        //this.in = nar.in.stream(this);
    }

    @Override
    public void clear() {
        synchronized (stm) {
            stm.clear();
        }
    }

    @Override
    public final void accept(@NotNull Task t) {

        if (!t.isBeliefOrGoal()) {
            return;
        }


        int stmCapacity = capacity.intValue();


//        if (!currentTask.isTemporalInductable() && !anticipation) { //todo refine, add directbool in task
//            return false;
//        }

        //new one happened and duration is already over, so add as negative task
        //nal.emit(Events.EventBasedReasoningEvent.class, currentTask, nal);

        //final long now = nal.memory.time();


        Term tt = t.term();


        List<Task> queued;
        synchronized (stm) {
            int s = stm.size();
            if (s > 0) {
                queued = $.newArrayList(s);
                int numExtra = Math.max(0, (s) - stmCapacity);

                Iterator<Task> ss = stm.iterator();
                while (ss.hasNext()) {

                    Task previousTask = ss.next();

                    if ((numExtra > 0) || (previousTask.isDeleted())) {
                        numExtra--;
                        ss.remove();
                    } else {
                        /* WARNING: not exhaustive, may still be the same concept in temporal cases */
                        if (!tt.equals(previousTask.term()))
                            queued.add(previousTask);
                    }


                }
            } else {
                queued = null;
            }

            stm.add(t);
        }

        if (queued != null) {

            float strength = this.strength.floatValue();
            float tPri = t.priSafe(0);
            if (tPri > 0) {
                for (int i = 0, queuedSize = queued.size(); i < queuedSize; i++) {
                    Task u = queued.get(i);
                    /** current task's... */
                    float interStrength = tPri * u.priSafe(0) * strength;
                    if (interStrength >= Pri.EPSILON) {
                        STMLink s = new STMLink(t, u, interStrength);
                        s.run(nar);
                        //in.input(s); //<- spams the executor
                    }
                }
            }
        }

    }

    public static class STMLink extends BinaryTask<Task, Task> {

        public STMLink(@NotNull Task t, @NotNull Task u, float interStrength) {
            super(t, u, interStrength);
        }

        @Nullable
        @Override
        public ITask[] run(@NotNull NAR n) {

            float p = pri;
            if (p == p) {
                Task a = getOne();
                Concept ac = a.concept(n);
                if (ac != null) {
                    Task b = getTwo();
                    Concept bc = b.concept(n);
                    if (bc != null && !bc.equals(ac)) { //null or same concept?

                        //TODO handle overflow?
                        bc.termlinks().put(new PLink(ac, p));
                        ac.termlinks().put(new PLink(bc, p));

                        //tasklinks, not sure:
                        //        tgtConcept.tasklinks().put( new RawPLink(srcTask, scaleSrcTgt));
                        //        srcConcept.tasklinks().put( new RawPLink(tgtTask, scaleTgtSrc));

                    }
                }
            }

            return DeleteMe;
        }
    }

}
