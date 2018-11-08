package nars.derive.premise;

import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.data.map.CustomConcurrentHashMap;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.concept.Operator;
import nars.derive.Derivation;
import nars.derive.filter.CommutativeConstantPreFilter;
import nars.derive.filter.DoublePremiseRequired;
import nars.derive.filter.UnifyPreFilter;
import nars.derive.op.*;
import nars.op.SubIfUnify;
import nars.subterm.BiSubterm;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.PREDICATE;
import nars.term.util.transform.DirectTermTransform;
import nars.term.var.VarPattern;
import nars.truth.func.NALTruth;
import nars.truth.func.TruthFunc;
import nars.unify.constraint.*;
import nars.unify.op.TaskPunctuation;
import nars.unify.op.TermMatch;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.eclipse.collections.impl.set.mutable.UnifiedSet;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static jcog.data.map.CustomConcurrentHashMap.*;
import static nars.Op.*;
import static nars.subterm.util.SubtermCondition.*;
import static nars.term.control.PREDICATE.sortByCostIncreasing;
import static nars.unify.op.TaskPunctuation.Belief;
import static nars.unify.op.TaskPunctuation.Goal;

/**
 * A rule which matches a Premise and produces a Task
 * contains: preconditions, predicates, postconditions, post-evaluations and metainfo
 */
public class PremiseRuleSource extends ProxyTerm  {

    private static final Pattern ruleImpl = Pattern.compile("\\|\\-");
    private final String source;
    public final Truthify truthify;
    /**
     * return this to being a inline evaluable functor
     */
    static final IntroVars introVars = new IntroVars();
    /**
     * conditions which can be tested before unification
     */

    private final MutableSet<UnifyConstraint> constraints;
    protected final ImmutableSet<UnifyConstraint> CONSTRAINTS;
    private final MutableSet<PREDICATE> pre;
    protected final PREDICATE[] PRE;
    protected final Occurrify.TaskTimeMerge time;
    protected final boolean varIntro;
    protected final byte taskPunc;
    protected final Term postcons;
    protected final byte puncOverride;
    protected final Term beliefTruth;
    protected final Term goalTruth;

    protected final Term taskPattern;
    protected final Term beliefPattern;
    protected final Term concPattern;


    private static final PatternIndex INDEX = new PatternIndex();
    protected final Termify termify;
    protected final MutableSet<UnifyConstraint> constraintSet;

