package nars.concept.table;

import nars.Memory;
import nars.task.Task;
import org.jetbrains.annotations.Nullable;

/** task table used for storing Questions and Quests.
 *  simpler than Belief/Goal tables
 * */
public interface QuestionTable extends TaskTable {

    /**
     * attempt to insert a task.
     *
     * @return: the input task itself, it it was added to the table
     * an existing equivalent task if this was a duplicate
     */
    @Nullable
    Task add(Task t, BeliefTable answers, Memory m);

    void setCapacity(int newCapacity);

    /**
     * @return null if no duplicate was discovered, or the first Task that matched if one was
     */
    @Nullable
    Task get(Task t);

    /** called when a new answer appears */
    void answer(Task result);

//    {
//        for (Task a : this) {
//            if (e.test(a, t))
//                return a;
//        }
//        return null;
//    }
}
