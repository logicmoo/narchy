package nars.table;

import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.nal.Stamp;
import nars.task.Revision;
import nars.task.TruthPolation;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import nars.util.Util;
import nars.util.data.list.MultiRWFasterList;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.set.primitive.ImmutableLongSet;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Param.rankTemporalByConfidence;
import static nars.Param.simultaneity;
import static nars.time.Tense.ETERNAL;
import static nars.truth.TruthFunctions.c2w;

/**
 * stores the items unsorted; revection manages their ranking and removal
 */
public class MicrosphereTemporalBeliefTable implements TemporalBeliefTable {



    private volatile int capacity;
    final MultiRWFasterList<Task> list;

    public MicrosphereTemporalBeliefTable(int initialCapacity) {
        super();
        this.list = MultiRWFasterList.newList(initialCapacity);
        this.capacity = initialCapacity;
    }

    @Override
    public Iterator<Task> iterator() {
        throw new UnsupportedOperationException();
        //return list.iterator();
    }

    @Override
    public final void forEach(Consumer<? super Task> action) {
        list.forEach(action);
    }

    public void capacity(int newCapacity, long now, @NotNull List<Task> removed) {

        if (this.capacity != newCapacity) {

            this.capacity = newCapacity;

            clean(removed);

            int nullAttempts = 4; //
            int toRemove = list.size() - newCapacity;
            for (int i = 0; i < toRemove && nullAttempts > 0; ) {

                Task ww = weakest(now); //read-lock
                if (ww!=null) {
                    remove(ww, removed);
                    i++;
                } else {
                    nullAttempts--;
                }
            }
        }

    }

    @Override
    public final int size() {
        return list.size();
    }

    @Override
    public final boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public final int capacity() {
        return capacity;
    }



    @Nullable
    @Override
    public final TruthDelta add(@NotNull Task input, EternalTable eternal, @NotNull List<Task> displ, Concept concept, @NotNull NAR nar) {

        int cap = capacity();
        if (cap == 0)
            return null;

        //the result of compression is processed separately
        final TruthDelta[] delta = new TruthDelta[1];

        list.withWriteLockAndDelegate(l -> {
            final Truth before;

            long now = nar.time();

            before = truth(now, eternal);

            Task next;
            if ((next = compress(input, now, eternal, displ, concept)) != null) {

                list.add(input);

                if (next != input && list.size() + 1 <= cap) {
                    list.add(next);
                }

                final Truth after = truth(now, eternal);
                delta[0] = new TruthDelta(before, after);
            }
        });

        return delta[0];
    }


    @Override
    public boolean removeIf(@NotNull Predicate<? super Task> o, List<Task> displ) {
        return list.removeIf(((Predicate<Task>) t -> {
            if (o.test(t)) {
                displ.add(t);
            }
            return false;
        }));
    }


    @Override
    public final boolean isFull() {
        return size() == capacity();
    }

//    @Override
//    public void minTime(long minT) {
//        this.min = minT;
//    }
//
//    @Override
//    public void maxTime(long maxT) {
//        this.max = maxT;
//    }

//
//    @Override
//    public final void range(long[] t) {
//        for (Task x : this.items) {
//            if (x != null) {
//                long o = x.occurrence();
//                if (o < t[0]) t[0] = o;
//                if (o > t[1]) t[1] = o;
//            }
//        }
//    }


    public boolean remove(@NotNull Object object) {
        return list.remove(object);
    }

    private final boolean remove(@NotNull Task x, @NotNull List<Task> displ) {
        if (list.remove(x)) {
            displ.add(x);
            return true;
        }
        return false;
    }


//    @Override
//    public final boolean remove(Object object) {
//        if (super.remove(object)) {
//            invalidRangeIfLimit((Task)object);
//            return true;
//        }
//        return false;
//    }


    public Task weakest(long now) {
        return list.minBy(x -> rankTemporalByConfidence(x, now));
    }

    public Task weakest(long now, @NotNull Task toMergeWith) {
        return list.minBy(rankPenalizingFreqAndTemporalDistance( toMergeWith, now ));
    }