    private PremiseRuleSource(String ruleSrc) throws Narsese.NarseseException {
        super(
                INDEX.rule(new UppercaseAtomsToPatternVariables().transform($.pFast(parseRuleComponents(ruleSrc))))
        );

        this.source = ruleSrc;

        constraints = new UnifiedSet(8);
        pre = new UnifiedSet(8);

        /**
         * deduplicate and generate match-optimized compounds for rules
         */


        Term[] precon = ref.sub(0).arrayShared();
        Term[] postcon = ref.sub(1).arrayShared();


        this.taskPattern = PatternIndex.patternify(precon[0]);
        this.beliefPattern = PatternIndex.patternify(precon[1]);
        if (beliefPattern.op() == Op.ATOM) {
            throw new RuntimeException("belief term must contain no atoms: " + beliefPattern);
        }

        Term originalConcPattern = PatternIndex.patternify(postcon[0]);
        Term concPattern = originalConcPattern;

        {
            //direct substitution macros in conclusions

            //if (!filteredConcPattern.equals(taskPattern))
//            if (!taskPattern.op().temporal && !taskPattern.op().commutative)
            if (!taskPattern.op().var)
                concPattern = concPattern.replace(taskPattern, Derivation.TaskTerm);
            //if (!filteredConcPattern.equals(beliefPattern))
//            if (!beliefPattern.op().temporal && !beliefPattern.op().commutative)
            if (!beliefPattern.op().var)
                concPattern = concPattern.replace(beliefPattern, Derivation.BeliefTerm);
        }

        byte taskPunc = 0;
        for (int i = 2; i < precon.length; i++) {

            Term p = precon[i];

            boolean negated = p.op() == NEG;
            boolean negationApplied = false; //safety check to make sure semantic of negation was applied by the handler
            if (negated)
                p = p.unneg();

            Term name = Functor.func(p);
            if (name == Bool.Null)
                throw new RuntimeException("invalid precondition: " + p);

            String pred = name.toString();
            Subterms args = Operator.args(p);
            int an = args.subs();
            Term X = an > 0 ? args.sub(0) : null;
            Term Y = an > 1 ? args.sub(1) : null;



        /*} else {
            throw new RuntimeException("invalid arguments");*/
            /*args = null;
            arg1 = arg2 = null;*/


            switch (pred) {


                case "neq":
                    neq(constraints, X, Y);
                    break;
                case "neqRoot":
                    neq(constraints, X, Y);
                    neqRoot(constraints, X, Y);
                    break;
                case "eqNeg":
                    neq(constraints, X, Y);
                    constraints.add(new NotEqualConstraint.EqualNegConstraint(X, Y));
                    break;


                case "neqRCom":
                    neqRoot(constraints, X, Y);
                    constraints.add(new NotEqualConstraint.NeqRootAndNotRecursiveSubtermOf(X, Y));
                    break;

                case "subOf": {

                    if (!(Y instanceof VarPattern)) {
                        if (negated)
                            throw new TODO();
                        match(X, new TermMatch.Contains(Y));

                    } else {
                        if (!negated)
                            neq(constraints, X, Y);

                        constraints.add(new SubOfConstraint(X, Y, Subterm).negIf(negated));

                        if (negated)
                            negationApplied = true;
                    }
                    break;
                }

                case "subPosOrNeg":
                    neq(constraints, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, Subterm, 0));
                    break;

                case "in":
                    neq(constraints, X, Y);
                    constraints.add(new SubOfConstraint(X, Y, Recursive));
                    break;

                case "conjParallel":
                    match(X, ConjParallel.the);
                    break;

                case "eventOf":
                case "eventOfNeg":
                    if (!negated) {
                        neq(constraints, X, Y);
                        match(X, new TermMatch.Is(CONJ));
                    }
                    constraints.add(new SubOfConstraint(X, Y, Event, pred.contains("Neg") ? -1 : +1).negIf(negated));
                    if (negated)  negationApplied = true;
                    break;

                case "eventFirstOf":
                case "eventFirstOfNeg":
                case "eventLastOf":
                case "eventLastOfNeg":
                    if (!negated) {
                        neq(constraints, X, Y);
                        match(X, new TermMatch.Is(CONJ));
                    }
                    constraints.add(new SubOfConstraint(X, Y, pred.contains("First") ? EventFirst : EventLast, pred.contains("Neg") ? -1 : +1).negIf(negated));
                    if (negated)  negationApplied = true;
                    break;

                case "eventOfPosOrNeg":
                    neq(constraints, X, Y);


                    match(X, new TermMatch.Is(CONJ));

                    constraints.add(new SubOfConstraint(X, Y,  Event, 0));
                    break;

                case "eventsOf":
                    neq(constraints, X, Y);


                    match(X, new TermMatch.Is(CONJ));

                    constraints.add(new SubOfConstraint(X, Y,  Events, 1));
                    break;

                case "eventCommon":


                    neq(constraints, X, Y);
                    match(X, new TermMatch.Is(CONJ));
                    match(Y, new TermMatch.Is(CONJ));
                    constraints.add(new CommonSubEventConstraint(X, Y));

                    break;


                case "subsMin":
                    match(X, new TermMatch.SubsMin((short) $.intValue(Y)));
                    break;

//                case "imaged": {
//                    //@Deprecated use subOf and --subOf for both / and \
//                    if (!taskPattern.containsRecursively(X) && !taskPattern.equals(X))
//                        throw new TODO("expected/tested occurrence in task concPattern ");
//
//                    final byte[] pp = Terms.constantPath(taskPattern, X);
//                    assert pp != null;
//                    pre.add(new Imaged(X, !negated, pp));
//                    if (negated) negationApplied = true;
//                    break;
//                }


                case "notSet":
                    /** deprecated soon */
                    matchNot(X, new TermMatch.Is(Op.Set));
                    break;


                case "is": {
                    match(X, new TermMatch.Is(Op.the($.unquote(Y))), !negated);
                    if (negated)
                        negationApplied = true;
                    break;
                }

                case "has": {
                    //hasAny
                    match(X, new TermMatch.Has(Op.the($.unquote(Y)), true), !negated);
                    if (negated)
                        negationApplied = true;
                    break;
                }


                case "task":
                    String XString = X.toString();
                    switch (XString) {


                        case "\"?\"":
                            pre.add(TaskPunctuation.Question);
                            taskPunc = '?';
                            break;


                        case "\"@\"":
                            pre.add(TaskPunctuation.Quest);
                            taskPunc = '@';
                            break;
                        case "\".\"":
                            pre.add(Belief);
                            taskPunc = '.';
                            break;
                        case "\"!\"":
                            pre.add(Goal);
                            taskPunc = '!';
                            break;

//                        case "\"*\"":
//                            pre.add(new TaskBeliefOp(PROD, true, false));
//                            break;
//                        case "\"&&\"":
//                            pre.add(new TaskBeliefOp(CONJ, true, false));
//                            break;


                        default:
                            throw new RuntimeException("Unknown task punctuation type: " + XString);
                    }
                    break;


                default:
                    throw new RuntimeException("unhandled postcondition: " + pred + " in " + this);

            }

            if (negationApplied != negated)
                throw new RuntimeException("unhandled negation: " + p);
        }

