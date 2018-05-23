package jcog.optimize;

import com.google.common.base.Joiner;
import jcog.io.arff.ARFF;
import jcog.list.FasterList;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.MultiDirectionalSimplex;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.SimplexOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.util.MathArrays;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.DoubleObjectPair;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * Optimization solver wrapper w/ lambdas
 * instance of an experiment
 */
public class Optimize<X> {

    /**
     * if a tweak's 'inc' (increment) is not provided,
     * use the known max/min range divided by this value as 'inc'
     *
     * this controls the exploration rate
     */
    static final float autoInc_default = 5f;

    final List<Tweak<X, ?>> tweaks;
    final Supplier<X> subject;

    private final static Logger logger = LoggerFactory.getLogger(Optimize.class);

    protected Optimize(Supplier<X> subject, Tweaks<X> t) {
        this(subject, t, Map.of("autoInc", autoInc_default));
    }

    protected Optimize(Supplier<X> subject, Tweaks<X> t, Map<String, Float> hints) {
        Pair<List<Tweak<X, ?>>, SortedSet<String>> uu = t.get(hints);
        List<Tweak<X, ?>> ready = uu.getOne();
        SortedSet<String> unknown = uu.getTwo();
        if (ready.isEmpty()) {
            throw new RuntimeException("tweaks not ready:\n" + Joiner.on('\n').join(unknown));
        }


        if (!unknown.isEmpty()) {
            for (String w : unknown) {
                logger.warn("unknown: {}", w);
            }
        }

        this.subject = subject;
        this.tweaks = ready;
    }


    /**
     * TODO support evaluator that collects data during execution, and return score as one data field
     *
     * @param data
     * @param maxIterations
     * @param repeats
     * @param eval
     * @param exe
     * @return
     */
    public Result<X> run(final ARFF data, int maxIterations, int repeats,
                         //FloatFunction<Supplier<X>> eval,
                         Optimizing.Optimal<X,?>[] eval,
                         ExecutorService exe) {


        assert (repeats >= 1);


        final int dim = tweaks.size();

        double[] mid = new double[dim];
        //double[] sigma = new double[n];
        double[] min = new double[dim];
        double[] max = new double[dim];
        double[] inc = new double[dim];
//        double[] range = new double[dim];

        X example = subject.get();
        int i = 0;
        for (Tweak w : tweaks) {
            TweakFloat s = (TweakFloat) w;

            //initial guess: get from sample, otherwise midpoint of min/max range
            Object guess = s.get(example);
            mid[i] = guess != null ? ((float) guess) : ((s.getMax() + s.getMin()) / 2f);

            min[i] = (s.getMin());
            max[i] = (s.getMax());
            inc[i] = s.getInc();
//            range[i] = max[i] - min[i];
            //sigma[i] = Math.abs(max[i] - min[i]) * 0.75f; //(s.getInc());
            i++;
        }

        //TODO add the seeks to the experiment vector


        FasterList<DoubleObjectPair<double[]>> experiments = new FasterList<>(maxIterations);


        final double[] maxScore = {Double.NEGATIVE_INFINITY};

        ObjectiveFunction func = new ObjectiveFunction(point -> {

            double score;
            try {

                double sum = 0;

                Supplier<X> x = () -> subject(point);

                List<Map<String,Object>> exp = new FasterList(repeats);

                CountDownLatch c = new CountDownLatch(repeats);
                List<Future<Map<String,Object>>> each = new FasterList(repeats);
                for (int r = 0; r < repeats; r++) {
                    each.add( exe.submit(()->{
                        try {
                            X y = x.get();

                            float subScore = 0;
                            Map<String,Object> e = new HashMap();
                            for (Optimizing.Optimal<X,?> o : eval) {
                                ObjectFloatPair<?> xy = o.eval(y);
                                e.put(o.id, xy.getOne());
                                subScore += xy.getTwo();
                            }

                            e.put("_", subScore); //HACK

                            return e;
                        } finally {
                            c.countDown();
                        }
                    }) );
                }

                c.await();

                for (int r = 0; r < repeats; r++) {
                    Map<String, Object> y = each.get(r).get();
                    exp.add(y);

                    sum += (Float)y.get("_");
                }

                //TODO interplate and store the detected features

                score = sum / repeats;

            } catch (Exception e) {
                logger.error("{} {} {}", this, point, e);
                score = Float.NEGATIVE_INFINITY;
            }


//            if (trace)
//                csv.out(ArrayUtils.add(point, 0, score));
            MutableList p = new FasterList(tweaks.size()+1).with(score);
            for (double x : point)
                p.add(x);
            data.add( p.toImmutable() );

            maxScore[0] = Math.max(maxScore[0], score);
//            System.out.println(
//                    n4(score) + " / " + n4(maxScore[0]) + "\t" + n4(point)
//            );

            experiments.add(pair(score, point));
            experimentIteration(point, score);
            return score;
        });


//        if (trace)
//            csv = new CSVOutput(System.out, Stream.concat(
//                    Stream.of("score"), tweaks.stream().map(t -> t.id)
//            ).toArray(String[]::new));


        experimentStart();

        try {
            solve(dim, func, mid, min, max, inc, maxIterations);
        } catch (Throwable t) {
            logger.info("solve {} {}", func, t);
        }

        return new Result<>(data, experiments, tweaks);
    }

