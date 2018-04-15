package jcog.decide;

import jcog.Util;
import jcog.pri.Pri;
import jcog.pri.PriReference;
import jcog.pri.Prioritized;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;

import java.util.Arrays;
import java.util.Random;
import java.util.function.IntPredicate;

import static java.lang.Math.exp;

/**
 * roulette decision making
 */
public enum Roulette {
    ;

    public final static FloatFunction<? super PriReference> linearPri = (p) -> {
        return Math.max(p.priElseZero(), Prioritized.EPSILON);
    };

    final static FloatFunction<? super PriReference> softMaxPri = (p) -> {
        return (float) exp(p.priElseZero() * 3 /* / temperature */);
    };


    public static int decideRoulette(float[] x, Random rng) {
        return decideRoulette(x.length, (n) -> x[n], rng);
    }

    /**
     * https://en.wikipedia.org/wiki/Fitness_proportionate_selection
     * Returns the selected index based on the weights(probabilities)
     */
    public static int decideRoulette(int weightCount, IntToFloatFunction weight, Random rng) {
        // calculate the total weight
        assert (weightCount > 0);
        return decideRoulette(weightCount, weight, Util.sumIfPositive(weightCount, weight), rng);
    }


    /**
     * roulette selection on a softmax scale
     */
    public static int decideSoftmax(int count, IntToFloatFunction weight, float temperature, Random random) {
        return decideRoulette(count, (i) ->
                (float) exp(weight.valueOf(i) / temperature), random);
    }

//  /** roulette selection on a softmax scale */
//    public static void decideSoftmax(int count, IntToFloatFunction weight, float temperature, Random random, IntFunction<RouletteControl> each) {
//        decideRoulette(count, (i) ->
//                (float) exp(weight.valueOf(i) / temperature), random, each);
//    }

    /**
     * faster if the sum is already known
     */
    public static int decideRoulette(final int count, IntToFloatFunction weight, float weight_sum, Random rng) {

        int i = rng.nextInt(count); //random start location
        assert (i >= 0);
        if (weight_sum < Pri.EPSILON) {
            return i; //flat, choose one at random
        }

        float distance = rng.nextFloat() * weight_sum;
        boolean dir = rng.nextBoolean(); //randomize the direction

        while ((distance = distance - weight.valueOf(i)) > 0) {
            if (dir) {
                if (++i == count) i = 0;
            } else {
                if (--i == -1) i = count - 1;
            }
        }

        return i;
    }

    public static class RouletteUnique {
        /** weights of each choice */
        private final float[] w;
        private final Random rng;

        /** current weight sum */
        private float weightSum;
        private float remaining;

        int i;

        public static void run(float[] weights, IntPredicate select, Random rng) {
            switch (weights.length) {
                case 0: throw new UnsupportedOperationException();
                case 1: select.test(0); return;
                //TODO simple 2 case, with % probability of the ordering then try the order in sequence
                default: {
                    RouletteUnique r = new RouletteUnique(weights, rng);
                    while (r.next(select)) { }
                }
            }
        }

        public boolean next(IntPredicate select) {
            return select.test(next()) && --remaining > 0;
        }

        private int next() {

            assert(remaining > 0);

            float[] w = this.w;

            int count = w.length;

            if (remaining == 1) {
                //special case:
                //there is only one with non-zero weight remaining

                for (int x = 0; x < count; x++)
                    if (w[x] > 0)
                        return x;

                throw new RuntimeException();
            } else {

                float distance = rng.nextFloat() * weightSum;
                //boolean dir = rng.nextBoolean(); //randomize the direction

                int i = this.i;

                while ((distance = distance - w[i]) > Float.MIN_NORMAL) {
                    //if (dir) {
                    if (++i == count) i = 0;
                    //} else {
                    //  if (--i == -1) i = count - 1;
                    //}
                }


                weightSum -= w[i];
                w[i] = 0; //clear

                this.i = i;

                return i;
            }

        }

        RouletteUnique(float[] w, Random rng) {
            this.w = w;
            this.remaining = w.length;

            this.weightSum = Util.sum(w);

            if (weightSum > Float.MIN_VALUE){
                //weightSum + " is non-positive";
                Arrays.fill(w, 1);
                weightSum = w.length;
            }

            this.i = (this.rng = rng).nextInt(w.length); //random start position
        }

    }


    public static enum RouletteControl {
        STOP, CONTINUE, WEIGHTS_CHANGED
    }
}