        Term beliefTruth = null, goalTruth = null;
        byte puncOverride = 0;
        Occurrify.TaskTimeMerge time = Occurrify.mergeDefault;

        Term[] modifiers = postcon != null && postcon.length > 1 ? Terms.sorted(postcon[1].arrayShared()) : Op.EmptyTermArray;
        boolean varIntro = false;
        for (Term m : modifiers) {
            if (m.op() != Op.INH)
                throw new RuntimeException("Unknown postcondition format: " + m);

            Term type = m.sub(1);
            Term which = m.sub(0);

            switch (type.toString()) {

                case "Also":
                    switch (which.toString()) {
                        case "VarIntro":
                            varIntro = true;
                            break;
                    }
                    break;
                case "Punctuation":
                    switch (which.toString()) {
                        case "Question":
                            puncOverride = QUESTION;
                            break;
                        case "Goal":
                            puncOverride = Op.GOAL;
                            break;
                        case "Belief":
                            puncOverride = Op.BELIEF;
                            break;
                        case "Quest":
                            puncOverride = QUEST;
                            break;

                        default:
                            throw new RuntimeException("unknown punctuation: " + which);
                    }
                    break;

                case "Time":
                    time = Occurrify.merge.get(which);
                    if (time == null)
                        throw new RuntimeException("unknown Time modifier:" + which);
                    break;


                case "Belief":
                    beliefTruth = which;
                    break;

                case "Goal":
                    goalTruth = which;
                    break;


                default:
                    throw new RuntimeException("Unhandled postcondition: " + type + ':' + which);
            }

        }

        Op to = taskPattern.op();
        boolean taskIsPatVar = to == Op.VAR_PATTERN;
        Op bo = beliefPattern.op();
        boolean belIsPatVar = bo == Op.VAR_PATTERN;


        TruthFunc beliefTruthOp = NALTruth.get(beliefTruth);
        if (beliefTruth != null && beliefTruthOp == null)
            throw new RuntimeException("unknown BeliefFunction: " + beliefTruth);

        TruthFunc goalTruthOp = NALTruth.get(goalTruth);
        if (goalTruth != null && goalTruthOp == null)
            throw new RuntimeException("unknown GoalFunction: " + goalTruth);



