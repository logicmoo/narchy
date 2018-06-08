package nars.task;

import nars.NAR;
import nars.Task;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.truth.Truth;

import java.util.Collection;

/** task which bypasses the evaluation procedure on input.
 *  this is faster but also necessary when
 *  something is specified in the task that evaluation
 *  otherwise would un-do.
 */
public class UnevaluatedTask extends NALTask {

    public UnevaluatedTask(Term t, byte punct, Truth truth, long creation, long start, long end, long[] stamp) {
        super(t, punct, truth, creation, start, end,
                stamp /* TODO use an implementation which doenst need an array for this */);
    }

    public UnevaluatedTask(Term c, Task xx, Truth t) throws InvalidTaskException {
        super(c, xx.punc(), t, xx.creation(), xx.start(), xx.end(), xx.stamp());
    }

    @Override
    public void preProcess(NAR n, Term y, Collection< ITask > queue) {
        queue.add(inputStrategy(this)); //direct
    }
}
