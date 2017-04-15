package nars.derive.meta.op;

import nars.Op;
import nars.derive.meta.AtomicPredicate;
import nars.premise.Derivation;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 8/27/15.
 */
final public class TaskPunctuation extends AtomicPredicate<Derivation> {

    public final byte punc;
    public final String id;


    public static final TaskPunctuation Belief = new TaskPunctuation('.');
    public static final TaskPunctuation Goal = new TaskPunctuation('!');

    public static final AtomicPredicate<Derivation> QuestionOrQuest = new AtomicPredicate<>() {
        @Override
        public boolean test(@NotNull Derivation o) {
            byte c = o.taskPunct;
            return c == Op.QUESTION || c == Op.QUEST;
        }

        @Override
        public String toString() {
            return "task:\"?@\"";
        }
    };
    public static final AtomicPredicate<Derivation> Question = new AtomicPredicate<>() {
        @Override
        public boolean test(@NotNull Derivation o) {
            return o.taskPunct == Op.QUESTION;
        }

        @Override
        public String toString() {
            return "task:\"?\"";
        }
    };
    public static final AtomicPredicate<Derivation> Quest = new AtomicPredicate<>() {
        @Override
        public boolean test(@NotNull Derivation o) {
            return o.taskPunct == Op.QUEST;
        }

        @Override
        public String toString() {
            return "task:\"@\"";
        }
    };

    //    /** only belief, not goal or question */
//    public static final AtomicBoolCondition NotGoal = new AtomicBoolCondition()  {
//        @Override public boolean booleanValueOf(@NotNull PremiseEval o) {
//            return (o.premise.task().punc() != Symbols.GOAL);
//        }
//        @Override public String toString() { return "task:\".\""; }
//    };
    public static final AtomicPredicate<Derivation> NotQuestion = new AtomicPredicate<>() {
        @Override
        public boolean test(@NotNull Derivation o) {
            byte p = o.taskPunct;
            return (p != Op.QUESTION && p != Op.QUEST);
        }

        @Override
        public String toString() {
            return "task:\".!\"";
        }
    };
//    public static final AtomicBoolCondition NotBelief = new AtomicBoolCondition()  {
//        @Override public boolean booleanValueOf(@NotNull PremiseEval o) {
//            return (o.premise.task().punc() != Symbols.BELIEF);
//        }
//        @Override public String toString() { return "task:\"!?@\""; }
//    };


    TaskPunctuation(char p) {
        this(p, "task:\"" + p + '\"');
    }

    TaskPunctuation(char p, String id) {
        this.punc = (byte) p;
        this.id = id;
    }

    @NotNull
    @Override
    public final String toString() {
        return id;
    }

    @Override
    public final boolean test(@NotNull Derivation m) {
        return m.taskPunct == punc;
    }

    //    @NotNull
//    @Override
//    public String toJavaConditionString() {
//        return "'" + punc + "' == p.getTask().getPunctuation()";
//    }
}
