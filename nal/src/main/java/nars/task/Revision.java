package nars.task;

import jcog.Util;
import jcog.data.set.MetalLongSet;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.subterm.Subterms;
import nars.task.util.TaskRegion;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.Conj;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.Truth;
import nars.truth.Truthed;
import nars.truth.polation.TruthPolation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.CONJ;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static nars.truth.TruthFunctions.c2wSafe;

/**
 * Revision / Projection / Revection Utilities
 */
public class Revision {

    public static final Logger logger = LoggerFactory.getLogger(Revision.class);

    @Nullable
    public static Truth revise(/*@NotNull*/ Truthed a, /*@NotNull*/ Truthed b, float factor, float minEvi) {

        float ae = a.evi();
        float be = b.evi();
        float w = ae + be;
        float e = w * factor;

        return e <= minEvi ?
                null :
                PreciseTruth.byEvi(
                        (ae * a.freq() + be * b.freq()) / w,
                        e
                );
    }


    public static Truth revise(/*@NotNull*/ Truthed a, /*@NotNull*/ Truthed b) {
        return revise(a, b, 1f, 0f);
    }


    private static Term intermpolate(/*@NotNull*/ Term a, long bOffset, /*@NotNull*/ Term b, float aProp, float curDepth, NAR nar) {

        if (a.equals(b)/* && bOffset == 0*/)
            return a;

        if (a instanceof Atomic || b instanceof Atomic)
            return Null; //atomics differ

        Op ao = a.op(), bo = b.op();
        if (ao != bo)
            return Null;


        int len = a.subs();
        assert len > 0;
//        if (len == 0) {
//            //WTF
//            if (a.op() == PROD) return a;
////            else
////                throw new WTF();
//        }


        if (ao.temporal) {
            if (ao == CONJ && curDepth == 1) {
                return Conj.conjIntermpolate(a, b, aProp, bOffset); //root only: conj sequence merge
            } else  {
                return dtMergeDirect(a, b, aProp, curDepth, nar);
            }
        } else {

            Subterms aa = a.subterms(), bb = b.subterms();
//            if (aa.equals(bb))
//                return a;

            Term[] ab = new Term[len];
            boolean change = false;
            for (int i = 0; i < len; i++) {
                Term ai = aa.sub(i), bi = bb.sub(i);
                if (!ai.equals(bi)) {
                    Term y = intermpolate(ai, 0, bi, aProp, curDepth / 2f, nar);
                    if (y instanceof Bool)
                        return Null;

                    if (!ai.equals(y)) {
                        change = true;
                        ai = y;
                    }
                }
                ab[i] = ai;
            }

            return !change ? a : ao.the(chooseDT(a, b, aProp, nar), ab);
        }

    }


    /*@NotNull*/
    private static Term dtMergeDirect(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, float depth, NAR nar) {


        Term a0 = a.sub(0), a1 = a.sub(1), b0 = b.sub(0), b1 = b.sub(1);

        int dt = chooseDT(a, b, aProp, nar);
        if (a0.equals(b0) && a1.equals(b1)) {
            return a.dt(dt);
        } else {

            depth /= 2f;

            Term na = intermpolate(a0, 0, b0, aProp, depth, nar);
            if (na == Null || na == Bool.False) return na;

            Term nb = intermpolate(a1, 0, b1, aProp, depth, nar);
            if (nb == Null || nb == Bool.False) return nb;

            return a.op().the(dt, na, nb);
        }

    }

    public static int chooseDT(Term a, Term b, float aProp, NAR nar) {
        int adt = a.dt(), bdt = b.dt();
        return chooseDT(adt, bdt, aProp, nar);
    }

    static int chooseDT(int adt, int bdt, float aProp, NAR nar) {
        int dt;
        if (adt == bdt) {
            dt = adt;
        } else if (adt == XTERNAL || bdt == XTERNAL) {

            dt = adt == XTERNAL ? bdt : adt;
            //dt = choose(adt, bdt, aProp);

        } else if (adt == DTERNAL || bdt == DTERNAL) {

            dt = DTERNAL;
            //dt = adt == DTERNAL ? bdt : adt;
            //dt = choose(adt, bdt, aProp, nar.random());

        } else {
            dt = merge(adt, bdt, aProp, nar);
        }


        return Tense.dither(dt, nar);
    }

