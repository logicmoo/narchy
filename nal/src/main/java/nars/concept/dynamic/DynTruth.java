package nars.concept.dynamic;

import jcog.Util;
import jcog.list.FasterList;
import jcog.pri.Pri;
import jcog.pri.Priority;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.budget.BudgetFunctions;
import nars.term.Compound;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 12/4/16.
 */
public final class DynTruth implements Truthed {

    @Nullable public final FasterList<Task> e;
    public Truthed truth;

    public float freq;
    public float conf; //running product

    public DynTruth(FasterList<Task> e) {
        //this.t = t;
        this.e = e;
        this.truth = null;
    }

    public void setTruth(Truthed truth) {
        this.truth = truth;
    }

    @Nullable
    public float budget() {
        //RawBudget b = new RawBudget();
        int s = e.size();
        assert (s > 0);

        if (s > 1) {
            //float f = 1f / s;
            //            for (Task x : e) {
            //                BudgetMerge.plusBlend.apply(b, x.budget(), f);
            //            }
            //            return b;
            return e.maxValue(Task::priElseZero); //use the maximum of their truths
        } else {
            return e.get(0).priElseZero();
        }
    }

    @Nullable
    public long[] evidence() {

        //return e == null ? null :
        return Stamp.zip(e);
    }

    @Override
    @Nullable
    public PreciseTruth truth() {
        return conf <= 0 ? null : new PreciseTruth(freq, conf);
    }


    @Override
    public String toString() {
        return truth().toString();
    }

    @Nullable public DynamicBeliefTask task(@NotNull Compound c, boolean beliefOrGoal, long cre, long start, @Nullable Priority b, NAR nar) {

        Truth tr = truth();
        if (tr == null)
            return null;

        Priority priority;
        if (b != null) {
            if ((priority = b).isDeleted())
                return null;
        } else {
             float p = budget();
             if (p!=p)
                 return null; //deleted
            priority = new Pri(p);
        }


        //HACK try to reconstruct the term because it may be invalid
        int dt = c.dt();
        if (c.op().temporal && dt == DTERNAL && start!=ETERNAL)
            dt = 0; //actually it is measured at the current time so make it parallel

        c = compoundOrNull(nar.terms.the(c.op(), dt, c.toArray()));
        if (c == null)
            return null;

        if (null == (c = Task.content(c, nar)))
            return null;

        if (null == (c = nar.terms.retemporalize(c)))
            return null;

        // then if the term is valid, see if it is valid for a task
        if (!Task.taskContentValid(c, beliefOrGoal ? BELIEF : GOAL, null, true)) {
            return null;
        }

        long dur = (start!=ETERNAL && c.op() == CONJ) ? c.dtRange() : 0;

        DynamicBeliefTask dyn = new DynamicBeliefTask(c, beliefOrGoal ? Op.BELIEF : Op.GOAL,
                tr, cre, start, start + dur, evidence());
        dyn.setPri(priority);
        //        if (srcCopy == null) {
//            delete();
//        } else {
//            float p = srcCopy.priSafe(-1);
//            if (p < 0) {
//                delete();
//            } else {
//                setPriority(p);
//            }
//        }
//
//        return this;
        if (Param.DEBUG)
            dyn.log("Dynamic");

        return dyn;
    }
}
