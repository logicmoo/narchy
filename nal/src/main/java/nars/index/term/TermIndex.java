package nars.index.term;

import nars.Builtin;
import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.build.ConceptBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.subst.MapSubst;
import nars.term.subst.MapSubst1;
import nars.term.transform.CompoundTransform;
import nars.term.transform.Retemporalize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;

/**
 *
 */
public abstract class TermIndex implements TermContext {


    private static final Logger logger = LoggerFactory.getLogger(TermIndex.class);
    public NAR nar;
    protected ConceptBuilder conceptBuilder;


    /**
     * internal get procedure
     */
    @Nullable
    public abstract Termed get(@NotNull Term key, boolean createIfMissing);

//    @Override
//    protected int dt(int dt) {
//        NAR n = this.nar;
//        if (n == null)
//            return dt;
//
//        switch (dt) {
//            case DTERNAL:
//            case XTERNAL:
//            case 0:
//                return dt; //no-change
//        }
//
//        return Math.abs(dt) < n.dur() ? 0 : dt;
//    }

    /**
     * sets or replaces the existing value, unless the existing value is a PermanentConcept it must not
     * be replaced with a non-Permanent concept
     */
    public abstract void set(@NotNull Term src, Termed target);

    public final void set(@NotNull Termed t) {
        set(t instanceof Term ? (Term) t : t.term(), t);
    }


    abstract public void clear();

    abstract public void forEach(Consumer<? super Termed> c);


    /**
     * called when a concept has been modified, ie. to trigger persistence
     */
    public void commit(Concept c) {
        //by default does nothing
    }

    public void start(NAR nar) {

        this.nar = nar;
        this.conceptBuilder = nar.conceptBuilder;

        for (Concept t : Builtin.statik)
            set(t);

        Builtin.load(nar);

    }

    /**
     * # of contained terms
     */
    public abstract int size();


    /**
     * a string containing statistics of the index's current state
     */
    @NotNull
    public abstract String summary();

    public abstract void remove(@NotNull Term entry);


//    public final HijacKache<Compound, Term> normalizations =
//            new HijacKache<>(Param.NORMALIZATION_CACHE_SIZE, 4);
//    public final HijacKache<ProtoCompound, Term> terms =
//            new HijacKache<>(Param.TERM_CACHE_SIZE, 4);

//    final Function<? super ProtoCompound, ? extends Term> termizer = pc -> {
//
//        return theSafe(pc.op(), pc.dt(), pc.terms() );
//    };
//
//    private int volumeMax(Op op) {
//        if (nar!=null) {
//            return nar.termVolumeMax.intValue();
//        } else {
//            return Param.COMPOUND_VOLUME_MAX;
//        }
//    }

//    @NotNull
//    private final Term theSafe(@NotNull Op o, int dt, @NotNull Term[] u) {
//        try {
//            return super.the(o, dt, u);
//            //return t == null ? False : t;
//        } catch (@NotNull InvalidTermException | InvalidTaskException x) {
//            if (Param.DEBUG_EXTRA) {
//                logger.warn("{x} : {} {} {}", x, o, dt, u);
//            }
//        } catch (Throwable e) {
//            logger.error("{x} : {} {} {}", e, o, dt, u);
//        }
//        return False; //place a False placeholder so that a repeat call will not have to discover this manually
//    }

//    @NotNull
//    public final Term the(@NotNull Compound csrc, @NotNull TermContainer newSubs) {
//        if (csrc.subterms().equals(newSubs)) {
//            return csrc;
//        } else {
//            return the(csrc.op(), csrc.dt(), newSubs.terms());
//        }
//    }

    @NotNull
    public final Term the(@NotNull Compound csrc, @NotNull Term... args) {
        return csrc.equalTerms(args) ? csrc : csrc.op().the(csrc.dt(), args);
    }

    @NotNull
    public final Term the(@NotNull Compound csrc, int newDT) {
        return csrc.dt() == newDT ? csrc : csrc.op().the(newDT, csrc.toArray());
    }

//    @Override
//    public final @NotNull Term the(@NotNull Op op, int dt, @NotNull Term[] args) throws InvalidTermException {
//
////        int totalVolume = 0;
////        for (Term x : u)
////            totalVolume += x.volume();
//
////        if (totalVolume > volumeMax(op))
////            throw new InvalidTermException(op, dt, u, "Too voluminous");
//
//        boolean cacheable =
//                //(totalVolume > 2)
//                        //&&
//                (op !=INH) || !(args[0].op() == PROD && args[1].op()==ATOM && get(args[1]) instanceof Functor) //prevents caching for potential transforming terms
//                ;
//
//        if (cacheable) {
//
//            return terms.computeIfAbsent(new ProtoCompound.RawProtoCompound(op, dt, args), termizer);
//
//        } else {
//            return super.the(op, dt, args);
//        }
//    }

//    @Deprecated
//    public final @NotNull Term the(@NotNull Op op, @NotNull Term... tt) {
//        return the(op, DTERNAL, tt); //call this implementation's, not super class's
//    }


