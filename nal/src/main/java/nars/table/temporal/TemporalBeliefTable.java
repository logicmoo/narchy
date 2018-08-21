package nars.table.temporal;

import jcog.Skill;
import nars.Param;
import nars.Task;
import nars.control.Cause;
import nars.control.proto.Remember;
import nars.table.BeliefTable;
import nars.task.NALTask;
import nars.task.signal.SignalTask;

import java.util.function.Predicate;

import static nars.truth.polation.TruthIntegration.eviInteg;


/**
 * https://en.wikipedia.org/wiki/Compressed_sensing
 */
@Skill("Compressed_sensing")
public interface TemporalBeliefTable extends BeliefTable {


    void setCapacity(int temporals);

    void update(SignalTask x, Runnable change);

    long tableDur();

    void whileEach(Predicate<? super Task> each);

    /**
     * finds all temporally intersectnig tasks.  minT and maxT inclusive.  while the predicate remains true, it will continue scanning
     * TODO contains/intersects parameter
     */
    default void whileEach(long minT, long maxT, Predicate<? super Task> each) {
        whileEach(x -> {
            if (!x.isDeleted() && x.intersects(minT, maxT))
                return each.test(x);
            else
                return true;
        });
    }

    static void fundMerge(Task xy, Remember r, Task x, Task y) {


        ((NALTask)xy).cause(Cause.merge(Param.causeCapacity.intValue(), x, y));

        //factor in the evidence loss (and originality?) loss to reduce priority
        float exy = eviInteg(xy);
        float pFactor = exy / (eviInteg(x) + eviInteg(y));
        //assert(pFactor <= 1f);
        //float oxy = xy.originality();
        //float px = Util.unitize(exy/ eviInteg(x) ); // * (oxy * x.originality()));
        //float py = Util.unitize(exy/ eviInteg(y) ); // * (oxy * y.originality()));
        xy.take(x, pFactor, false, false);
        r.forget(x);
        xy.take(y, pFactor, false, false);
        r.forget(y);


        r.remember(xy);
    }


}
