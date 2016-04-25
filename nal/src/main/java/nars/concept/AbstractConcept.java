package nars.concept;

import javassist.scopedpool.SoftValueHashMap;
import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Created by me on 7/29/15.
 */
public abstract class AbstractConcept<T extends Term> implements Concept {

    protected final Bag<Task> taskLinks;
    protected final Bag<Termed> termLinks;

    @NotNull
    public final T term;


    @Nullable
    protected Map meta;

    protected AbstractConcept(@NotNull T term, Bag<Task> taskLinks, Bag<Termed> termLinks) {
        this.term = term;
        this.taskLinks = taskLinks;
        this.termLinks = termLinks;
    }

    public static final Logger logger = LoggerFactory.getLogger(AbstractConcept.class);

    /** returns the outgoing component only */
    @Nullable
    static final Concept linkSub(@NotNull Concept source, @NotNull Termed targetTerm,
                                              @NotNull Budgeted b, float subScale, boolean alsoReverse,
                                              @Nullable MutableFloat conceptOverflow,
                                              @Nullable MutableFloat termlinkOverflow, @NotNull NAR nar) {

        /*
        if (!(task.isStructual() && (cLink0.getType() == TermLink.TRANSFORM))) {

        }*/


        /** activate concept and insert tasklink */
        Concept target = nar.conceptualize(targetTerm, b, subScale, conceptOverflow, false);
        if (target == null)
            throw new RuntimeException("termlink to null concept");

        if (target == source)
            throw new RuntimeException("termlink self-loop");


        /** insert termlink target to source */
        if (alsoReverse) {
            subScale /= 2; //divide among both directions
            target.termlinks().put(source, b, subScale, termlinkOverflow);
        }

        /** insert termlink source to target */
        source.termlinks().put(target, b, subScale, termlinkOverflow);

        return target;
    }


    @Override @NotNull
    public Term term() {
        return term;
    }

    /**
     * metadata table where processes can store and retrieve concept-specific data by a key. lazily allocated
     */
    @Nullable
    @Override
    public final Map<Object, Object> meta() {
        return meta;
    }


    @NotNull
    @Override public <C> C meta(@NotNull Object key, @NotNull BiFunction value) {
        if (meta == null) {
            Object v;
            put(key, v = value.apply(key, null));
            return (C)v;
        } else {
            return (C) meta.compute(key, value);
        }
    }

    /** like Map.put for storing data in meta map
     *  @param value if null will perform a removal
     * */
    @Override
    @Nullable
    public final Object put(@NotNull Object key, @Nullable Object value) {

        Map<Object, Object> currMeta = meta;

        if (value != null) {

            if (currMeta == null) {
                this.meta = currMeta =
                        //new WeakIdentityHashMap();
                        new SoftValueHashMap(1);
            }

            return currMeta.put(key, value);
        }
        else {
            return currMeta != null ? currMeta.remove(key) : null;
        }

    }

    @Override
    public final boolean equals(@NotNull Object obj) {
        return (this == obj) || term.equals(obj);
    }

    @Override
    public final int hashCode() {
        return term.hashCode();
    }

    @Override
    public final int compareTo(@NotNull Object o) {
        return term.compareTo(o);
    }

    /**
     * Return a string representation of the concept, called in ConceptBag only
     *
     * @return The concept name, with taskBudget in the full version
     */
    @Override
    public final String toString() {  // called from concept bag
        //return (super.toStringBrief() + " " + key);
        //return super.toStringExternal();
        return term.toString();
    }

    /**
     * Task links for indirect processing
     */
    @Override
    public final Bag<Task> tasklinks() {
        return taskLinks;
    }

    /**
     * Term links between the term and its components and compounds; beliefs
     */
    @Override
    public final Bag<Termed> termlinks() {
        return termLinks;
    }


    public final boolean isConceptOf(Termed t) {
        return t == this ? true : t.equalsAnonymously(term());
    }

    /**
     * when a task is processed, a tasklink
     * can be created at the concept of its term
     */
    @Override public boolean link(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {

        if (b instanceof Task) {
            linkTask((Task)b, scale);
        }

        return true;
    }

    public void linkTask(@NotNull Task t, float scale) {
        if (term.vars()==0 || !isConceptOf(t)) { //insert tasklink to its terms own concept only if no variables
            taskLinks.put(t, t/*.budget()*/, scale, null);
        }
    }


//    /**
//     * called from {@link NARRun}
//     */
//    @Override
//    public String toStringLong() {
//        String res =
//                toStringWithBudget() + " " + getTerm().toString()
//                        + toStringIfNotNull(getTermLinks().size(), "termLinks")
//                        + toStringIfNotNull(getTaskLinks().size(), "taskLinks")
//                        + toStringIfNotNull(getBeliefs().size(), "beliefs")
//                        + toStringIfNotNull(getGoals().size(), "goals")
//                        + toStringIfNotNull(getQuestions().size(), "questions")
//                        + toStringIfNotNull(getQuests().size(), "quests");
//
//        //+ toStringIfNotNull(null, "questions");
//        /*for (Task t : questions) {
//            res += t.toString();
//        }*/
//        // TODO other details?
//        return res;
//    }

//    private String toStringIfNotNull(final Object item, final String title) {
//        if (item == null) {
//            return "";
//        }
//
//        final String itemString = item.toString();
//
//        return new StringBuilder(2 + title.length() + itemString.length() + 1).
//                append(' ').append(title).append(':').append(itemString).toString();
//    }

//    /** called by memory, dont call self or otherwise */
//    public void delete() {
//        /*if (getMemory().inCycle())
//            throw new RuntimeException("concept " + this + " attempt to delete() during an active cycle; must be done between cycles");
//        */
//
//        if (getMeta() != null) {
//            getMeta().clear();
//            setMeta(null);
//        }
//        //TODO clear bags
//    }

}
