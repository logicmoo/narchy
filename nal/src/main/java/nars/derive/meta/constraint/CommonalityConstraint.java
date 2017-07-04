package nars.derive.meta.constraint;

import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.Unify;
import org.jetbrains.annotations.NotNull;

/**
 * note: if the two terms are equal, it is automatically invalid ("neq")
 */
public abstract class CommonalityConstraint extends MatchConstraint {

    private final Term other;

    public CommonalityConstraint(String func, Term target, Term other) {
        super(func, target, other);
        this.other = other;
    }

    @Override
    public final boolean invalid(@NotNull Term y, @NotNull Unify f) {

        Term x = f.resolve(other);
        if (x == null)
            return false; //not invalid until both are present to be compared

        if (x.equals(y)) {
            return true;
        }

        if (y instanceof Compound) {
            Compound C = (Compound) y;
            return x instanceof Compound ?
                    invalid((Compound) x, C)
                    :
                    invalid(/*(Term)*/x, C);
        } else {
            return invalid(x, y);
        }
    }


    /**
     * equality will have already been tested prior to calling this
     */
    @NotNull
    protected abstract boolean invalid(Term x, Term y);

    /**
     * equality will have already been tested prior to calling this
     */
    @NotNull
    protected abstract boolean invalid(Compound x, Compound y);

    /**
     * equality will have already been tested prior to calling this
     */
    @NotNull
    protected abstract boolean invalid(Term x, Compound y);
}
