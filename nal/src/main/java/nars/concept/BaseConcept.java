package nars.concept;

import jcog.bag.Bag;
import jcog.pri.PriReference;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.conceptualize.ConceptBuilder;
import nars.conceptualize.DefaultConceptBuilder;
import nars.conceptualize.state.ConceptState;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.table.TaskTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static nars.Op.*;
import static nars.conceptualize.state.ConceptState.Deleted;

/** concept of a compound term which can NOT name a task, so it has no task tables and ability to process tasks */
public class BaseConcept<T extends Term> implements Concept, Termlike {

    @NotNull public final T term;
    @NotNull public final BeliefTable beliefs;
    @NotNull public final BeliefTable goals;
    @NotNull public final QuestionTable quests;
    @NotNull public final QuestionTable questions;
    @NotNull public final Bag<Task,PriReference<Task>> taskLinks;
    @NotNull public final Bag<Term,PriReference<Term>> termLinks;
    @NotNull public transient ConceptState state = Deleted;

    @Nullable private Map meta;

    /**
     * Constructor, called in Memory.getConcept only
     *  @param term      A term corresponding to the concept
     * @param termLinks
     * @param taskLinks
     */
    public BaseConcept(@NotNull T term,
                       @NotNull BeliefTable beliefs, @NotNull BeliefTable goals,
                       @NotNull QuestionTable questions, @NotNull QuestionTable quests,
                       @NotNull Bag[] bags) {
        this.term = term;
        this.termLinks = bags[0];
        this.taskLinks = bags[1];
        this.beliefs = beliefs;
        this.goals = goals;
        this.questions = questions;
        this.quests = quests;
        this.state = Deleted;
    }

    public BaseConcept(@NotNull T term, BeliefTable beliefs, BeliefTable goals, ConceptBuilder conceptBuilder) {
        this(term, beliefs, goals, conceptBuilder.newQuestionTable(), conceptBuilder.newQuestionTable(), conceptBuilder.newLinkBags(term));
    }


    public static float valueIfProcessedAt(@NotNull Task t, float activation, long when, NAR n) {
        return 0.001f * activation * (t.isBeliefOrGoal() ? t.conf(when, n.dur()) : 0.5f);
    }


//    @Override
//    public @NotNull TermContainer subterms() {
//        return term.subterms();
//    }
//
//    @Override
//    public void setNormalized() {
//        //ignore
//        assert(isNormalized()): "why wasnt this already normalized";
//    }

    @Override
    public final Term term() {
        //return this;
        return term;
    }

    @Override
    public final @NotNull Op op() {
//        Op t = term.op();
//        assert(t!=NEG); //HACK
//        return t;
        return term.op();
    }


    @Override
    public void setMeta(@NotNull Map newMeta) {
        this.meta = newMeta;
    }


    @Override
    public @Nullable Map<Object, Object> meta() {
        return meta;
    }

    @Override
    public @NotNull Bag<Task,PriReference<Task>> tasklinks() {
        return taskLinks;
    }

    @NotNull
    @Override
    public Bag<Term,PriReference<Term>> termlinks() {
        return termLinks;
    }

    @Override
    public TermContainer templates() {
        return term instanceof Compound ? ((Compound)term).subterms() : TermContainer.NoSubterms;
    }

    /**
     * used for setting an explicit OperationConcept instance via java; activates it on initialization
     */
    public BaseConcept(@NotNull T term, @NotNull NAR n) {
        this(term,  n.conceptBuilder);
    }


    public BaseConcept(@NotNull T term, @NotNull ConceptBuilder b) {
        this(term, b.newBeliefTable(term, true), b.newBeliefTable(term, false),
                b.newQuestionTable(), b.newQuestionTable(),
                b.newLinkBags(term));
    }

    @Override
    public final ConceptState state() {
        return state;
    }

    @NotNull
    public QuestionTable quests() {
        return quests;
    }

