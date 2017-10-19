package nars.term.transform;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntSupplier;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

abstract public class Retemporalize implements CompoundTransform {


    public static final Retemporalize retemporalizeAllToDTERNAL = new RetemporalizeAll(DTERNAL);
    public static final Retemporalize retemporalizeAllToXTERNAL = new RetemporalizeAll(XTERNAL);
    public static final Retemporalize retemporalizeXTERNALToDTERNAL = new RetemporalizeFromTo(XTERNAL, DTERNAL);
    public static final Retemporalize retemporalizeXTERNALToZero = new RetemporalizeFromTo(XTERNAL, 0);

    public static final Retemporalize retemporalizeConceptual = new Retemporalize() {

        @Nullable
        @Override
        public Term transform(Compound x, Op op, int dt) {
            if (op == IMPL) {
                if (dt!=DTERNAL) {
                    //special handling
                    Term subj = x.sub(0).root();
                    Term pred = x.sub(1).root();
                    return IMPL.the((subj.equals(pred)) ? XTERNAL : DTERNAL, subj, pred);
                } else {
                    return x; //unchanged
                }
            } else {
                return super.transform(x, op, dt);
            }
        }

        @Override
        public int dt(Compound x) {
            if (x.op() == CONJ && x.subs() == 2) return XTERNAL;
            else return DTERNAL;
        }
    };


    @Nullable
    @Override
    public Term transform(Compound x, Op op, int dt) {
        if (!x.hasAny(TemporalBits)) {
            return x;
        } else {
            return CompoundTransform.super.transform(x, op, op.temporal ? dt(x) : DTERNAL);
        }
    }


    @Override
    abstract public int dt(Compound x);

    @Deprecated
    public static final class RetemporalizeAll extends Retemporalize {

        final int targetDT;

        public RetemporalizeAll(int targetDT) {
            this.targetDT = targetDT;
        }

        @Override
        public final int dt(Compound x) {
            return targetDT;
        }
    }

    public static final class RetemporalizeFromTo extends Retemporalize {

        final int from, to;

        public RetemporalizeFromTo(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int dt(Compound x) {
            int dt = x.dt();
            return dt == from ? to : dt;
        }
    }

    public static final class RetemporalizeFromToFunc extends Retemporalize {

        final int from;
        final IntSupplier to;

        public RetemporalizeFromToFunc(int from, IntSupplier to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public int dt(Compound x) {
            int dt = x.dt();
            return dt == from ? to.getAsInt() : dt;
        }
    }
}
