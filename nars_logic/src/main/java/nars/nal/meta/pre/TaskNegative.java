package nars.nal.meta.pre;

import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PostCondition;
import nars.nal.meta.PremiseMatch;
import nars.task.Task;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 8/15/15.
 */
public final class TaskNegative extends AtomicBooleanCondition<PremiseMatch> {

    public static final TaskNegative the = new TaskNegative();

    private TaskNegative() {
        super();
    }

    @Override
    public boolean booleanValueOf(@NotNull PremiseMatch m) {
        Task task = m.currentPremise.task();
        return (task.isJudgmentOrGoal() && task.freq() < PostCondition.HALF);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }


}