        {
            CommutativeConstantPreFilter.tryFilter(true, taskPattern, beliefPattern, pre);
            CommutativeConstantPreFilter.tryFilter(false, taskPattern, beliefPattern, pre);
        }

        {
            //subIfUnify prefilter
            Term concFunc = Functor.func(originalConcPattern);
            if (concFunc.equals(SubIfUnify.SubIfUnify)) {

                Subterms a = Operator.args(originalConcPattern);
                UnifyPreFilter.tryAdd(a.sub(1), a.sub(2),
                        taskPattern, beliefPattern,
                        a, pre);

            }
        }

        if (!taskIsPatVar) {
            pre.add(new TermMatchPred(new TermMatch.Is(to), true, true, TaskTerm));

            int ts = taskPattern.structure() & (~Op.VAR_PATTERN.bit);
            if (Integer.bitCount(ts) > 1) {
                //if there are additional bits that the structure can filter, include the hasAll predicate
                pre.add(new TermMatchPred<>(new TermMatch.Has(
                        ts, false /* all */, taskPattern.complexity()),
                        true, true, TaskTerm));
            }
        }

        if (!belIsPatVar) {
            pre.add(new TermMatchPred<>(new TermMatch.Is(bo), true, true, BeliefTerm));
            int bs = beliefPattern.structure() & (~Op.VAR_PATTERN.bit);
            if (Integer.bitCount(bs) > 1) {
                //if there are additional bits that the structure can filter, include the hasAll predicate
                pre.add(new TermMatchPred<>(new TermMatch.Has(
                        bs, false /* all */, beliefPattern.complexity()),
                        true, true, BeliefTerm));
            }
        }


        if (beliefTruthOp != null && !beliefTruthOp.single()) {
            if ((taskPunc == 0 || taskPunc == BELIEF)) {
                pre.add(new DoublePremiseRequired(true, false, false));
            } else if (puncOverride == BELIEF && (taskPunc == QUESTION || taskPunc == QUEST)) {
                pre.add(new DoublePremiseRequired(false, false, true));
            }
        }
        if (goalTruthOp != null && !goalTruthOp.single()) {
            if ((taskPunc == 0 || taskPunc == GOAL)) {
                pre.add(new DoublePremiseRequired(false, true, false));
            } else if (puncOverride == GOAL && (taskPunc == QUESTION || taskPunc == QUEST)) {
                pre.add(new DoublePremiseRequired(false, false, true));
            }
        }

        /*System.out.println( Long.toBinaryString(
                        pStructure) + " " + concPattern
        );*/


//        constraints.forEach(cc -> {
//            PREDICATE<Derivation> p = cc.preFilter(taskPattern, beliefPattern);
//            if (p != null) {
//                pre.add(p);
//            }
//        });
        List<RelationConstraint> mirrors = new FasterList(4);
        constraints.removeIf(cc -> {
            PREDICATE<Derivation> p = cc.preFilter(taskPattern, beliefPattern);
            if (p != null) {
                pre.add(p);
                //TODO also remove the opposite direction if relation?
                return true;
            }
            if (cc instanceof RelationConstraint) {
                RelationConstraint m = ((RelationConstraint) cc).mirror();
                if (m!=null)
                    mirrors.add(m);
            }
            return false;
        });
        constraints.addAll(mirrors);


        {


            assert (puncOverride > 0) || !taskPattern.equals(originalConcPattern) :
                    "punctuation not modified yet rule task equals pattern: " + this;


            if (taskPunc == 0) {
                //no override, determine automaticaly by presence of belief or truth

                boolean b = false, g = false;
                //for (PostCondition x : POST) {
                if (puncOverride != 0) {
                    throw new RuntimeException("puncOverride with no input punc specifier");
                } else {
                    b |= beliefTruth != null;
                    g |= goalTruth != null;
                }
                //}

                if (!b && !g) {
                    throw new RuntimeException(ruleSrc + "\n^\tcan not assume this applies only to questions");
                } else if (b && g) {
                    pre.add(TaskPunctuation.BeliefOrGoal);
                } else if (b) {
                    pre.add(Belief);
                } else /* if (g) */ {
                    pre.add(Goal);
                }
            }
        }