    /**
     * merge delta
     */
    static int merge(int adt, int bdt, float aProp, NAR nar) {
        /*if (adt >= 0 == bdt >= 0)*/ { //require same sign ?

            int range = //Math.max(Math.abs(adt), Math.abs(bdt));
                        Math.abs(adt - bdt);
            int ab = Util.lerp(aProp, bdt, adt);
            int delta = Math.max(Math.abs(ab-adt), Math.abs(ab- bdt));
            float ratio = ((float) delta) / range;
            if (ratio <= nar.intermpolationRangeLimit.floatValue()) {
                return ab;
            }
        }

        //discard temporal information by resorting to eternity
        return DTERNAL;
    }

//    /** merge occurrence */
//    public static long merge(long at, long bt, float aProp, NAR nar) {
//        long dt;
//        long diff = Math.abs(at - bt);
//        if (diff == 1) {
//            return choose(at, bt, aProp, nar.random());
//        }
//        if ((float) diff /nar.dur() <= nar.intermpolationRangeLimit.floatValue()) {
//            //merge if within a some number of durations
//            dt = Util.lerp(aProp, bt, at);
//        } else {
//            dt = ETERNAL;
//        }
//        return dt;
//    }

    //    static Term choose(Term a, Term b, float aProp, /*@NotNull*/ Random rng) {
//        return (rng.nextFloat() < aProp) ? a : b;
//    }
//    static int choose(int a, int b, float aProp, /*@NotNull*/ Random rng) {
//        return rng.nextFloat() < aProp ? a : b;
//    }
//
//    static long choose(long a, long b, float aProp, /*@NotNull*/ Random rng) {
//        return rng.nextFloat() < aProp ? a : b;
//    }

//    /*@NotNull*/
//    public static Term[] choose(/*@NotNull*/ Term[] a, Term[] b, float aBalance, /*@NotNull*/ Random rng) {
//        int l = a.length;
//        Term[] x = new Term[l];
//        for (int i = 0; i < l; i++) {
//            x[i] = choose(a[i], b[i], aBalance, rng);
//        }
//        return x;
//    }


    public static Term intermpolate(/*@NotNull*/ Term a, /*@NotNull*/ Term b, float aProp, NAR nar) {
        return intermpolate(a, 0, b, aProp, nar);
    }

    /**
     * a is left aligned, dt is any temporal shift between where the terms exist in the callee's context
     */
    public static Term intermpolate(/*@NotNull*/ Term a, long dt, /*@NotNull*/ Term b, float aProp, NAR nar) {
        Term term = intermpolate(a, dt, b, aProp, 1, nar);

        if (term.volume() > nar.termVolumeMax.intValue())
            return Null;

        return term;
    }


    /** 2-ary merge with quick overlap filter */
    public static Task merge(TaskRegion x, TaskRegion y, NAR nar) {

        return Stamp.overlaps((Task) x, (Task) y) ? null : merge(nar, x, y);

    }

    @Nullable
    private static Task merge(NAR nar, TaskRegion... tt) {
        assert tt.length > 1;
        long[] u = Tense.union(tt);
        return merge(nar,  u[0], u[1], tt);
    }

    /**
     * warning: output task will have zero priority and input tasks will not be affected
     * this is so a merge construction can be attempted without actually being budgeted
     *
     * also cause merge is deferred in the same way
     */
    @Nullable
    private static Task merge(NAR nar, long start, long end, TaskRegion... tasks) {

        assert(tasks.length > 1);

        float eviMin = c2wSafe(nar.confMin.floatValue());

        TruthPolation T = Param.truth(start, end, 0).add(tasks);

        MetalLongSet stamp = T.filterCyclic(true, 2);
        if (stamp == null)
            return null;

        if (T.size() == 1)
            return null; //fail

        Truth truth = T.truth(nar, eviMin);
        if (truth == null)
            return null;


        Truth cTruth = truth.dithered(nar);
        if (cTruth == null)
            return null;

        byte punc = T.punc();
        return Task.tryTask(T.term, punc, cTruth, (c, tr) -> {
            int dith = nar.dtDither();
            return new UnevaluatedTask(c, punc,
                    tr,
                    nar.time(), Tense.dither(start, dith), Tense.dither(end, dith),
                    Stamp.sample(Param.STAMP_CAPACITY, stamp /* TODO account for relative evidence contributions */, nar.random())
            );
        });
    }


