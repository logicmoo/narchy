package nars.derive.op;

import jcog.Util;
import nars.*;
import nars.derive.Derivation;
import nars.derive.premise.PremiseRuleProto;
import nars.task.DebugDerivedTask;
import nars.task.DerivedTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.util.transform.Retemporalize;
import nars.time.Tense;
import nars.truth.Truth;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.*;
import static nars.Param.FILTER_SIMILAR_DERIVATIONS;
import static nars.time.Tense.ETERNAL;

public class Taskify extends AbstractPred<Derivation> {


    private final static Logger logger = LoggerFactory.getLogger(Taskify.class);
    /**
     * destination of any derived tasks; also may be used to communicate backpressure
     * from the recipient.
     */
    public final PremiseRuleProto.RuleCause channel;

    private static final Atomic TASKIFY = Atomic.the("taskify");

    public Taskify(PremiseRuleProto.RuleCause channel) {
        super(  $.func(TASKIFY, $.the(channel.id)) );
        this.channel = channel;
    }

    static boolean valid(Term x, byte punc) {
        return x != null &&
                x.unneg().op().conceptualizable &&
                !x.hasAny(Op.VAR_PATTERN) &&
                ((punc != BELIEF && punc != GOAL) || (!x.hasXternal() && !x.hasVarQuery()));
    }

    private boolean spam(Derivation d, int cost) {
        d.use(cost);
        d.concTerm = null; //erase immediately
        return true;
    }

    /**
     * note: the return value here shouldnt matter so just return true anyway
     */
    @Override
    public boolean test(Derivation d) {

        Term x0 = d.concTerm;

        final byte punc = d.concPunc;
        assert (punc != 0) : "no punctuation assigned";

        if ((punc == BELIEF || punc == GOAL) && x0.hasXternal()) {
            //HACK this is for deficiencies in the temporal solver that can be fixed
            x0 = x0.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
            if (!Taskify.valid(x0, d.concPunc)) {
                d.nar.emotion.deriveFailTemporal.increment();
                return spam(d, Param.TTL_DERIVE_TASK_FAIL);
            }
        }

        Term x1;
        try {
            x1 = d.anon.get(x0);
            if (x1 == null)
                throw new NullPointerException(); //return spam(d, Param.TTL_DERIVE_TASK_FAIL);
        } catch (RuntimeException r) {
            d.concTerm = null; //HACK
            return Derivation.fatal(r);
        }

        Term x = Task.forceNormalizeForBelief(x1);
        if (!x.op().conceptualizable)
            return spam(d, Param.TTL_DERIVE_TASK_FAIL);


        Truth tru;

        if (punc == BELIEF || punc == GOAL) {
            tru = d.concTruth;
            if (tru.conf() < d.confMin)
                return spam(d, Param.TTL_DERIVE_TASK_UNPRIORITIZABLE);

            //dither truth
            tru = tru.dithered(d.nar);
            if (tru == null)
                return spam(d, Param.TTL_DERIVE_TASK_UNPRIORITIZABLE);

        } else {
            tru = null; //questions and quests
        }


        long[] occ = d.concOcc!=null ? d.concOcc : new long[] { ETERNAL, ETERNAL };

        if (same(x, punc, tru, occ, d._task, d.nar) ||
                (d._belief != null && same(x, punc, tru, occ, d._belief, d.nar))) {
            d.nar.emotion.deriveFailParentDuplicate.increment();
            return spam(d, Param.TTL_DERIVE_TASK_SAME);
        }

        long start = occ[0], end = occ[1];
        assert (end >= start) : "task has reversed occurrence: " + start + ".." + end;


        DerivedTask t = Task.tryTask(x, punc, tru, (C, tr) -> {
            //dither time
            long _start, _end;
            if (start != ETERNAL) {
                int dither = d.ditherTime;
                _start = Tense.dither(start, dither);
                _end = Tense.dither(end, dither);
            } else {
                _start = _end = ETERNAL;
            }

            return Param.DEBUG ?
                    new DebugDerivedTask(C, punc, tr, _start, _end, d) :
                    new DerivedTask(C, punc, tr, _start, _end, d);
        });

        if (t == null) {
            d.nar.emotion.deriveFailTaskify.increment();
            return spam(d, Param.TTL_DERIVE_TASK_FAIL);
        }


        float priority = d.deriver.prioritize.pri(t, tru, d);
        if (priority != priority) {
            d.nar.emotion.deriveFailPrioritize.increment();
            return spam(d, Param.TTL_DERIVE_TASK_UNPRIORITIZABLE);
        }

        //these must be applied before possible merge on input to derivedTask bag
        t.cause = ArrayUtils.addAll(d.parentCause, channel.id);
        if (d.concSingle || (Param.OVERLAP_DOUBLE_SET_CYCLIC && d.overlapDouble))
            t.setCyclic(true);
        t.pri(priority);


        if (d.add(t) != t) {

            d.use(Param.TTL_DERIVE_TASK_REPEAT);
            d.nar.emotion.deriveFailDerivationDuplicate.increment();

        } else {

            if (Param.DEBUG)
                t.log(channel.ruleString);

            d.use(Param.TTL_DERIVE_TASK_SUCCESS);
            d.nar.emotion.deriveTask.increment();

        }

        return true;
    }

    protected boolean same(Term derived, byte punc, Truth truth, long[] occ, Task parent, NAR n) {
        if (parent.isDeleted())
            return false;

        if (FILTER_SIMILAR_DERIVATIONS) {

            if (parent.punc() == punc) {
                if (parent.term().equals(derived.term())) {
                    if (sameTime(occ, parent)) {

                        if ((punc == QUESTION || punc == QUEST) || (
                                Util.equals(parent.freq(), truth.freq(), n.freqResolution.floatValue()) &&
                                        parent.conf() <= truth.conf() - n.confResolution.floatValue()
                                                // / 2 /* + epsilon to avid creeping confidence increase */
                        )) {

                            if (Param.DEBUG_SIMILAR_DERIVATIONS)
                                logger.warn("similar derivation to parent:\n\t{} {}\n\t{}", derived, parent, channel.ruleString);

                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean sameTime(long[] occ, Task parent) {
//        if (parent.isEternal()) {
//            return true;
//        } else {
            return parent.contains(occ[0], occ[1]);
//            Tense.dither(parent.start(), n) == Tense.dither(occ[0], n) &&
//                    Tense.dither(parent.end(), n) == Tense.dither(occ[1], n);
//        }
    }

//    @Deprecated
//    protected boolean same(Task derived, Task parent, float truthResolution) {
//        if (parent.isDeleted())
//            return false;
//
//        if (derived.equals(parent)) return true;
//
//        if (FILTER_SIMILAR_DERIVATIONS) {
//
//            if (parent.term().equals(derived.term()) && parent.punc() == derived.punc() &&
//                    parent.start() == derived.start() && parent.end() == derived.end()) {
//                /*if (Arrays.equals(derived.stamp(), parent.stamp()))*/
//                if (parent.isQuestionOrQuest() ||
//                        (Util.equals(parent.freq(), derived.freq(), truthResolution) &&
//                                parent.evi() >= derived.evi())
//                ) {
//                    if (Param.DEBUG_SIMILAR_DERIVATIONS)
//                        logger.warn("similar derivation to parent:\n\t{} {}\n\t{}", derived, parent, channel.ruleString);
//
//
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

}