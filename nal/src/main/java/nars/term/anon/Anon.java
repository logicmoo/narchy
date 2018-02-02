package nars.term.anon;

import jcog.list.FasterList;
import nars.Task;
import nars.subterm.Subterms;
import nars.subterm.TermVector;
import nars.task.TaskProxy;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.transform.DirectTermTransform;
import nars.term.transform.TermTransform;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import static nars.term.anon.Anom.MAX_ANOM;

/**
 * term anonymization context, for canonicalization and generification of compounds
 */
public class Anon {

    /** term -> id */
    final ObjectByteHashMap<Term> fwd;

    /** id -> term */
    public final FasterList<Term> rev = new FasterList<>(0);

    private final TermTransform PUT, GET;

    public Anon() {
        this(1);
    }

    public Anon(int estSize) {
        fwd = new ObjectByteHashMap(estSize);
        this.PUT = newPut();
        this.GET = newGet();
    }


    final ByteFunction<Term> nextUniqueAtom = (Term next) -> {
        int s = rev.addAndGetSize(next);
        assert (s < MAX_ANOM);
        return (byte) s;
    };

    protected TermTransform newPut() {
        return new DirectTermTransform() {
            @Override
            public final @Nullable Termed transformAtomic(Term atomic) {
                return put(atomic);
            }
        };
    }

    protected TermTransform newGet() {
        //can not be DirectTermTransform
        return new TermTransform() {
            @Override
            public final @Nullable Termed transformAtomic(Term atomic) {
                return get(atomic);
            }
        };
    }

    public void clear() {
        fwd.clear();
        rev.clear();
    }

    public Term put(Term x) {
        if (x instanceof AnonID) {
            //assert (!(x instanceof UnnormalizedVariable));
//            if (x instanceof UnnormalizedVariable)
//                throw new RuntimeException("unnormalized variable for Anon: " + x);
            return x; //ignore normalized variables since they are AnonID
        } else if (x instanceof Atomic) {

            if (x instanceof Int.IntRange) return x; //HACK

            return Anom.the[fwd.getIfAbsentPutWithKey(x, nextUniqueAtom)];

        } else {
            return PUT.transformCompound((Compound)x);
        }
    }

    public Term get(Term x) {
        if (x instanceof Anom) {
            return rev.get(((Anom) x).id - 1); //assume it is an int
        } else if (x instanceof Compound) {
            return GET.transformCompound((Compound) x);
        } else {
            return x; //ignore variables, ints
        }
    }

    public Task put(Task t) {
        Term x = t.term();
        assert (x.isNormalized()) : t + " has non-normalized Term content";
        Term y = put(x);
        if (y == null || y instanceof Bool) {
            throw new RuntimeException("Anon fail for term: " + t);
        }
        if (y instanceof Compound) {
            Subterms yy = y.subterms();
            if (yy instanceof TermVector)
                ((TermVector) yy).setNormalized();
        }

        return new TaskProxy.WithTerm(y, t);
    }


}
