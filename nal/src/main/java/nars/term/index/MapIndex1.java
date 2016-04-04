package nars.term.index;

import nars.concept.ConceptBuilder;
import nars.term.Compound;
import nars.term.TermBuilder;
import nars.term.Termed;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

/** implements AbstractMapIndex with one ordinary map implementation */
public class MapIndex1 extends AbstractMapIndex {

    public final Map<Termed,Termed> data;

    public MapIndex1(TermBuilder termBuilder, ConceptBuilder conceptBuilder, Map<Termed,Termed> compounds) {
        super(termBuilder, conceptBuilder);
        this.data = compounds;
    }


    @Nullable
    @Override
    protected final Termed theCompound(@NotNull Compound x, boolean create) {
        if (create) {
            return data.computeIfAbsent(x, (X) -> {
                Compound XX = (Compound)X; //??
                return internCompound(internSubterms(XX.subterms(), XX.op(), XX.relation(), XX.dt()));
            });
//            Termed existing = data.get(x);
//            if (existing!=null)
//                return existing;
//
//            Termed c = internCompound(internSubterms(x.subterms(), x.op(), x.relation(), x.dt()));
//            data.put(c, c);
//            return c;
        } else {
            return data.get(x);
        }
    }

    @Nullable
    @Override
    public final Termed set(@NotNull Termed t) {
        Termed existing = data.putIfAbsent(t, t);
        if ((existing !=null) && (existing!=t))
            throw new RuntimeException("pre-existing value");
        return t;
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        data.forEach((k,v)-> c.accept(v));
    }

    @Override
    public int size() {
        return data.size() /* + atoms.size? */;
    }


    @Nullable
    @Override
    public TermContainer theSubterms(TermContainer s) {
        return s;
    }

    @Override
    public int subtermsCount() {
        return -1; //unsupported
    }
}