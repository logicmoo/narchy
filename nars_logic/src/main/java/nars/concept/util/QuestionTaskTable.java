package nars.concept.util;

import nars.Memory;
import nars.budget.BudgetMerge;
import nars.task.Task;

import java.util.function.BiPredicate;

/** task table used for storing Questions and Quests.
 *  simpler than Belief/Goal tables
 * */
public interface QuestionTaskTable extends TaskTable {

    /**
     * attempt to insert a task.
     *
     * @return: the input task itself, it it was added to the table
     * an existing equivalent task if this was a duplicate
     */

    Task add(Task t, BiPredicate<Task, Task> equality, BudgetMerge duplicateMerge, Memory m);
}
