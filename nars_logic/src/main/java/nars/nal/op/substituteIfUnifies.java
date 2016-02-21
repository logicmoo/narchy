package nars.nal.op;

import nars.Op;
import nars.nal.meta.PremiseMatch;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** substituteIfUnifies(term, variableType, varFrom, varTo)
 * TODO is this better named "substituteAll"
 * */
public final class substituteIfUnifies extends substitute {

    @Nullable
    @Override public Term function(@NotNull Compound p, @NotNull PremiseMatch r) {
        final Term[] xx = p.terms();
        final Term term = xx[0];
        final Term opT = xx[1];
        final Term x = xx[2];
        final Term y = xx[3];

        Op op = substitute.getOp(opT);
        if (op == null)
            //throw new RuntimeException("unrecognizd subst type: " + type);
            return null;

        OneMatchFindSubst omf = new OneMatchFindSubst(op, r, term);
        Term mm = omf.tryMatch(x, y);
        omf.delete();
        return mm;
    }

    private final static class OneMatchFindSubst extends FindSubst {

        private final Term xterm;
        @NotNull
        private final PremiseMatch r;
        @Nullable
        private Term result;

        public OneMatchFindSubst(@NotNull Op op, @NotNull PremiseMatch r, Term xterm) {
            super(op, r.currentPremise.memory().random, r);
            this.xterm = xterm;
            this.r = r;
        }

        /** terminates after the first match */
        @Override public boolean onMatch() {
            //apply the match before the xy/yx mapping gets reverted after leaving the termutator
            result = subst(r, this, xterm);
            return false;
        }

        @Nullable
        public Term tryMatch(@NotNull Term x, @NotNull Term y) {
            matchAll(x, y, true);
            return result;
        }

    }
}
