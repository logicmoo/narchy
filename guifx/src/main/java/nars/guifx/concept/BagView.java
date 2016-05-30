package nars.guifx.concept;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import nars.Global;
import nars.bag.ArrayBLink;
import nars.bag.Bag;

import java.util.Collection;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;

import static java.util.stream.Collectors.toCollection;
import static javafx.application.Platform.runLater;

/**
 * Created by me on 3/18/16.
 */
public class BagView<X> extends VBox /* FlowPane */ implements Runnable {

    final Map<ArrayBLink<X>, Node> componentCache = new WeakHashMap<>();
    private final Supplier<Bag<X>> bag;
    private final Function<ArrayBLink<X>, Node> builder;
    final Collection<ArrayBLink<X>> pending = Global.newHashSet(1); //Global.newArrayList();
    final AtomicBoolean queued = new AtomicBoolean();
    private final int limit;


    public BagView(Supplier<Bag<X>> bag, Function<ArrayBLink<X>, Node> builder, int limit) {
        this.bag = bag;
        this.builder = builder;
        this.limit = limit;

        setCache(true);

        update();
    }

    Node getNode(ArrayBLink<X> n) {
        Node existing = componentCache.computeIfAbsent(n, builder::apply);
//            Node existing = componentCache.get(n);
//            if (existing == null) {
//                componentCache.put(n, existing = builder.apply(n));
//            } else {
        //since it will already have been run as part of the builder
        if (existing instanceof Runnable)
            ((Runnable) existing).run();
//            }
        return existing;
    }

    public void update() {

        Bag<X> bLinks = bag.get();
        if (bLinks == null) {
            return;
        }

        if (!queued.compareAndSet(false, true)) {
            Collection<ArrayBLink<X>> p = this.pending;
            p.clear();
            bLinks.forEach(limit, p::add);

            //if (!getChildren().equals(p)) {
            runLater(this);
            /*} else {
                queued.set(false);
            }*/
        }

    }

    @Override
    public void run() {
        //synchronized (pending) {
        if (!queued.get())
            return;

        try {
            ObservableList<Node> ch = getChildren();
            ch.clear();
            pending.stream().map(this::getNode).collect(toCollection(() -> ch));
        } catch (Exception e) {
            System.err.println("BagView: " + e);
        }

//            getChildren().forEach(n -> {
//                if (n instanceof Runnable)
//                    ((Runnable) n).run();
//            });

        queued.set(false);

    }
}
