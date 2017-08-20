/*
 * Copyright (c) 2003-2005 The BISON Project
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License version 2 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 *
 */

package jcog.data.graph;

import jcog.TriConsumer;
import jcog.util.TriFunction;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.Supplier;

/**
 * Implements a graph which uses the neighbour list representation.
 * No multiple edges are allowed. The implementation also supports the
 * growing of the graph. This is very useful when the number of nodes is
 * not known in advance or when we construct a graph reading a file.
 */
public class AdjGraph<V, E> implements Graph<V, E>, java.io.Serializable {

// =================== private fields ============================
// ===============================================================

    int serial = 0;

    public V node(int i) {
        return antinodes.get(i).v;
    }

    /**
     * Contains sets of node indexes. If "nodes" is not null, indices are
     * defined by "nodes", otherwise they correspond to 0,1,...
     */
    public static class Node<V> {
        public final V v;
        public final IntHashSet e; //shouldnt be final otherwise i'd extend it :(
        public final int id;

        public Node(V v, int id) {
            super();
            this.v = v;
            this.id = id;
            this.e = new IntHashSet(0);
        }

        @Override
        public String toString() {
            return v.toString();
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || v.equals(obj) || (obj instanceof Node && v.equals(((Node) obj).v));
        }

        @Override
        public int hashCode() {
            return v.hashCode();
        }
    }


    /**
     * Contains the indices of the nodes. The vector "nodes" contains this
     * information implicitly but this way we can find indexes in log time at
     * the cost of memory (node that the edge lists typically use much more memory
     * than this anyway). Note that the nodes vector is still necessary to
     * provide constant access to nodes based on indexes.
     */
    private final ObjectIntHashMap<Node<V>> nodes;
    private final IntObjectHashMap<Node<V>> antinodes;


    /**
     * edge values indexed by their unique 64-bit id formed from the id's of the src (32bit int) and target (32bit int)
     */
    private final LongObjectHashMap<E> edges;


    /**
     * Indicates if the graph is directed.
     */
    private final boolean directed;

// =================== public constructors ======================
// ===============================================================

    /**
     * Constructs an empty graph. That is, the graph has zero nodes, but any
     * number of nodes and edges can be added later.
     *
     * @param directed if true the graph will be directed
     */
    public AdjGraph(boolean directed) {
        this(directed, 0, 0);
    }

    public AdjGraph(boolean directed, int nodeCap, int edgeCap) {
        edges = new LongObjectHashMap<>(edgeCap);
        nodes = new ObjectIntHashMap<>(nodeCap);
        antinodes = new IntObjectHashMap(nodeCap);
        this.directed = directed;
    }


// =================== public methods =============================
// ================================================================

    /**
     * If the given object is not associated with a node yet, adds a new
     * node. Returns the index of the node. If the graph was constructed to have
     * a specific size, it is not possible to add nodes and therefore calling
     * this method will throw an exception.
     *
     * @throws NullPointerException if the size was specified at construction time.
     */
    public int addNode(V o) {

        int index = nodes.getIfAbsent(o, -1);
        if (index == -1) {
            int id = serial++;
            Node n = new Node(o, id);
            nodes.put(n, id);
            antinodes.put(id, n);
            return id;
        }

        return index;
    }


// =================== graph implementations ======================
// ================================================================


    @Override
    public boolean setEdge(int i, int j) {
        throw new UnsupportedOperationException();
    }

    public boolean setEdge(V i, V j, E value) {
        int ii = nodes.getIfAbsent(i, -1);
        if (ii != -1) {
            int jj = nodes.getIfAbsent(j, -1);
            if (jj != -1) {
                return setEdge(ii, jj, value);
            }
        }
        return false;
    }

    @Override
    public boolean setEdge(int i, int j, E value) {
        boolean ret = antinodes.get(i).e.add(j);
        if (ret && !directed) antinodes.get(j).e.add(i);
        edges.put(eid(i, j), value);
        return ret;
    }

    /**
     * Returns null always.
     */
    @Override
    @Nullable
    public E edge(int i, int j) {
        return edges.get(eid(i, j));
    }

