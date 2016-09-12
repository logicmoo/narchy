package nars.nal.meta.op;

import nars.Op;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.PremiseEval;
import org.jetbrains.annotations.NotNull;

/**
 * requires both subterms to match a minimum bit structure
 */
public final class SubTermsStructure extends AtomicBoolCondition {

    public final int bits;
    @NotNull
    private final transient String id;


    public SubTermsStructure(int bits) {
        this(Op.VAR_PATTERN, bits);
    }

    public SubTermsStructure(@NotNull Op matchingType, int bits) {

        this.bits = SubTermStructure.filter(matchingType, bits);

        id = "SubTermsStruct:" +
                Integer.toString(bits, 16);
    }


    @NotNull
    @Override
    public String toString() {
        return id;
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseEval ff, int now) {
        /*Compound t = ff.term;
        return !t.term(subterm).impossibleStructureMatch(bits);*/
        return ff.subTermsMatch(bits);
    }
}
