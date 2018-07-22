package nars.term.anon;

import jcog.WTF;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.compound.util.ByteAnonMap;
import nars.term.var.UnnormalizedVariable;
import nars.util.term.transform.TermTransform;
import org.jetbrains.annotations.Nullable;

/**
 * term anonymization context, for canonicalization and generification of compounds
 *         //return new DirectTermTransform() {
 *         //return new TermTransform.NegObliviousTermTransform() {
 */
public class Anon extends ByteAnonMap implements TermTransform.NegObliviousTermTransform {


    private boolean putOrGet = true;

    public Anon() {
        this(1);
    }

    public Anon(int estSize) {
        super(estSize);
    }

    public int uniques() {
        return idToTerm.size();
    }

    /** returns true if anything changed */
    public boolean rollback(int toUniques) {
        if (toUniques == 0) {
            clear();
            return true;
        }

        int max;
        if (toUniques < (max = uniques())) {
            for (int i = toUniques; i < max; i++)
                termToId.removeKey(idToTerm.get(i));
            idToTerm.removeAbove(toUniques);
            return true;
        }
        return false;
    }


    @Override
    public final @Nullable Term transformAtomic(Atomic atomic) {
        return putOrGet ? put(atomic) : get(atomic);
    }

    @Override
    public boolean eval() {
        return false;
    }

    public final Term put(Term x) {
        if (x instanceof AnonID) {
            return x;
        } else if (x instanceof Atomic) {

            if (x instanceof UnnormalizedVariable || x instanceof Int.IntRange)
                return x;

            return Anom.the[intern(x)];

        } else {
            putOrGet = true;
            Term y = transformCompound((Compound)x);
            validate(x, y);
            return y;
        }
    }

    public static void validate(Term x, Term y) {
        if (y.op()!=x.op() || y.volume()!=x.volume())
            throw new WTF();
    }

    public final Term get(Term x) {
        if (x instanceof Anom) {
            return interned((byte) ((Int) x).id);
        } else if (x instanceof Compound) {
            putOrGet = false;
            return transformCompound((Compound) x);
        } else {
            return x; 
        }
    }




}
