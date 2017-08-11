package nars.index.term.map;

import nars.concept.PermanentConcept;
import nars.index.term.StaticTermIndex;
import nars.term.Termed;

import java.util.function.BiFunction;

/**
 * Index which is supported by Map/Cache-like operations
 */
public abstract class MaplikeTermIndex extends StaticTermIndex {


    public static final BiFunction<? super Termed, ? super Termed, ? extends Termed> setOrReplaceNonPermanent = (prev, next) -> {
        if (prev instanceof PermanentConcept && !(next instanceof PermanentConcept))
            return prev;
        return next;
    };

}
