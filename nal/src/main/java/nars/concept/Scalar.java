package nars.concept;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.concept.dynamic.DynamicBeliefTable;
import nars.table.EternalTable;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import org.jetbrains.annotations.Nullable;

/**
 * dynamically computed time-dependent function
 */
public class Scalar extends Sensor {


//        private final LongToFloatFunction belief;
//        private final LongToFloatFunction goal;

    public Scalar(Term c, LongToFloatFunction belief, NAR n) {
        this(c, belief, null, n);
    }

    public Scalar(Term term, @Nullable LongToFloatFunction belief, @Nullable LongToFloatFunction goal, NAR n) {
        super(term,
                belief != null ? new ScalarBeliefTable(term, true, belief, n) : n.conceptBuilder.newTable(term, true),
                goal != null ? new ScalarBeliefTable(term, false, goal, n) : n.conceptBuilder.newTable(term, false),
                n.conceptBuilder);
//            this.belief = belief;
//            this.goal = goal;
//        n.on(this);
    }

    /** samples at time-points */
    static class ScalarBeliefTable extends DynamicBeliefTable {

        private final LongToFloatFunction value;
        long stampSeed, stampStart;

        protected ScalarBeliefTable(Term term, boolean beliefOrGoal, LongToFloatFunction value, NAR n) {
            super(term, beliefOrGoal, EternalTable.EMPTY,
                    n.conceptBuilder.newTemporalTable(term));
            stampStart = n.time();

            this.stampSeed = term.hashCode();

            this.value = value;
        }

        @Override
        protected Task taskDynamic(long start, long end, Term template /* ignored */, NAR nar) {
            long mid = Tense.dither((start+end)/2L, nar);
            Truth t = truthDynamic(mid, mid, template, nar);
            if (t == null)
                return null;
            else {

                long stampDelta = mid - stampStart; //TODO optional dithering
                long stamp = stampDelta ^ stampSeed; //hash
                return new SignalTask(term, punc(), t, nar.time(), mid, mid, stamp);
            }
        }

        @Override
        protected @Nullable Truth truthDynamic(long start, long end, Term template /* ignored */, NAR nar) {
            long t = (start + end) / 2L; //time-point
            float f = value.valueOf(t);
            return f == f ? truth(f, nar) : null;
        }

        /** truther: value -> truth  */
        protected Truth truth(float v, NAR n) {
            return $.t(v, n.confDefault(punc()));
        }

    }
}