    @Nullable
    public E edge(V i, V j) {
        int ii = nodes.getIfAbsent(i, -1);
        if (ii != -1) {
            int jj = nodes.getIfAbsent(j, -1);
            if (jj != -1) {
                return edge(ii, jj);
            }
        }
        return null;
    }


    // ---------------------------------------------------------------


    @Override
    public String toString() {
        return antinodes.makeString() + " x " + edges.makeString();
    }

    @Override
    public boolean removeEdge(int i, int j) {
        boolean ret = antinodes.get(i).e.remove(j);
        if (ret && !directed) {
            boolean ret2 = antinodes.get(j).e.remove(i);
            assert (ret2);
        }
        boolean ret3 = edges.remove(eid(i, j)) != null;
        assert (ret3);
        return ret;
    }

    public final long eid(int i, int j) {
        if (!directed) {
            if (j < i) {
                int x = j;
                j = i;
                i = x;
            }
        }
        long x = i;
        x <<= 32;
        x |= j;
        return x;
    }

// ---------------------------------------------------------------

    @Override
    public boolean isEdge(int i, int j) {
        return antinodes.get(i).e.contains(j);
    }

// ---------------------------------------------------------------

    @Override
    public IntHashSet neighbors(int i) {
        return antinodes.get(i).e;
    }

// ---------------------------------------------------------------

    /**
     * If the graph was gradually grown using {@link #addNode}, returns the
     * object associated with the node, otherwise null
     */
    @Override
    public V vertex(int v) {
        return nodes == null ? null : antinodes.get(v).v;
    }

// ---------------------------------------------------------------


// ---------------------------------------------------------------

    @Override
    public int size() {
        return nodes.size();
    }

// --------------------------------------------------------------------

    @Override
    public boolean directed() {
        return directed;
    }

// --------------------------------------------------------------------

    @Override
    public int degree(int i) {
        return antinodes.get(i).e.size();
    }

    public int nodeCount() {
        return nodes.size();
    }

    public int edgeCount() {
        return edges.size();
    }

    public E edge(V s, V p, E ifMissing) {
        @Nullable E existing = edge(s, p);
        return existing != null ? existing : ifMissing;
    }

    public E edge(V s, V p, Supplier<E> ifMissing) {
        @Nullable E existing = edge(s, p);
        return existing != null ? existing : ifMissing.get();
    }

    public boolean removeEdge(V i, V j) {
        int ii = nodes.getIfAbsent(i, -1);
        if (ii != -1) {
            int jj = nodes.getIfAbsent(j, -1);
            if (jj != -1) {
                return removeEdge(ii, jj);
            }
        }
        return false;
    }

    public void eachNode(TriConsumer<Node<V>, Node<V>, E> edge) {
        edges.forEachKeyValue((eid, ev) -> {
            int a = (int) (eid >> 32);
            int b = (int) (eid & 0xffffffff);
            Node<V> na = antinodes.get(a);
            Node<V> nb = antinodes.get(b);
            if (na == null || nb == null) {
                throw new RuntimeException("oob");
            }
            edge.accept(na, nb, ev);
        });

    }

    /**
     * returns true if modified
     */
    public void each(TriConsumer<V, V, E> edge) {
        eachNode((an, bn, e) -> {
            edge.accept(an.v, bn.v, e);
        });
    }


    public AdjGraph<V, E> compact() {
        return compact((x, y, z) -> z);
    }

    public AdjGraph<V, E> compact(TriFunction<V, V, E, E> retain) {
        AdjGraph g = new AdjGraph(directed);
        each((a, b, e) -> {
            //TODO combine the addNode and setEdge into one call
            e = retain.apply(a, b, e);
            if (e != null) {
                int aa = g.addNode(a);
                int bb = g.addNode(b);
                g.setEdge(aa, bb, e);
            }
        });
        return g;
    }

    /**
     * Saves the given graph to
     * the given stream in GML format.
     */
    public void writeGML(PrintStream out) {

        out.println("graph [ directed " + (directed ? "1" : "0"));

        for (Node<V> k : nodes.keySet())
            out.println("node [ id " + k.id + " label \"" + k.v + "\" ]");

        eachNode((a, b, e) -> {
            out.println(
                    "edge [ source " + a.id + " target " + b.id + " label \"" + e + "\" ]");
        });

        out.println("]");
    }
}



