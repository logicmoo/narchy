package nars.term;

import nars.Op;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.*;

/**
 * something which is like a term but isnt quite,
 * like a subterm container
 * <p>
 * Features exhibited by, and which can classify terms
 * and termlike productions
 */
public interface Termlike {


    Term sub(int i);


    /**
     * number of subterms. if atomic, size=0
     */
    int subs();

    /**
     * syntactic volume = 1 + total volume of terms = complexity of subterms - # variable instances
     */
    default int volume() {
        return 1 + sum(Term::volume);
    }


    /**
     * syntactic complexity 1 + total complexity number of leaf terms, excluding variables which have a complexity of zero
     */
    default int complexity() {
        return 1 + sum(Term::complexity);
    }

      /** only 1-layer (shallow, non-recursive) */
    default int sum(ToIntFunction<Term> value) {
        int x = 0;
        int s = subs();
        for (int i = 0; i < s; i++)
            x += value.applyAsInt(sub(i));
        return x;
    }

    default void recurseTerms(/*@NotNull*/ Consumer<Term> v) {
        int s = subs();
        for (int i = 0; i < s; i++)
            sub(i).recurseTerms(v);
    }

    default boolean hasXternal() {
        return hasAny(Op.Temporal) && OR(Term::hasXternal);
    }

    /** recursive, visits only 1 layer deep, and not the current if compound */
    default int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        int n = subs();
        for (int i = 0; i < n; i++)
            v = reduce.intValueOf(v, sub(i));
        return v;
    }
    /** recursive, visits each component */
    default int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        int n = subs();
        for (int i = 0; i < n; i++)
            v = sub(i).intifyRecurse(reduce, v);
        return v;
    }

    /**
     * counts subterms matching the predicate
     */
    default int subs(Predicate<Term> match) {
        return intifyShallow((c, sub) -> match.test(sub) ? c + 1 : c, 0);
    }
    /**
     * counts subterms matching the supplied op
     */
    default int subs(Op matchingOp) {
        return hasAny(matchingOp) ? subs(x -> x.op() == matchingOp) : 0;
    }

    /**
     * whether this term is or contains, as subterms, any temporal terms
     */
    default boolean isTemporal() {
        return OR(Term::isTemporal);
    }

    /**
     * structure hash bitvector
     */
    default int structure() {
        //return intifyShallow((s, x) -> x == null ? s : s | x.structure(), 0);
        return intifyShallow((s, x) -> s | x.structure(), 0);
    }

    /**
     * average of complexity and volume
     */
    default float voluplexity() {
        return (complexity() + volume()) / 2f;
    }


    /**
     * (first-level only, non-recursive)
     * if contained within; doesnt match this term (if it's a term);
     * false if term is atomic since it can contain nothing
     */
    default boolean contains(Term t) {
        return !impossibleSubTerm(t) && OR(t::equals);
    }
    default boolean containsNeg(Term x) {
        return contains(x.neg());
    }

    /**
     * shouldnt need overrridden
     */
    default boolean containsRecursively(Term t) {
        return containsRecursively(t, (x) -> true);
    }

    default boolean containsRecursively(Term t, Predicate<Term> inSubtermsOf) {
        return !impossibleSubTerm(t) && containsRecursively(t, false, inSubtermsOf);
    }

    /**
     * if root is true, the root()'s of the terms will be compared
     */
    boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf);


    default boolean hasAll(int structuralVector) {
        return Op.hasAll(structure(), structuralVector);
    }

    default boolean hasAny(int structuralVector) {
        return Op.hasAny(structure(), structuralVector);
    }

    default boolean hasAny(Op... oo) {
        if (oo.length == 0)
            return false;

        int checkStruct = 0;
        for (Op o : oo)
            checkStruct |= o.bit;

        return hasAny(checkStruct);
    }

    Op op();

    default boolean hasAny(/*@NotNull*/ Op op) {
        return hasAny(op.bit);
    }

    default boolean hasVarIndep() {
        return hasAny(Op.VAR_INDEP);
    }

    default boolean hasVarDep() {
        return hasAny(Op.VAR_DEP);
    }

    default boolean hasVarQuery() {
        return hasAny(Op.VAR_QUERY);
    }
    default boolean hasVarPattern() {
        return hasAny(Op.VAR_PATTERN);
    }

    default boolean impossibleSubTerm(/*@NotNull*/Termlike target) {
        return !hasAll(target.structure()) || impossibleSubTermVolume(target.volume());
    }

    /**
     * if it's larger than this term it can not be equal to this.
     * if it's larger than some number less than that, it can't be a subterm.
     */
    default boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
        return otherTermsVolume > volume();
    }










    /**
     * tries to get the ith subterm (if this is a TermContainer),
     * or of is out of bounds or not a container,
     * returns the provided ifOutOfBounds
     */
    default Term sub(int i, Term ifOutOfBounds) {
        return subs() <= i ? ifOutOfBounds : sub(i);
    }

    default boolean impossibleSubTermVolume(int otherTermVolume) {
        return otherTermVolume > volume() - subs();
    }


    default boolean impossibleSubTermOrEquality(/*@NotNull*/Term target) {
        return ((!hasAll(target.structure())) ||
                (impossibleSubTermOrEqualityVolume(target.volume())));
    }

    /**
     * returns true if evaluates true for any terms
     *
     * @param p
     */
    default boolean OR(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (p.test(sub(i)))
                return true;
        return false;
    }

    /**
     * returns true if evaluates true for all terms
     *
     * @param p
     */
    default boolean AND(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (!p.test(sub(i)))
                return false;
        return true;
    }

    /** supplies the current index as 2nd lambda argument */
    default boolean ANDwith(/*@NotNull*/ ObjectIntPredicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (!p.accept(sub(i), i))
                return false;
        return true;
    }

    /** supplies the current index as 2nd lambda argument */
    default boolean ORwith(/*@NotNull*/ ObjectIntPredicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (p.accept(sub(i), i))
                return true;
        return false;
    }

    default boolean ANDrecurse(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (!sub(i).ANDrecurse(p))
                return false;
        return true;
    }

    default boolean ORrecurse(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (sub(i).ORrecurse(p))
                return true;
        return false;
    }



    /**
     * stream of each subterm
     */
    default Stream<Term> subStream() {
        int subs = subs();
        switch (subs) {
            case 0:
                return Stream.empty();
            case 1:
                return Stream.of(sub(0));
            case 2:
                return Stream.of(sub(0), sub(1));
            case 3:
                return Stream.of(sub(0), sub(1), sub(2));
            default:
                return IntStream.range(0, subs).mapToObj(this::sub);
        }
    }


















    default int vars() {
        return hasVars() ? sum(Term::vars) : 0;
    }

    default boolean hasVars() {
        return hasAny(VAR_INDEP.bit | VAR_DEP.bit | VAR_QUERY.bit | VAR_PATTERN.bit);
    }

    /**
     * # of contained dependent variables in subterms (1st layer only)
     */
    default int varDep() {
        return sum(Term::varDep);
    }

    default int varIndep() {
        return sum(Term::varIndep);
    }

    default int varQuery() {
        return sum(Term::varQuery);
    }

    default int varPattern() {
        return sum(Term::varPattern);
    }
















    /**
     * return whether a subterm op at an index is an operator.
     * if there is no subterm or the index is out of bounds, returns false.
     */
    default boolean subIs(int i, Op o) {
        Term x = sub(i, null);
        return x != null && x.op() == o;
    }



}

