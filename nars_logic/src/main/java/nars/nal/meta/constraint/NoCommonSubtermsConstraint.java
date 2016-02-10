package nars.nal.meta.constraint;

import nars.Global;
import nars.term.Compound;
import nars.term.Term;
import nars.term.transform.subst.FindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;


public final class NoCommonSubtermsConstraint implements MatchConstraint {

    private final Term b;

    public NoCommonSubtermsConstraint(Term b) {
        this.b = b;
    }

    @Override
    public boolean invalid(Term x, Term y, @NotNull FindSubst f) {
        Term B = f.getXY(b);
        if (B != null) {
            Set<Term> tmpSet = Global.newHashSet(0);
            return sharedSubterms(y, B, tmpSet);
        }
        return false;
    }

    @NotNull
    @Override
    public String toString() {
        return "noCommonSubterms(" + b + ')';
    }

    private static boolean sharedSubterms(Term a, Term b, Set<Term> s) {
        addUnmatchedSubterms(a, s, null);
        return !addUnmatchedSubterms(b, null, s); //we stop early this way (efficiency)
    }

    private static boolean addUnmatchedSubterms(Term x, @Nullable Set<Term> AX, @Nullable Set<Term> BX) {
        if (BX != null && BX.contains(x)) { //by this we can stop early
            return false;
        }

        if (AX != null && AX.add(x) && x instanceof Compound) {
            Compound c = (Compound) x;
            int l = c.size();
            for (int i = 0; i < l; i++) {
                Term d = c.term(i);
                if (!addUnmatchedSubterms(d, AX, BX)) {
                    //by this we can stop early
                    return false;
                }
            }
        }

        return true;
    }



}