    @NotNull
    public QuestionTable questions() {
        return questions;
    }

    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @NotNull
    public final BeliefTable beliefs() {
        return beliefs;
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @NotNull
    public final BeliefTable goals() {
        return goals;
    }

    protected final void beliefCapacity(int be, int bt, int ge, int gt) {

        beliefs.setCapacity(be, bt);
        goals.setCapacity(ge, gt);

    }


    @Override
    public final boolean equals(Object obj) {
        return this == obj || term.equals(obj);
    }

    @Override
    public final int hashCode() {
        return term.hashCode();
    }

    @Override
    public final String toString() {
        return term.toString();
    }

    @Override
    public int size() {
        return term.size();
    }

    /** first-level only */
    @Deprecated @Override public boolean contains(@NotNull Termlike t) {
        return term.contains(t);
    }

    @Deprecated
    @Override
    public boolean isTemporal() {
        return false; //term.isTemporal();
    }

    @Override
    public <T extends Term> @Nullable T sub(int i, @Nullable T ifOutOfBounds) {
        return term.sub(i, ifOutOfBounds);
    }

    @Override
    public boolean AND(Predicate<Term> v) {
        return term.AND(v);
    }

    @Override
    public boolean ANDrecurse(@NotNull Predicate<Term> v) {
        return term.ANDrecurse(v);
    }

    @Override
    public void recurseTerms(@NotNull Consumer<Term> v) {
        term.recurseTerms(v);
    }

    @Override
    public boolean OR(Predicate<Term> v) {
        return term.OR(v);
    }

    @Override
    public boolean ORrecurse(@NotNull Predicate<Term> v) {
        return term.ORrecurse(v);
    }

    @Deprecated
    @Override
    public int vars() {
        return term.vars();
    }

    @Deprecated
    @Override
    public int varIndep() {
        return term.varIndep();
    }

    @Deprecated
    @Override
    public int varDep() {
        return term.varDep();
    }

    @Deprecated
    @Override
    public int varQuery() {
        return term.varQuery();
    }

    @Deprecated
    @Override
    public int varPattern() {
        return term.varPattern();
    }

    @Deprecated
    @Override
    public int complexity() {
        return term.complexity();
    }

    @Deprecated
    @Override
    public int structure() {
        return term.structure();
    }

    @Override
    public int volume() {
        return term.volume();
    }

    @Override
    public boolean isNormalized() {
        return term.isNormalized(); //compound concepts may be un-normalized
    }

    @Override
    public boolean isDynamic() {
        return false; //concepts themselves are always non-dynamic
    }


    @Override
    public ConceptState state(@NotNull ConceptState p) {
        ConceptState current = this.state;
        if (current != p) {
            this.state = p;
            termLinks.setCapacity(p.linkCap(this, true));
            taskLinks.setCapacity(p.linkCap(this, false));

            int be = p.beliefCap(this, true, true);
            int bt = p.beliefCap(this, true, false);

            int ge = p.beliefCap(this, false, true);
            int gt = p.beliefCap(this, false, false);

            beliefCapacity(be, bt, ge, gt);
            questions.capacity(p.questionCap(true));
            quests.capacity(p.questionCap(false));

        }
        return p;
    }

    /**
     * Directly process a new task, if belief tables agree to store it.
     * Called exactly once on each task.
     */
    public void process(@NotNull Task t, @NotNull NAR n) {
        table(t.punc()).add(t, this, n);
    }

    public float valueIfProcessed(@NotNull Task t, float activation, NAR n) {
        //positive value based on the conf but also multiplied by the activation in case it already was known
        return valueIfProcessedAt(t, activation, n.time(), n);

//            @Override
//    public float value(@NotNull Task t, NAR n) {
//        byte p = t.punc();
//        if (p == BELIEF || p == GOAL) {// isGoal()) {
//            //example value function
//            long s = t.end();
//
//            if (s!=ETERNAL) {
//                long now = n.time();
//                long relevantTime = p == GOAL ?
//                        now - n.dur() : //present or future goal
//                        now; //future belief prediction
//
//                if (s > relevantTime) //present or future TODO irrelevance discount for far future
//                    return (float) (0.1f + Math.pow(t.conf(), 0.25f));
//            }
//        }
//
//        //return super.value(t, activation, n);
//        return 0;
//    }
    }

    @Override
    public void print(@NotNull Appendable out, boolean showbeliefs, boolean showgoals, boolean showtermlinks, boolean showtasklinks) {

        Consumer<Task> printTask = s -> {
            try {
                out.append(printIndent);
                out.append(s.toString());
                out.append(" ");
                Object ll = s.lastLogged();
                if (ll != null)
                    out.append(ll.toString());
                out.append('\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        try {
            if (showbeliefs) {
                out.append(" Beliefs:");
                if (beliefs().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    beliefs().forEachTask(printTask);
                }
                out.append(" Questions:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    questions().forEachTask(printTask);
                }
            }

            if (showgoals) {
                out.append(" Goals:");
                if (goals().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    goals().forEachTask(printTask);
                }
                out.append(" Quests:");
                if (questions().isEmpty()) out.append(" none").append('\n');
                else {
                    out.append('\n');
                    quests().forEachTask(printTask);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Nullable
    public TaskTable table(byte punc) {
        switch (punc) {
            case BELIEF:
                return beliefs;
            case GOAL:
                return goals;
            case QUESTION:
                return questions;
            case QUEST:
                return quests;
            default:
                throw new UnsupportedOperationException();
        }
    }

    public void forEachTask(boolean includeConceptBeliefs, boolean includeConceptQuestions, boolean includeConceptGoals, boolean includeConceptQuests, @NotNull Consumer<Task> each) {
        if (includeConceptBeliefs) beliefs.forEachTask(each);
        if (includeConceptQuestions) questions.forEachTask(each);
        if (includeConceptGoals) goals.forEachTask(each);
        if (includeConceptQuests) quests.forEachTask(each);
    }

    public void forEachTask(@NotNull Consumer<Task> each) {
        beliefs.forEachTask(each);
        questions.forEachTask(each);
        goals.forEachTask(each);
        quests.forEachTask(each);
    }

    @Override
    public void delete(@NotNull NAR nar) {
        termLinks.delete();
        taskLinks.delete();
        beliefs.clear();
        goals.clear();
        questions.clear();
        quests.clear();
        meta.clear();
    }

    @Override public Stream<Task> tasks(boolean includeBeliefs, boolean includeQuestions, boolean includeGoals, boolean includeQuests) {
        List<Stream<Task>> s = new LinkedList<>();
        if (includeBeliefs) s.add(beliefs.stream());
        if (includeGoals) s.add(goals.stream());
        if (includeQuestions) s.add(questions.stream());
        if (includeQuests) s.add(quests.stream());
        return s.stream().flatMap(x -> x);
    }
}

//    /**
//     * apply derivation feedback and update NAR emotion state
//     */
//    protected void feedback(@NotNull Task input, @NotNull TruthDelta delta, @NotNull CompoundConcept concept, @NotNull NAR nar) {
//
//        //update emotion happy/sad
//        Truth before = delta.before;
//        Truth after = delta.after;
//
//        float deltaSatisfaction, deltaConf, deltaFreq;
//
//
//        if (before != null && after != null) {
//
//            deltaFreq = after.freq() - before.freq();
//            deltaConf = after.conf() - before.conf();
//
//        } else {
//            if (before == null && after != null) {
//                deltaConf = after.conf();
//                deltaFreq = after.freq();
//            } else if (before!=null) {
//                deltaConf = -before.conf();
//                deltaFreq = -before.freq();
//            } else {
//                deltaConf = 0;
//                deltaFreq = 0;
//            }
//        }
//
//        Truth other;
//        int polarity = 0;
//
//        Time time = nar.time;
//        int dur = time.dur();
//        long now = time.time();
//        if (input.isBelief()) {
//            //compare against the current goal state
//            other = concept.goals().truth(now, dur);
//            if (other != null)
//                polarity = +1;
//        } else if (input.isGoal()) {
//            //compare against the current belief state
//            other = concept.beliefs().truth(now, dur);
//            if (other != null)
//                polarity = -1;
//        } else {
//            other = null;
//        }
//
//
//        if (other != null) {
//
//            float otherFreq = other.freq();
//
//            if (polarity==0) {
//
//                //ambivalence: no change
//                deltaSatisfaction = 0;
//
//            } else {
//
////                if (otherFreq > 0.5f) {
////                    //measure how much the freq increased since goal is positive
////                    deltaSatisfaction = +polarity * deltaFreq / (2f * (otherFreq - 0.5f));
////                } else {
////                    //measure how much the freq decreased since goal is negative
////                    deltaSatisfaction = -polarity * deltaFreq / (2f * (0.5f - otherFreq));
////                }
//
//                if (after!=null) {
//                    deltaSatisfaction = /*Math.abs(deltaFreq) * */ (2f * (1f - Math.abs(after.freq() - otherFreq)) - 1f);
//
//                    deltaSatisfaction *= (after.conf() * other.conf());
//
//                    nar.emotion.happy(deltaSatisfaction);
//                } else {
//                    deltaSatisfaction = 0;
//                }
//            }
//
//
//        } else {
//            deltaSatisfaction = 0;
//        }
//
//        feedback(input, delta, nar, deltaSatisfaction, deltaConf);
//
//    }
//
//    protected void feedback(@NotNull Task input, @NotNull TruthDelta delta, @NotNull NAR nar, float deltaSatisfaction, float deltaConf) {
//        if (!Util.equals(deltaConf, 0f, TRUTH_EPSILON))
//            nar.emotion.confident(deltaConf, input.term());
//
//        input.feedback(delta, deltaConf, deltaSatisfaction, nar);
//    }

//    private void checkConsistency() {
//        synchronized (tasks) {
//            int mapSize = tasks.size();
//            int tableSize = beliefs().size() + goals().size() + questions().size() + quests().size();
//
//            int THRESHOLD = 50; //to catch when the table explodes and not just an off-by-one inconsistency that will correct itself in the next cycle
//            if (Math.abs(mapSize - tableSize) > THRESHOLD) {
//                //List<Task> mapTasks = new ArrayList(tasks.keySet());
//                Set<Task> mapTasks = tasks.keySet();
//                ArrayList<Task> tableTasks = Lists.newArrayList(
//                        Iterables.concat(beliefs(), goals(), questions(), quests())
//                );
//                //Collections.sort(mapTasks);
//                //Collections.sort(tableTasks);
//
//                System.err.println(mapSize + " vs " + tableSize + "\t\t" + mapTasks.size() + " vs " + tableTasks.size());
//                System.err.println(Joiner.on('\n').join(mapTasks));
//                System.err.println("----");
//                System.err.println(Joiner.on('\n').join(tableTasks));
//                System.err.println("----");
//            }
//        }
//    }

//    public long minTime() {
//        ageFactor();
//        return min;
//    }
//
//    public long maxTime() {
//        ageFactor();
//        return max;
//    }
//
//    public float ageFactor() {
//
//        if (min == ETERNAL) {
//            //invalidated, recalc:
//            long t[] = new long[] { Long.MAX_VALUE, Long.MIN_VALUE };
//
//            beliefs.range(t);
//            goals.range(t);
//
//            if (t[0] == Long.MAX_VALUE) {
//                min = max= 0;
//            } else {
//                min = t[0];
//                max = t[1];
//            }
//
//        }
//
//        //return 1f;
//        long range = max - min;
//        /* history factor:
//           higher means it is easier to hold beliefs further away from current time at the expense of accuracy
//           lower means more accuracy at the expense of shorter memory span
//     */
//        float historyFactor = Param.TEMPORAL_DURATION;
//        return (range == 0) ? 1 :
//                ((1f) / (range * historyFactor));
//    }