    public void print(@NotNull PrintStream out) {
        forEach(out::println);
        out.println();
    }


    abstract public Stream<Termed> stream();



//    @Nullable
//    public Term transform(@NotNull Compound src, @NotNull ByteList path, @NotNull Term replacement) {
//        return transform(src, path, 0, replacement);
//    }
//
//    @Nullable
//    private Term transform(@NotNull Term src, @NotNull ByteList path, int depth, @NotNull Term replacement) {
//        int ps = path.size();
//        if (ps == depth)
//            return replacement;
//        if (ps < depth)
//            throw new RuntimeException("path overflow");
//
//        if (!(src instanceof Compound))
//            return src; //path wont continue inside an atom
//
//        int n = src.size();
//        Compound csrc = (Compound) src;
//
//        Term[] target = new Term[n];
//
//
//        boolean changed = false;
//        for (int i = 0; i < n; ) {
//            Term x = csrc.sub(i);
//            Term y;
//            if (path.get(depth) != i) {
//                //unchanged subtree
//                y = x;
//            } else {
//                //replacement is in this subtree
//                y = transform(x, path, depth + 1, replacement);
//                changed = true;
//            }
//
//            target[i++] = y;
//        }
//
//        if (!changed)
//            return csrc;
//
//        return csrc.op().the(csrc.dt(), target);
//    }


    /** un-normalized */
    @NotNull public <T extends Term> T termRaw(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) Narsese.term(termToParse, false);
    }

    /** normalized */
    @NotNull public <T extends Term> T term(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) (Narsese.term(termToParse, true));
    }


    /**
     * applies normalization and anonymization to resolve the term of the concept the input term maps t
     * term should be conceptualizable prior to calling this
     */
    @Nullable
    public final Concept concept(@NotNull Term term, boolean createIfMissing) {

        assert (term.op() != NEG); //term = term.unneg();

        @Nullable Termed c = get(term, createIfMissing);
        if (!(c instanceof Concept)) {
//            if (createIfMissing) {
//                throw new Concept.InvalidConceptException(term, "Failed to build concept");
//            }
            return null;
        }

        Concept cc = (Concept) c;
        if (cc.isDeleted()) {
            cc.state(conceptBuilder.init());
        }
        commit(cc);
        return cc;
    }


    @Nullable
    public final Term replace(@NotNull Term src, @NotNull Map<Term, Term> m) {
        return new MapSubst(m).transform(src);
    }

    @Nullable
    public final Term replace(@NotNull Term src, @NotNull Term from, @NotNull Term to) {
        return new MapSubst1(from, to).transform(src);
    }


    protected final void onRemove(Termed value) {
        if (value instanceof Concept) {
            if (value instanceof PermanentConcept) {
                //refuse deletion
                set(value);
            } else {

                Concept c = (Concept) value;
                onBeforeRemove(c);
                c.delete(nar);
            }
        }
    }

    protected void onBeforeRemove(Concept c) {

    }



    @Nullable
    public Term retemporalize(@NotNull Term tx, Retemporalize r) {
        if (!(tx instanceof Compound)) return tx;

        Compound x = (Compound)tx;

        Term y = x.transform(r.dt(x), r);
        if (!(y instanceof Compound)) {
            return y;
        } else {
            Compound yy = (Compound)y;


            //            int ydt = yy.dt();
//            if (ydt ==XTERNAL|| ydt ==DTERNAL) {
//                int zdt = r.dt(x);
//                if (ydt!=zdt)
//                    //yy = compoundOrNull(transform(yy, zdt, CompoundTransform.Identity));
//                    yy = compoundOrNull(yy.op().the(zdt, yy.toArray()));
//            }
//            if (yy == null)
//                return null;
            return yy.normalize();
        }

    }

    @Nullable
    public Term queryToDepVar(@NotNull Compound term) {
        return term.transform(CompoundTransform.queryToDepVar);
    }


    public final Retemporalize retemporalizeDTERNAL = new Retemporalize.RetemporalizeNonXternal(DTERNAL);
    public final Retemporalize retemporalizeZero = new Retemporalize.RetemporalizeNonXternal(0);




}
