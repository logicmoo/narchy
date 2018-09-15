package toxi.geom;

import jcog.tree.rtree.rect.RectFloat2D;

import java.util.function.Consumer;

public interface SpatialIndex<T> {

    void clear();

    boolean index(T p);

    boolean isIndexed(T item);

//    public List<T> itemsWithinRadius(T p, float radius, List<T> results);

    void itemsWithinRadius(Vec2D p, float radius, Consumer<T> results);

    boolean reindex(T p, Consumer<T> update);

    int size();

    boolean unindex(T p);

    /** resize bounds of the index */
    default void bounds(RectFloat2D bounds) {

    }
}