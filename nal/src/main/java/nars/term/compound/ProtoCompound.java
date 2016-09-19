package nars.term.compound;

import nars.Op;
import nars.term.Term;
import nars.util.Util;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static nars.time.Tense.DTERNAL;

/**
 * compound builder
 */
public interface ProtoCompound {

    Op op();

    Term[] terms();

    int dt();


    static ProtoCompound the(Op o, Term[] args) {
        return the(o, DTERNAL, args);
    }

    static ProtoCompound the(Op o, int dt, Term[] args) {
        return new RawProtoCompound(o, dt, args);
    }


    class RawProtoCompound implements ProtoCompound {


        @NotNull private final Op op;
        @NotNull private final Term[] args;
        @NotNull private final int dt;

        private final int hash;

        protected RawProtoCompound(@NotNull Op op, int dt, @NotNull Term... t) {
            this.op = op;
            this.dt = dt;
            this.args = t;

            this.hash = Util.hashCombine(Util.hashCombine(t), op.ordinal(), dt);
        }

        @Override
        public String toString() {
            return "RawProtoCompound:" +
                    op +
                    '(' + dt +
                    ", " + Arrays.toString(args) +
                    ')';
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RawProtoCompound)) return false;

            RawProtoCompound that = (RawProtoCompound) o;

            if (dt != that.dt) return false;
            if (op != that.op) return false;

            return Util.equals(args, that.args);
        }

        @Override
        public final Op op() {
            return op;
        }

        @Override
        public final Term[] terms() {
            return args;
        }

        @Override
        public final int dt() {
            return dt;
        }
    }







}