    /**
     * heuristic representing the difference between the dt components
     * of two temporal terms.
     * 0 means they are identical or otherwise match.
     * > 0 means there is some difference.
     * <p>
     * this adds a 0.5 difference for && vs &| and +1 for each dt
     * XTERNAL matches anything
     */
    public static float dtDiff(Term a, Term b) {
        float d = dtDiff(a, b, 1);
        //return Util.assertUnitized(d);
        return d;
    }

    private static float dtDiff(Term a, Term b, int depth) {
        if (a.equals(b))
            return 0f;

        Op ao = a.op(), bo = b.op();
        if ((ao != bo) || (a.volume() != b.volume()) || (a.structure() != b.structure()))
            return Float.POSITIVE_INFINITY;

        Subterms aa = a.subterms(), bb = b.subterms();
//        int len = bb.subs();

        float d = 0;

        //        if (a.op() == CONJ && !aSubsEqualsBSubs) {
//
//            Conj c = new Conj();
//            String as = Conj.sequenceString(a, c).toString();
//            String bs = Conj.sequenceString(b, c).toString();
//
//            int levDist = Texts.levenshteinDistance(as, bs);
//            float seqDiff = (float) levDist / Math.min(as.length(), bs.length());
//
//
//            float rangeDiff = Math.max(1f, Math.abs(a.dtRange() - b.dtRange()));
//
//            d += (1f + rangeDiff) * (1f + seqDiff);
//
//            return Float.POSITIVE_INFINITY;
//
//        } else {
        if (!aa.equals(bb)) {

            if (aa.subs() != bb.subs())
                return Float.POSITIVE_INFINITY;

            d = dtDiff(aa, bb, true, depth);

//            if (!Float.isFinite(d) && len == 2 && ao.commutative && aa.hasAny(Op.Temporal) ) {
//                //try reversing
//                d = dtDiff(aa, bb, false, depth);
//            }


        } else {

            int adt = a.dt(), bdt = b.dt();
            if (adt != bdt) {
                if (adt == XTERNAL || bdt == XTERNAL) {
                    //zero, match
                    int other = adt == XTERNAL ? b.dt() : a.dt();
                    float range = other!=DTERNAL ? Math.max(1, Math.abs(other)) : 0.5f;
                    d += 0.25f / range; //undercut the DT option
                } else {

                    boolean ad = adt == DTERNAL;
                    boolean bd = bdt == DTERNAL;
                    int range;
                    float delta;
                    if (!ad && !bd) {
                        range = Math.max(Math.abs(adt), Math.abs(bdt));
                        delta = Math.abs(Math.abs(adt - bdt));
                    } else {
                        range = Math.max(1, Math.abs(ad ? b.dt() : a.dt()));
                        delta = 0.5f; //one is dternal the other is not, record at least some difference (half time unit)
                    }
                    assert(delta > 0 && range > 0);
                    d += delta / range;
                }
            }

        }

        return d / depth;
    }

    private static float dtDiff(Subterms aa, Subterms bb, boolean parity, int depth) {
        float d = 0;
        int len = aa.subs();
        for (int i = 0; i < len; i++) {
            Term ai = aa.sub(i);
            float dx = dtDiff(ai, bb.sub(parity ? i : (len-1)-i), depth + 1);
            if (!Float.isFinite(dx)) {
                return Float.POSITIVE_INFINITY;
            }
            d += dx;
        }
        return d;
        //return d/len; // avg
    }

//    public static Task mergeOrChoose(@Nullable Task x, @Nullable Task y, long start, long end, Predicate<Task> filter, NAR nar) {
//        if (x == null && y == null)
//            return null;
//
//        if (filter != null) {
//            if (x != null && !filter.test(x))
//                x = null;
//            if (y != null && !filter.test(y))
//                y = null;
//        }
//
//        if (y == null)
//            return x;
//
//        if (x == null)
//            return y;
//
//        if (x.equals(y))
//            return x;
//
//
//        Top<Task> top = new Top<>(t -> TruthIntegration.eviInteg(t, 1));
//
//        if (x.term().equals(y.term()) && !Stamp.overlapsAny(x, y)) {
//
//            Task xy = merge(nar, nar.dur(), start, end, true, x, y);
//            if (xy != null && (filter == null || filter.test(xy)))
//                top.accept(xy);
//        }
//        top.accept(x);
//        top.accept(y);
//
//        return top.the;
//    }
}



























































































































































































































































































































































































































































































































































































































































































































































































































