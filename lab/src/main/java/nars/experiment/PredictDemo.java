package nars.experiment;

import jcog.learn.LivePredictor;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.sensor.Scalar;
import nars.gui.NARui;
import nars.time.clock.RealTime;
import nars.util.signal.BeliefPredict;
import org.eclipse.collections.api.block.function.primitive.LongToFloatFunction;
import spacegraph.SpaceGraph;
import spacegraph.space2d.container.grid.Gridding;

import java.util.List;

public class PredictDemo {
    public static void main(String[] args) {
        float fps = 50f;

        NAR n = new NARS.DefaultNAR(0, true)
                .time(new RealTime.MS(true).durFPS(fps))
                .get();


        LongToFloatFunction f = (rt) -> {
            float t = ((float) rt) / n.dur();
            return (float) (0.5f + 0.5f * Math.sin(t / 6f));
        };
        Scalar X1 = new Scalar($.the("x1"), f, n);
        Scalar X2 = new Scalar($.the("x2"), f, n);


        int history = 32;
        int projections = 64;
        int sampleDur = n.dur() * 2;
        new BeliefPredict(List.of(X1), history, sampleDur, projections,

                new LivePredictor.LSTMPredictor(0.15f, 1),
                n
        );
        new BeliefPredict(List.of(X2), history, sampleDur, projections,

                new LivePredictor.MLPPredictor(0.15f),
                n
        );

        SpaceGraph.window(new Gridding(Gridding.VERTICAL,

                NARui.beliefCharts(64, List.of(X1, X2), n)
//
//                NARui.beliefCharts(256, List.of(X), n),
//                NARui.beliefCharts(2048, List.of(X), n)

        ), 800, 800);

        n.startFPS(fps);
    }
}