    void solve(int dim, ObjectiveFunction func, double[] mid, double[] min, double[] max, double[] inc, int maxIterations) {
        if (dim == 1) {
            //use a solver capable of 1 dim
            new SimplexOptimizer(1e-10, 1e-30).optimize(
                    new MaxEval(maxIterations),
                    func,
                    GoalType.MAXIMIZE,
                    new InitialGuess(mid),
                    //new NelderMeadSimplex(inc)
                    new MultiDirectionalSimplex(inc)
            );
        } else {

            int popSize =
                    //4 + 3 ln(n)
                    (int) Math.ceil(4 + 3 * Math.log(tweaks.size()));


            double[] sigma = MathArrays.scale(1f, inc);

            MyCMAESOptimizer m = new MyCMAESOptimizer(maxIterations, Double.NaN,
                    true, 0,
                    1, new MersenneTwister(System.nanoTime()),
                    true, null, popSize, sigma);
            m.optimize(
                    func,
                    GoalType.MAXIMIZE,
                    new MaxEval(maxIterations),
                    new SimpleBounds(min, max),
                    new InitialGuess(mid)
            );
            //m.print(System.out);

//            final int numIterpolationPoints = 3 * dim; //2 * dim + 1 + 1;
//            new BOBYQAOptimizer(numIterpolationPoints,
//                    dim * 2.0,
//                    1.0E-4D /* ? */).optimize(
//                    MaxEval.unlimited(), //new MaxEval(maxIterations),
//                    new MaxIter(maxIterations),
//                    func,
//                    GoalType.MAXIMIZE,
//                    new SimpleBounds(min, max),
//                    new InitialGuess(mid));
        }

    }

    /**
     * called before experiment starts
     */
    protected void experimentStart() {
    }

    /**
     * called after each iteration
     */
    protected void experimentIteration(double[] point, double score) {
    }

    /**
     * builds an experiment subject (input)
     * TODO handle non-numeric point entries
     */
    private X subject(double[] point) {
        X x = subject.get();

        for (int i = 0, dim = point.length; i < dim; i++) {
            point[i] = ((Tweak<X, Float>) tweaks.get(i)).set(x, (float) point[i]);
        }

        return x;
    }

}

//public class MeshOptimize<X> extends Optimize<X> {
//
//    /** experiment id's */
//    private static final AtomicInteger serial = new AtomicInteger();
//
//    /** should get serialized compactly though by msgpack */
//    private final MeshMap<Integer, List<Float>> m;
//
//    public MeshOptimize(String id, Supplier<X> subject, Tweaks<X> tweaks) {
//        super(subject, tweaks);
//
//        m = MeshMap.get(id, (k,v)->{
//            System.out.println("optimize recv: " + v);
//        });
//
//    }
//
//    @Override
//    protected void experimentIteration(double[] point, double score) {
//        super.experimentIteration(point, score);
//        m.put(serial.incrementAndGet(), Floats.asList(ArrayUtils.add(Util.toFloat(point), (float)score)));
//    }
//}
