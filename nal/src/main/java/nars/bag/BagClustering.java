package nars.bag;

import jcog.bag.impl.ArrayBag;
import jcog.learn.gng.NeuralGasNet;
import jcog.learn.gng.impl.Centroid;
import jcog.pri.VLink;
import jcog.pri.op.PriMerge;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * clusterjunctioning
 * TODO abstract into general purpose "Cluster of Bags" class
 */
public class BagClustering<X> {

    public final ArrayBag<X, VLink<X>> bag;

    final Dimensionalize<X> model;

    public final Clusters net;

    final AtomicBoolean busy = new AtomicBoolean(false);


    /**
     * TODO allow dynamic change
     */
    private final short clusters;

    public BagClustering(Dimensionalize<X> model, int centroids, int initialCap) {

        this.clusters = (short) centroids;

        this.model = model;

        this.net = new Clusters(model.dims, centroids);



        PriMerge merge = PriMerge.avg;
        this.bag = new ArrayBag<>(merge, new ConcurrentHashMap<>(initialCap)) {


            @Nullable
            @Override
            public X key(@NotNull VLink<X> x) {
                return x.id;
            }

//            @Override
//            public void onAdd(@NotNull VLink<X> x) {
////                synchronized (bag) {
////                    learn(x);
////                }
//            }
//
//            @Override
//            public void onRemove(@NotNull VLink<X> value) {
//                //value.delete();
//            }


        };
        bag.setCapacity(initialCap);


    }


    public Centroid newCentroid(int i, int dims) {
        return new BagCentroid(i, dims);
    }

    public void print() {
        print(System.out);
    }

    public void print(PrintStream out) {
        forEachCluster(c -> {
            out.println(c.toString());
            stream(c.id).forEach(i -> {
                out.print("\t");
                out.println(i);
            });
            out.println();
        });
        out.println(net.edges);
    }

    public void forEachCluster(Consumer<Centroid> c) {
        for (Centroid b : net.centroids) {
            c.accept(b);
        }
    }


//    protected class MyForget extends PriForget<VLink<X>> {
//
//        public MyForget(float priFactor) {
//            super(priFactor);
//        }
//
//        @Override
//        public void accept(VLink<X> b) {
//            super.accept(b);
//            learn(b);
//        }
//    }

    public int size() {
        return bag.size();
    }


    /**
     * how to interpret the bag items as vector space data
     */
    abstract public static class Dimensionalize<X> {

        public final int dims;

        protected Dimensionalize(int dims) {
            this.dims = dims;
        }

        abstract public void coord(X t, double[] d);

    }


    /**
     * TODO make this an abstract class or interface for pluggable Clustering implementations. gasnet is just the first for now
     */
    public class Clusters extends NeuralGasNet<Centroid> {

        public Clusters(int dimension, int maxNodes) {
            this(dimension, maxNodes, null);
        }

        public Clusters(int dimension, int maxNodes, Centroid.DistanceFunction distanceSq) {
            super(dimension, maxNodes, distanceSq);
        }

        @NotNull
        @Override
        public Centroid newCentroid(int i, int dims) {
            return new BagCentroid(i, dims);
        }


    }


    public boolean commit(int iterations) {

        if (busy.compareAndSet(false, true)) {

            try {
                synchronized (bag.items) {

                    int s = bag.size();
                    if (s == 0)
                        return false;

                    //                net.compact();
                    int cc = bag.capacity();
                    net.setLambda(1 + cc/2);
                    net.setWinnerUpdateRate(10f / cc, 5f / cc);

                    bag.commit(); //first, apply bag forgetting

                    for (int i = 0; i < iterations; i++) {
                        bag.forEach(this::learn);
                    }
                }
            } catch (Throwable t) {
                throw new RuntimeException(t);
            } finally {
                 busy.set(false);

            }

            return true;
        } else {
            return false;
        }

    }

    private void learn(VLink<X> x) {
        x.centroid = net.put(x.coord).id;
    }

    public void clear() {
        synchronized (bag.items) {
            bag.clear();
            net.clear();
        }
    }

    public void put(X x, float pri) {
        bag.putAsync(new VLink<>(x, pri, new double[model.dims], model::coord)); //TODO defer vectorization until after accepted
    }

    public void remove(X x) {
        bag.remove(x);
    }

    /**
     * returns NaN if either or both of the items are not present
     */
    public double distance(X x, X y) {
        assert (!x.equals(y));
        @Nullable VLink<X> xx = bag.get(x);
        if (xx != null && xx.centroid >= 0) {
            @Nullable VLink<X> yy = bag.get(y);
            if (yy != null && yy.centroid >= 0) {
                return Math.sqrt( net.distanceSq.distance(xx.coord, yy.coord) );
            }
        }
        return Double.POSITIVE_INFINITY;
    }

    /**
     * TODO this is O(N) not great
     */
    public Stream<VLink<X>> stream(int centroid) {
        return bag.stream().filter(y -> y.centroid == centroid);
    }

    public Stream<VLink<X>> neighbors(X x) {
        @Nullable VLink<X> link = bag.get(x);
        if (link != null) {
            int centroid = link.centroid;
            if (centroid >= 0) {
                Centroid[] nodes = net.centroids;
                if (centroid < nodes.length) //in case of resize
                    return stream(centroid)
                            .filter(y -> !y.equals(x))
                            ;
            }
        }
        return Stream.empty();
    }

    public class BagCentroid extends Centroid {
        public BagCentroid(int i, int dims) {
            super(i, dims);
        }
    }


}