    @NotNull public Function<Task, Float> rankPenalizingFreqAndTemporalDistance(@NotNull Task y, long now) {

        //(when selecting by minimum rank:)
        //  more confidence and more freq delta increases rank which means less likely to select
        //  more simultaneity should be more likely to select

        float duration = 2f;

        return x ->
            y.conf() +
            Math.abs(x.freq()-y.freq()) +
            Math.abs(x.occurrence() - y.occurrence())/duration
       ;

    }

    @NotNull public Function<Task, Float> rankPenalizingOverlap(long now, @NotNull Task toMergeWith) {
        long occ = toMergeWith.occurrence();
        ImmutableLongSet toMergeWithEvidence = toMergeWith.evidenceSet();
        return x -> rankPenalizingOverlap(x, toMergeWithEvidence, occ, now);
    }

    public static float rankPenalizingOverlap(@Nullable Task x, @NotNull LongSet evidence, long occ, long now) {
        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        return (x == null) ? Float.NEGATIVE_INFINITY : (Stamp.overlapFraction(evidence, x.evidence()) * rankTemporalByConfidence(x, now));
    }



    /**
     * frees one slot by removing 2 and projecting a new belief to their midpoint. returns the merged task
     */
    @Nullable
    protected Task compress(@Nullable Task input, long now, @Nullable EternalTable eternal, @NotNull List<Task> displ, @Nullable Concept concept) {

        int cap = capacity();

        if (size() < cap || clean(displ)) {
            return input; //no need for compression
        }


        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        float inputRank = input != null ? rankTemporalByConfidence(input, now) : Float.POSITIVE_INFINITY;

        Task a = weakest(now);
        //return rankTemporalByConfidenceAndOriginality(t, when, now, -1);
        if (a == null || inputRank <= rankTemporalByConfidence(a, now) || !remove(a, displ)) {
            //dont continue if the input was too weak, or there was a problem removing a (like it got removed already by a different thread or something)
            return null;
        }

        Task b = weakest(now, a);
        if (b != null && remove(b, displ)) {
            return merge(a, b, now, concept, eternal);
        } else {
            return input;
        }

    }

    /**
     * t is the target time of the new merged task
     */
    @Nullable
    private Task merge(@NotNull Task a, @NotNull Task b, long now, Concept concept, @Nullable EternalTable eternal) {

        float ac = c2w(a.conf());
        float bc = c2w(b.conf());
        long mid = (long) Math.round(Util.lerp(a.occurrence(), b.occurrence(), ac / (ac + bc)));

//        float f =
//                //more evidence overlap indicates redundant information, so reduce the confWeight (measure of evidence) by this amount
//                //TODO weight the contributed overlap amount by the relative confidence provided by each task
//                //1f - Stamp.overlapFraction(a.evidence(), b.evidence())/2f;
//                1f;
        float p = ac/(ac+bc);
        Truth t = Revision.revise(a, p, b, Param.TRUTH_EPSILON /*nar.confMin*/);

        if (t != null)
            return Revision.mergeInterpolate(a, b, mid, now, t, concept);

        return null;
    }


    @Nullable
    @Override
    public final Task match(long when, long now, @Nullable Task against) {

        if (against == null) {
            return list.maxBy(x -> rankTemporalByConfidence(x, now));
        } else {
            return list.maxBy(rankPenalizingOverlap(now, against));
        }

    }

    @Nullable
    public final Truth truth(long when, @Deprecated @Nullable EternalTable eternal) {
        return truth(when, when, eternal);
    }

    @Nullable
    @Override
    public final Truth truth(long when, long now, @Nullable EternalTable eternal) {


        Task topEternal = eternal.strongest();
        boolean includeEternal = topEternal != null;

        Task[] tr = list.toArray((i) -> new Task[i], includeEternal ? 1 : 0);
        int s = tr.length;
        if (includeEternal)
            tr[s -1] = topEternal;

        switch (s) {

            case 0:
                return null;

            case 1:
                Task the = tr[0];
                Truth res = the.truth();
                long o = the.occurrence();
                if ((now == ETERNAL || when == now) && o == when) //optimization: if at the current time and when
                    return res;
                else
                    return Revision.project(res, when, now, o, false);

            default:
                return new TruthPolation(s).truth(when, now, tr);

        }

    }


