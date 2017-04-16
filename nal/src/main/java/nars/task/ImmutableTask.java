package nars.task;

import jcog.Util;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.RawBudget;
import nars.task.util.InvalidTaskException;
import nars.term.Compound;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.apache.commons.collections4.map.Flat3Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static nars.Op.*;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 2/24/17.
 */
public class ImmutableTask extends RawBudget implements Task {

    public final Compound term;
    public final Truth truth;
    public final byte punc;
    private final long creation, start, end;
    public final long[] stamp;
    final int hash;

    public Map meta = null;


    public static ImmutableTask Eternal(Compound term, byte punc, Truth truth, long creation, long[] stamp) {
        return new ImmutableTask(term, punc, truth, creation, ETERNAL, ETERNAL, stamp);
    }


    public ImmutableTask(Compound term, byte punc, Truth truth, long creation, long start, long end, long[] stamp) {


        if (term.op() == NEG) {
            Compound term2 = compoundOrNull(term.unneg());
            if (term2 == null)
                throw new InvalidTaskException(term, "became non-compound on un-negation");
            term = term2;
            if (truth != null)
                truth = truth.negated();
        }

        if (truth == null && ((punc == BELIEF) || (punc == GOAL)))
            throw new InvalidTaskException(term, "null truth");

        if (term.varQuery() > 0 && (punc == BELIEF || punc == GOAL))
            throw new InvalidTaskException(term, "query variable in belief or goal");

        this.priority = 0;

        this.term = term;
        this.truth = truth;
        this.punc = punc;
        this.creation = creation;
        this.start = start;
        this.end = end;
        this.stamp = stamp;

        int h = Util.hashCombine(
                term.hashCode(),
                punc,
                Arrays.hashCode(stamp)
        );

        if (stamp.length > 1) {

            Truth t = truth();

            h = Util.hashCombine(
                    h,
                    Long.hashCode(start),
                    Long.hashCode(end)
            );

            if (t != null)
                h = Util.hashCombine(h, t.hashCode());
        }

        if (h == 0) h = 1; //reserve 0 for non-hashed

        this.hash = h;
    }

    @Override
    public final int hashCode() {
        return hash;
    }

    @Override
    public final boolean equals(Object that) {
        return this == that ||
                (that instanceof Task &&
                        hashCode() == that.hashCode() &&
                        Task.equivalentTo(this, (Task) that, true, true, true, true, true));

    }

    @Nullable
    @Override
    public Truth truth() {
        return truth;
    }

    @Override
    public byte punc() {
        return punc;
    }

    @Override
    public long creation() {
        return creation;
    }


    @Override
    public @NotNull Compound term() {
        return term;
    }

    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }


    @Override
    public @NotNull long[] stamp() {
        return stamp;
    }

    @Override
    public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {

    }

    @Override
    public boolean delete() {
        if (super.delete()) {
            if (!Param.DEBUG)
                this.meta = null; //.clear();
            return true;
        }
        return false;
    }

    @Override
    @Deprecated
    public @Nullable List log() {
        if (meta != null) {
            Map m = meta;
            Object s = m.get(String.class);
            if (s != null)
                return (List) s;
        }
        return null;
    }


    @NotNull
    @Override
    @Deprecated
    public String toString() {
        return appendTo(null, null).toString();
    }


    @Override
    public Map meta() {
        return meta;
    }

    @Override
    public void meta(Object key, Object value) {
        //synchronized (this) {
        if (meta == null) {
            //meta = new UnifiedMap(1); /* for compactness */
            meta = new Flat3Map(); /* for compactness */
        }

        meta.put(key, value);
        //}
    }

    @Override
    public <X> X meta(Object key) {
        if (meta != null) {
            //synchronized (this) {
            return (X) meta.get(key);
            //}
        }
        return null;
    }

    @Nullable
    public Task project(long newStart, int dur, float confMin) {
        float newConf = conf(newStart, dur);
        if (newConf < confMin)
            return null;


        ImmutableTask t = new ImmutableTask(term, punc, $.t(freq(), newConf), creation, newStart, newStart, stamp);
        t.budgetSafe(this);
        //t.meta
        //t.log("Projected")
        return t;
    }

    public Task clone(Compound x) {
        ImmutableTask t = new ImmutableTask(x, punc, truth, creation, start(), end(), stamp);
        t.budgetSafe(this);
        return t;
    }

}
