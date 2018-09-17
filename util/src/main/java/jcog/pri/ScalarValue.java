package jcog.pri;

import jcog.WTF;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.math.FloatSupplier;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

/**
 * general purpose value.  consumes and supplies 32-bit float numbers
 * supports certain numeric operations on it
 * various storage implementations are possible
 * as well as the operation implementations.
 *
 * see: NumericUtils.java (lucene)
 * */
public interface ScalarValue {

    /**
     * global minimum difference necessary to indicate a significant modification in budget float number components
     * TODO find if there is a better number
     */
    float EPSILON = 0.000002f;

    /** setter
     *  @return value after set
     * */
    float pri(float p);

    /** getter.  returns NaN to indicate deletion */
    float pri();


    default float pri(FloatSupplier update) {
        return pri(update.asFloat());
    }
    default float pri(FloatToFloatFunction update) {
        return pri(update.valueOf(pri()));
    }
    default float pri(FloatFloatToFloatFunction update, float x) {
        return pri(update.apply(pri(), x));
    }

    /** implementations can provide a faster non-value-returning strategy */
    default void priUpdate(FloatFloatToFloatFunction update, float x) {
        pri(update, x);
    }


    default float[] priDelta(FloatFloatToFloatFunction update, float x) {
        float[] beforeAfter = new float[2];
        beforeAfter[1] = pri((xx,yy)-> {
            beforeAfter[0] = xx;
            return update.apply(xx, yy);
        }, x);
        return beforeAfter;
    }

    /**
     * the result of this should be that pri() is not finite (ex: NaN)
     * returns false if already deleted (allowing overriding subclasses to know if they shold also delete)
     */
    default boolean delete() {
        float p = pri();
        if (p==p) {
            this.pri(Float.NaN);
            return true;
        }
        return false;
    }

//    @Deprecated private static float priElseZero(ScalarValue x) {
//        float p = x.pri();
//        return (p==p) ? p : 0;
//    }

    default float priMax(float _max) {
        //pri(Math.max(priElseZero(this), max));
        return pri((x, max) -> Math.max(max, (x!=x) ? 0 : x), _max);
    }

    default float priMin(float _min) {
        return pri((x, min) -> Math.min(min, (x!=x) ? 0 : x), _min);
    }


    FloatFloatToFloatFunction priAddUpdateFunction = (x, y) -> {
        if (x != x)
            //remains deleted by non-positive addend
            //undeleted by positive addend
            return y <= 0 ? Float.NaN : y;
        else
            return x + y;
    };

    /** doesnt return any value so implementations may be slightly faster than priAdd(x) */
    default void priAdd(float a) {
        priUpdate(priAddUpdateFunction, a);
    }

    default float priAddAndGet(float a) {
        return pri(priAddUpdateFunction, a);
    }

    default void priSub(float toSubtract) {
        assert (toSubtract >= 0) : "trying to subtract negative priority: " + toSubtract;

        priAdd(-toSubtract);
    }

    default float priMult(float _y) {
        return pri((x,y)-> (x == x) ? (x * y) : Float.NaN, _y);
    }

    /** y should be in domain (0...1) - for decaying result */
    default float priMult(float _y, float applyIfGreaterThan) {
        return pri((x,y)-> (x == x) ?
                ( x > applyIfGreaterThan ? Math.max(applyIfGreaterThan, (x * y)) : x)
                :
                Float.NaN,
        _y);
    }



    /**
     * assumes 1 max value (Plink not NLink)
     */
    default float priAddOverflow(float inc /* float upperLimit=1 */) {

        if (inc <= EPSILON)
            return 0;

        float[] beforeAfter = priDelta((x,y)-> ((x!=x) ? 0 : x) + y, inc);

        float after = beforeAfter[1];
        float before = beforeAfter[0];
        float delta = (before != before) ? after : (after - before);
        return Math.max(0, inc - delta); //should be >= 0
    }


    class PlainScalarValue implements ScalarValue {
        private float pri;

        @Override
        public float pri(float p) {
            return this.pri = p;
        }

        @Override
        public final float pri() {
            return pri;
        }
    }

    abstract class AtomicScalarValue implements ScalarValue {
        protected static final AtomicFloatFieldUpdater<AtomicScalarValue> FLOAT =
                new AtomicFloatFieldUpdater(AtomicScalarValue.class, "pri");

        private static final VarHandle INT;

        static {
            try {
                INT = MethodHandles.lookup().in(AtomicScalarValue.class)
                    .findVarHandle(AtomicScalarValue.class,"pri",int.class);
            } catch (Exception e) {
                throw new WTF(e);
            }
        }


        final static int iNaN = floatToIntBits(Float.NaN);

        private volatile int pri;


        public final float priElseZero() {
            int i = _pri();
            return i == iNaN ? 0 : intBitsToFloat(i);
        }

        @Override
        public boolean delete() {
            return ((int)INT.getAndSet(this, iNaN)) != iNaN;
            //if the above doesnt work, try converting with intToFloatBits( then do NaN test for equality etc
        }

        /** post-filter */
        abstract public float v(float x);

        /** allows NaN */
        private float _v(float x) {
            return x!=x ? Float.NaN : v(x);
        }

        private int _pri() {

            return (int) INT.getOpaque(this);
            //return (int) INT.get(this);
        }

        @Override
        public final float pri() {
            return intBitsToFloat( _pri() );
        }

        public boolean isDeleted() {
            return _pri() == iNaN;
        }



        /** set */
        @Override public float pri(float p) {
            INT.set(this, floatToIntBits(v(p)));
            return p;
        }

        /** update */
        @Override public final float pri(FloatSupplier update) {
            return FLOAT.updateAndGet(this, update);
        }

        /** update */
        @Override public final float pri(FloatToFloatFunction update) {
            return FLOAT.updateAndGet(this, update, this::_v);
        }

        /** update */
        @Override public final float pri(FloatFloatToFloatFunction update, float x) {
            return FLOAT.updateAndGet(this, x, update, this::_v);
        }

        @Override
        public final void priUpdate(FloatFloatToFloatFunction update, float x) {
            FLOAT.update(this, x, update, this::_v);
        }
    }
}
