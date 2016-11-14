package nars.util.version;

import nars.util.data.DequePool;
import nars.util.list.FasterList;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 5/9/16.
 */
public final class FasterListDequePool extends DequePool<FasterList> {
    private final int stackLimit;

    public FasterListDequePool(int capacity, int stackLimit) {
        super(capacity);
        this.stackLimit = stackLimit;
    }

    @NotNull
    @Override
    public FasterList create() {
        return new FasterList(stackLimit);
    }
}
