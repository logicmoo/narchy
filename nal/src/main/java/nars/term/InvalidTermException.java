package nars.term;

import nars.Op;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static nars.nal.Tense.DTERNAL;

/**
 * Created by me on 2/26/16.
 */
public final class InvalidTermException extends RuntimeException {

    @NotNull private final Op op;
    private final int dt;
    @NotNull private final Term[] args;
    @NotNull private final String reason;



    public InvalidTermException(Op op, Term[] args, String reason) {
        this(op, DTERNAL, args, reason);
    }

    public InvalidTermException(Op op, int dt, Term[] args, String reason) {
        this.op = op;
        this.dt = dt;
        this.args = args;
        this.reason = reason;
    }

    @NotNull
    @Override
    public String getMessage() {
        return toString();
    }

    @NotNull
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + reason + " {" +
                op +
                ", dt=" + dt +
                ", args=" + Arrays.toString(args) +
                '}';
    }
}