        this.truthify = Truthify.the(puncOverride, beliefTruthOp, goalTruthOp, time);
        this.time = time;
        this.varIntro = varIntro;
        this.taskPunc = taskPunc;

        this.postcons = postcon[0];
        this.puncOverride = puncOverride;
        this.beliefTruth = beliefTruth;
        this.goalTruth = goalTruth;
        this.CONSTRAINTS = Sets.immutable.of(theInterned(constraints));


        int rules = pre.size();
        PREDICATE[] PRE = pre.toArray(new PREDICATE[rules + 1 /* extra to be filled in later stage */]);
        //ArrayUtils.sort(PRE, 0, rules - 1, (x) -> -x.cost());


        Arrays.sort(PRE, 0, rules, sortByCostIncreasing);

        assert (PRE[PRE.length - 1] == null);
        assert rules <= 1 || (PRE[0].cost() <= PRE[rules - 2].cost()); //increasing cost
        this.PRE = PRE;

        //not working yet:
//        for (int i = 0, preLength = PRE.length; i < preLength; i++) {
//            PRE[i] = INDEX.intern(PRE[i]);
//        }
        this.termify = new Termify(this.concPattern = concPattern, truthify, time);

        this.constraintSet = CONSTRAINTS.toSet();

    }


    private static final Map<Term, UnifyConstraint> constra =
            new CustomConcurrentHashMap<>(STRONG, EQUALS, WEAK, EQUALS, 1024);

    private static UnifyConstraint intern(UnifyConstraint x) {
        UnifyConstraint y = constra.putIfAbsent(x.term(), x);
        return y != null ? y : x;
    }

    private static UnifyConstraint[] theInterned(MutableSet<UnifyConstraint> constraints) {
        if (constraints.isEmpty())
            return UnifyConstraint.EMPTY_UNIFY_CONSTRAINTS;

        UnifyConstraint[] mc = UnifyConstraint.the(constraints);
        for (int i = 0, mcLength = mc.length; i < mcLength; i++)
            mc[i] = intern(mc[i]);
        return mc;
    }

    protected PremiseRuleSource(PremiseRuleSource raw) {
        super((/*index.rule*/(raw.ref)));

        this.termify = raw.termify;
        this.PRE = raw.PRE.clone(); //because it gets modified when adding Branchify suffix
        this.CONSTRAINTS = null;
        this.source = raw.source;
        this.truthify = raw.truthify;
        this.constraints = raw.constraints;
        this.pre = raw.pre;
        this.time = raw.time;
        this.varIntro = raw.varIntro;
        this.taskPunc = raw.taskPunc;
        this.postcons = raw.postcons;
        this.puncOverride = raw.puncOverride;
        this.beliefTruth = raw.beliefTruth;
        this.goalTruth = raw.goalTruth;
        this.taskPattern = raw.taskPattern;
        this.beliefPattern = raw.beliefPattern;
        this.concPattern = raw.concPattern;
        this.constraintSet = raw.constraintSet;

    }

    public static PremiseRuleSource parse(String ruleSrc) throws Narsese.NarseseException {
        return new PremiseRuleSource(ruleSrc);
    }

    public static Stream<PremiseRuleSource> parse(String... rawRules) {
        return parse(Stream.of(rawRules));
    }

    public static Stream<PremiseRuleSource> parse(Stream<String> rawRules) {
        return rawRules.map(src -> {
            try {
                return parse(src);
            } catch (Exception e) {
                throw new RuntimeException("rule parse: " + e.getCause() + "\n\t" + src);
            }
        });

    }

    private static Subterms parseRuleComponents(String src) throws Narsese.NarseseException {


        String[] ab = ruleImpl.split(src);
        if (ab.length != 2)
            throw new Narsese.NarseseException("Rule component must have arity=2, separated by \"|-\": " + src);

        String A = '(' + ab[0].trim() + ')';
        Term a = Narsese.term(A, false);
        if (!(a instanceof Compound))
            throw new Narsese.NarseseException("Left rule component must be compound: " + src);

        String B = '(' + ab[1].trim() + ')';
        Term b = Narsese.term(B, false);
        if (!(b instanceof Compound))
            throw new Narsese.NarseseException("Right rule component must be compound: " + src);

        return new BiSubterm(a, b);
    }

    private void matchSuper(boolean taskOrBelief, TermMatch m, boolean trueOrFalse) {
        pre.add(new TermMatchPred(m, trueOrFalse, false, TaskOrBelief(taskOrBelief)));
    }


    private void match(boolean taskOrBelief, byte[] path, TermMatch m, boolean isOrIsnt) {
        if (path.length == 0) {
            //root
            pre.add(new TermMatchPred<>(m, isOrIsnt, true, TaskOrBelief(taskOrBelief)));
        } else {
            //subterm
            pre.add(new TermMatchPred.Subterm(path, m, isOrIsnt, TaskOrBelief(taskOrBelief)));
        }
    }


    private void match(Term x,
                       BiConsumer<byte[], byte[]> preDerivationExactFilter,
                       BiConsumer<Boolean, Boolean> preDerivationSuperFilter /* task,belief*/
    ) {

        //boolean isTask = taskPattern.equals(x);
        //boolean isBelief = beliefPattern.equals(x);
        byte[] pt = //(isTask || !taskPattern.ORrecurse(s -> s instanceof Ellipsislike)) ?
                Terms.pathConstant(taskPattern, x);
        byte[] pb = //(isBelief || !beliefPattern.ORrecurse(s -> s instanceof Ellipsislike)) ?
                Terms.pathConstant(beliefPattern, x);// : null;
        if (pt != null || pb != null) {



            if ((pt != null) && (pb !=null)) {
                //only need to test one. use shortest path
                if (pb.length < pt.length)
                    pt = null;
                else
                    pb = null;
            }

            preDerivationExactFilter.accept(pt, pb);
        } else {
            //assert(!isTask && !isBelief);
            //non-exact filter
            boolean inTask = taskPattern.containsRecursively(x);
            boolean inBelief = beliefPattern.containsRecursively(x);

            if (inTask && inBelief) {
                //only need to test one. use smallest volume
                if (beliefPattern.volume() < taskPattern.volume())
                    inTask = false;
                else
                    inBelief = false;
            }
            preDerivationSuperFilter.accept(inTask, inBelief);
        }
    }


    private void match(Term x, TermMatch m) {
        match(x, m, true);
    }

    private void matchNot(Term x, TermMatch m) {
        match(x, m, false);
    }

    private void match(Term x, TermMatch m, boolean trueOrFalse) {
        match(x, (pathInTask, pathInBelief) -> {


                    if (pathInTask != null)
                        match(true, pathInTask, m, trueOrFalse);
                    if (pathInBelief != null)
                        match(false, pathInBelief, m, trueOrFalse);

                }, (inTask, inBelief) -> {

                    if (inTask)
                        matchSuper(true, m, trueOrFalse);
                    if (inBelief)
                        matchSuper(false, m, trueOrFalse);

                    constraints.add(m.constraint(x, trueOrFalse));
                }
        );
    }


