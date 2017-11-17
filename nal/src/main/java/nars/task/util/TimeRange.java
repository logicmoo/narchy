package nars.task.util;

import jcog.tree.rtree.HyperRegion;

/**
 * only valid for comparison during rtree iteration
 */
public class TimeRange implements HyperRegion {

    long start = Long.MIN_VALUE, end = Long.MAX_VALUE;

    public TimeRange() {

    }

    public TimeRange(long s, long e) {
        set(s, e);
    }

    public TimeRange set(long s, long e) {
        this.start = s;
        this.end = e;
        return this;
    }

    @Override
    public HyperRegion mbr(HyperRegion r) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int dim() {
        return 3;
    }

    @Override
    public boolean intersects(HyperRegion x) {
        TaskRegion t = (TaskRegion)x;
        return start <= t.end() && end >= t.start();
    }


    @Override
    public boolean contains(HyperRegion x) {
        TaskRegion t = (TaskRegion)x;
        return start <= t.start() && end >= t.end();
    }

    @Override
    public double coord(boolean maxOrMin, int dimension) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float coordF(boolean maxOrMin, int dimension) {
        throw new UnsupportedOperationException();
    }
}