    private boolean clean(@NotNull List<Task> displ) {
        return list.removeIf(x -> {
            if (x == null) {
                return true;
            } else if (x.isDeleted()) {
                displ.add(x);
                return true;
            } else {
                return false;
            }
        });
    }


    //    public final boolean removeIf(@NotNull Predicate<? super Task> o) {
//
//        IntArrayList toRemove = new IntArrayList();
//        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
//            Task x = this.get(i);
//            if ((x == null) || (o.test(x)))
//                toRemove.add(i);
//        }
//        if (toRemove.isEmpty())
//            return false;
//        toRemove.forEach(this::remove);
//        return true;
//    }

    //    public Task weakest(Task input, NAR nar) {
//
//        //if (polation == null) {
//            //force update for current time
//
//        polation.credit.clear();
//        Truth current = truth(nar.time());
//        //}
//
////        if (polation.credit.isEmpty())
////            throw new RuntimeException("empty credit table");
//
//        List<Task> list = list();
//        float min = Float.POSITIVE_INFINITY;
//        Task minT = null;
//        for (int i = 0, listSize = list.size(); i < listSize; i++) {
//            Task t = list.get(i);
//            float x = polation.value(t, -1);
//            if (x >= 0 && x < min) {
//                min = x;
//                minT = t;
//            }
//        }
//
//        System.out.println("removing " + min + "\n\t" + polation.credit);
//
//        return minT;
//    }


    //    public @Nullable Truth topTemporalCurrent(long when, long now, @Nullable Task topEternal) {
//        //find the temporal with the best rank
//        Task t = topTemporal(when, now);
//        if (t == null) {
//            return (topEternal != null) ? topEternal.truth() : Truth.Null;
//        } else {
//            Truth tt = t.truth();
//            return (topEternal() != null) ? tt.interpolate(topEternal.truth()) : tt;
//
//            //return t.truth();
//        }
//    }


//    //NEEDS DEBUGGED
//    @Nullable public Truth topTemporalWeighted(long when, long now, @Nullable Task topEternal) {
//
//        float sumFreq = 0, sumConf = 0;
//        float nF = 0, nC = 0;
//
//        if (topEternal!=null) {
//            //include with strength of 1
//
//            float ec = topEternal.conf();
//
//            sumFreq += topEternal.freq() * ec;
//            sumConf += ec;
//            nF+= ec;
//            nC+= ec;
//        }
//
//        List<Task> temp = list();
//        int numTemporal = temp.size();
//
//        if (numTemporal == 1) //optimization: just return the only temporal truth value if it's the only one
//            return temp.get(0).truth();
//
//
////        long maxtime = Long.MIN_VALUE;
////        long mintime = Long.MAX_VALUE;
////        for (int i = 0, listSize = numTemporal; i < listSize; i++) {
////            long t = temp.get(i).occurrence();
////            if (t > maxtime)
////                maxtime = t;
////            if (t < mintime)
////                mintime = t;
////        }
////        float dur = 1f/(1f + (maxtime - mintime));
//
//
//        long mdt = Long.MAX_VALUE;
//        for (int i = 0; i < numTemporal; i++) {
//            long t = temp.get(i).occurrence();
//            mdt = Math.min(mdt, Math.abs(now - t));
//        }
//        float window = 1f / (1f + mdt/2);
//
//
//        for (int i = 0, listSize = numTemporal; i < listSize; i++) {
//            Task x = temp.get(i);
//
//            float tc = x.conf();
//
//            float w = TruthFunctions.temporalIntersection(
//                    when, x.occurrence(), now, window);
//
//            //strength decreases with distance in time
//            float strength =  w * tc;
//
//            sumConf += tc * w;
//            nC+=tc;
//
//            sumFreq += x.freq() * strength;
//            nF+=strength;
//        }
//
//        return nC == 0 ? Truth.Null :
//                new DefaultTruth(sumFreq / nF, (sumConf/nC));
//    }

}