//    void subsMin(Term X, int min) {
//        if (taskPattern.equals(X)) {
//            pre.add(new SubsMin.SubsMinProto(true, min));
//        } else if (beliefPattern.equals(X)) {
//            pre.add(new SubsMin.SubsMinProto(false, min));
//        } else {
//            constraints.add(new SubsMin(X, min));
//        }
//
//        //TODO
//        //filter(x, (pt, pb)->
//
//    }



    private static void neq(Set<UnifyConstraint> constraints, Term x, Term y) {
        constraints.add(new NotEqualConstraint(x, y));
    }

    private static void neqRoot(Set<UnifyConstraint> constraints, Term x, Term y) {
        constraints.add(new NotEqualConstraint.NotEqualRootConstraint(x, y));
    }

    public static Term pp(byte[] b) {
        return b == null ? Op.EmptyProduct : $.p(b);
    }

//    static class MatchFilter extends AbstractPred<Derivation> {
//
//        private static final Atom MATCH_FILTER = (Atom) Atomic.the(MatchFilter.class.getSimpleName());
//        private final Term pattern;
//        private final boolean isTaskOrBelief;
//
//        protected MatchFilter(Term pattern, boolean isTaskOrBelief) {
//            super($.func(MATCH_FILTER, pattern, isTaskOrBelief ? Task : Belief));
//            this.isTaskOrBelief = isTaskOrBelief;
//            this.pattern = pattern;
//        }
//
//        @Override
//        public boolean test(Derivation derivation) {
//            return false;
//        }
//    }

    //    static final class SubOf extends AbstractPred<Derivation> {
