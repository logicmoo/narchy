package nars.remote;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.experiment.arkanoid.Arkancide;
import nars.gui.Vis;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.time.MySTMClustered;
import nars.time.FrameClock;
import nars.truth.Truth;
import nars.util.data.random.XorShift128PlusRandom;
import nars.video.*;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import spacegraph.Surface;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import static nars.$.t;
import static nars.experiment.tetris.Tetris.DEFAULT_INDEX_WEIGHT;
import static nars.experiment.tetris.Tetris.conceptLinePlot;
import static nars.experiment.tetris.Tetris.exe;
import static spacegraph.SpaceGraph.window;
import static spacegraph.obj.GridSurface.col;
import static spacegraph.obj.GridSurface.grid;

/**
 * Created by me on 9/19/16.
 */
abstract public class SwingAgent extends NAgent {

    public final Map<String, CameraSensor> cam = new LinkedHashMap<>();

    public SwingAgent(NAR nar, int reasonerFramesPerEnvironmentFrame) {
        super(nar, reasonerFramesPerEnvironmentFrame);

    }

    public static void playSwing(Function<NAR, SwingAgent> init,int framesToRun) {
        Random rng = new XorShift128PlusRandom(1);

        //Multi nar = new Multi(3,512,
        Default nar = new Default(1024,
                Arkancide.CONCEPTS_FIRE_PER_CYCLE, 2, 2, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), DEFAULT_INDEX_WEIGHT, false, exe)
                //new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 100000, 8192, 2)

                , new FrameClock(), exe);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.8f);

        float p = 0.4f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.75f * p;
        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.25f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f * p;

        //nar.cyclesPerFrame.set(Arkancide.cyclesPerFrame);
        nar.confMin.setValue(0.02f);
        nar.compoundVolumeMax.setValue(24);
        //new Abbreviation2(nar, "_");

        MySTMClustered stm = new MySTMClustered(nar, 128, '.', 3);
        MySTMClustered stmGoal = new MySTMClustered(nar, 64, '!', 2);

        SwingAgent a = init.apply(nar);
        a.trace = true;


        int history = 200;
        window(
                grid(
                        grid(a.cam.values().stream().map(cs -> new CameraSensorView(cs, nar)).toArray(Surface[]::new)),

                        //Vis.concepts(nar, 32),
                        Vis.agentActions(a, history),

                        Vis.budgetHistogram(nar, 10),
                        conceptLinePlot(nar,
                                Iterables.concat(a.actions, Lists.newArrayList(a.happy, a.joy)),
                                200)
                ), 1200, 900);


        a.run(framesToRun).join();
        //a.runSync(runFrames);

        NAR.printTasks(nar, true);
        NAR.printTasks(nar, false);
        nar.printConceptStatistics();
    }

    /**
     * pixelTruth defaults to linear monochrome brightness -> frequency
     */
    protected CameraSensor<Scale> addCamera(String id, Container w, int pw, int ph) {
        return addCamera(id, w, pw, ph, (v) -> t(v, alpha));
    }

    protected CameraSensor<Scale> addCamera(String id, Container w, int pw, int ph, FloatToObjectFunction<Truth> pixelTruth) {
        return addCamera(id, new Scale(new SwingCamera(w), pw, ph), pixelTruth);
    }

    protected <C extends PixelCamera> CameraSensor<C> addCamera(String id, C bc, FloatToObjectFunction<Truth> pixelTruth) {
        CameraSensor c = new CameraSensor<>($.the(id), bc, this, pixelTruth);
        cam.put(id, c);
        return c;
    }


    @Override
    protected float act() {

        cam.values().forEach(CameraSensor::update);

        //TODO update input

        return reward();
    }

    protected abstract float reward();
}
