package nars.index.term;

import nars.term.Term;
import nars.term.Termed;

import java.util.function.Function;

/**
 * interface necessary for evaluating terms
 */
public interface TermContext extends Function<Term,Termed> {


    /** elides superfluous .term() call */
    default Term applyTermIfPossible(/*@NotNull*/ Term x) {
        Termed y = apply(x);
        return y != null ? y.term() : x;
    }


}
