package nars.table;

import com.google.common.collect.Iterators;
import nars.NAR;
import nars.Task;
import nars.concept.CompoundConcept;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;

import static nars.time.Tense.ETERNAL;


/**
 * Stores beliefs ranked in a sorted ArrayList, with strongest beliefs at lowest indexes (first iterated)
 */
public class DefaultBeliefTable implements BeliefTable {

    @Nullable
    public EternalTable eternal = EternalTable.EMPTY;
    @Nullable
    public TemporalBeliefTable temporal = TemporalBeliefTable.EMPTY;


    /**
     * TODO this value can be cached per cycle (when,now) etc
     */
    @Nullable
    @Override
    public Truth truth(long when, long now, float dur) {

        Truth tt = temporal.truth(when, now, dur, eternal);

        return (tt != null) ? tt : eternal.truth();

    }

    @Override
    public boolean remove(Task x) {
        return (x.isEternal()) ? eternal.remove(x) : temporal.remove(x);
    }

    @Override
    public void clear() {
        temporal.clear();
        eternal.clear();
    }

    @NotNull
    @Override
    @Deprecated
    public final Iterator<Task> iterator() {
        return Iterators.concat(
                eternal.iterator(),
                temporal.iterator()
        );
    }

    @Override
    public void forEach(@NotNull Consumer<? super Task> action) {
        eternal.forEach(action);
        temporal.forEach(action);
    }

    @Override
    public float priSum() {
        final float[] total = {0};
        Consumer<Task> totaler = t -> total[0] += t.priSafe(0);
        eternal.forEach(totaler);
        temporal.forEach(totaler);
        return total[0];
    }

    @Override
    public int size() {
        return eternal.size() + temporal.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    @Deprecated
    public int capacity() {
        //throw new UnsupportedOperationException("doesnt make sense to call this");
        return eternal.capacity() + temporal.capacity();
    }

    @Override
    public final void capacity(int eternals, int temporals, NAR nar) {
        temporal.capacity(temporals, nar);
        eternal.capacity(eternals);
    }

    /**
     * get the most relevant belief/goal with respect to a specific time.
     */
    @Nullable
    @Override
    public Task match(long when, long now, float dur, @Nullable Task against, boolean noOverlap) {

        final Task ete = eternal.match();
        if (when == ETERNAL) {
            if (ete != null) {
                return ete;
            }
        }

        if (now != ETERNAL) {

            Task tmp = temporal.match(when, now, dur, against);

            if (tmp == null) {
                return ete;
            } else {
                if (ete == null) {
                    return tmp;
                } else {
                    return (ete.confWeight(when, dur) > tmp.confWeight(when, dur)) ?
                            ete : tmp;
                }
            }
        }

        return null;

    }


    @Override
    public TruthDelta add(@NotNull Task input, @NotNull QuestionTable questions, @NotNull CompoundConcept<?> concept, @NotNull NAR nar) {
        if (input.isEternal()) {

            if (eternal == EternalTable.EMPTY) {
                synchronized (concept) {
                    if (eternal == EternalTable.EMPTY) {
                        int cap = concept.state().beliefCap(concept, input.isBelief(), true);
                        if (cap > 0)
                            eternal = concept.newEternalTable(cap); //allocate
                        else
                            return null;
                    }
                }
            }

            return eternal.add(input, concept, nar);

        } else {
            if (temporal == TemporalBeliefTable.EMPTY) {
                synchronized (concept) {
                    if (temporal == TemporalBeliefTable.EMPTY) { //HACK double boiler
                        int cap = concept.state().beliefCap(concept, input.isBelief(), false);
                        if (cap > 0)
                            temporal = concept.newTemporalTable(cap); //allocate
                        else
                            return null;
                    }
                }
            }

            return temporal.add(input, eternal, concept, nar);
        }
    }


}