//        private final Term y;
//
//        boolean task;
//        boolean belief;
//
//        SubOf(Term y, Term taskPattern, Term x, Term beliefPattern) {
//            super($.func("subOf", $.quote(y.toString())));
//            this.y = y;
//
//            task = taskPattern.containsRecursively(x);
//            belief = beliefPattern.containsRecursively(y);
//            assert task || belief;
//        }
//
//        @Override
//        public boolean test(Derivation preDerivation) {
//            if (task && !preDerivation.taskTerm.containsRecursively(y))
//                return false;
//            return !belief || preDerivation.beliefTerm.containsRecursively(y);
//        }
//
//        @Override
//        public float cost() {
//            return 0.5f;
//        }
//    }

//    static final class Imaged extends AbstractPred<Derivation> {
//        private final byte[] pp;
//        private final boolean isOrIsnt;
//
//        Imaged(Term x, boolean hasOrHasnt, byte[] pp) {
//            super($.func("imaged", x));
//            this.pp = pp;
//            this.isOrIsnt = hasOrHasnt;
//        }
//
//        @Override
//        public float cost() {
//            return 0.1f;
//        }
//
//        @Override
//        public boolean test(Derivation o) {
//            Term prod = o.taskTerm.subPath(pp);
//            return prod.op() == PROD && (isOrIsnt==Image.imaged(prod));
//        }
//    }


    private static Function<PreDerivation, Term> TaskOrBelief(boolean taskOrBelief) {
        return taskOrBelief ? TaskTerm : BeliefTerm;
    }

    final static Function<PreDerivation, Term> TaskTerm = new Function<>() {

        @Override
        public String toString() {
            return "taskTerm";
        }

        @Override
        public Term apply(PreDerivation d) {
            return d.taskTerm;
        }
    };

    final static Function<PreDerivation, Term> BeliefTerm = new Function<>() {

        @Override
        public String toString() {
            return "beliefTerm";
        }

        @Override
        public Term apply(PreDerivation d) {
            return d.beliefTerm;
        }
    };


    static class UppercaseAtomsToPatternVariables extends DirectTermTransform {

        final UnifiedMap<String, Term> map = new UnifiedMap<>(4);

        static final ImmutableSet<Atomic> reservedMetaInfoCategories = Sets.immutable.of(
                Atomic.the("Belief"),
                Atomic.the("Goal"),
                Atomic.the("Punctuation"),
                Atomic.the("Time")
        );


        @Override
        public Term transformAtomic(Atomic atomic) {
            if (atomic instanceof Atom) {
                if (!reservedMetaInfoCategories.contains(atomic)) {
                    String name = atomic.toString();
                    if (name.length() == 1 && Character.isUpperCase(name.charAt(0))) {
                        return map.computeIfAbsent(name, (n) -> $.varPattern(1 + map.size()));

                    }
                }
            }
            return atomic;
        }

    }

}





