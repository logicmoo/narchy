package nars.budget.forget;

import nars.NAR;
import nars.bag.ArrayBLink;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/** processes a BLink, usually affecting its budget somehow */
public interface BudgetForget extends Consumer<ArrayBLink> {
    /**
     * called each frame to update parameters
     */
    void update(@NotNull NAR nar);

    /**
     * called each frame to increment the cycle-advanced time within the frame
     * @param subFrame time offset to be applied to the last frame time value , 0 <= x < 1
     */
    void cycle(float subFrame);

}
