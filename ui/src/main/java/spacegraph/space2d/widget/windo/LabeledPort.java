package spacegraph.space2d.widget.windo;

import spacegraph.space2d.widget.text.Label;

import java.util.function.Function;

public class LabeledPort<X> extends Port {
    private final Label l = new Label("?");

    public static LabeledPort<?> generic() {
        return new LabeledPort(Object::toString);
    }

    private LabeledPort(Function<X, String> toString) {
        content(l);
        on((v)->l.text(toString.apply((X) v)));
    }

}
