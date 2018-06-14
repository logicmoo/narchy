package jcog.lab;

import jcog.lab.util.ExperimentRun;
import jcog.lab.util.Optimization;
import jcog.math.FloatRange;
import jcog.math.Range;
import org.eclipse.collections.api.list.ImmutableList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabTest {

    static class Dummy {
        public float a = 0;
    }

    @Test
    public void testSimple() {
        Lab<Dummy> lab = new Lab<>(()->new Dummy());
        ExperimentRun<Dummy> t = lab.run((m, trial) -> {
            for (int i = 0; i < 10; i++) {
                m.a = (float) Math.sin(i);
                trial.sense();
            }

            m.a = 0.5f;
            m.a = 0.2f;
        }, List.of(
            Sensor.numeric("a", (Dummy m) -> m.a)
        ) );
        t.run();
        t.data.print();
    }

    public static class Model {
        public final SubModel sub = new SubModel();

        public final FloatRange floatRange = new FloatRange(0.5f, 0, 10);

        @Range(min=0, max=5/*, step=1f*/)
        public float tweakFloat = 0;

        @Range(min=-4, max=+4, step=2f)
        public int tweakInt = 0;

        public final int untweakInt = 0;

        public float score() {
            return (float) (
                    tweakInt +
                            Math.sin(-1 + tweakFloat) * tweakFloat +
                            (1f/(1f+sub.tweakFloatSub)))
                    + floatRange.floatValue();
        }

        public float score2() {
            return (float)(Math.cos(floatRange.floatValue()/4f));
        }

    }

    public static class SubModel {
        @Range(min=0, max=3, step=0.05f)
        public float tweakFloatSub;
    }

    @Test
    public void testSingleObjective() {
        Lab<Model> a = new Lab<>(Model::new).discover();
        a.vars.values().forEach(
                System.out::println
        );
        assertTrue(a.vars.size() >= 4);


        Optimization<Model> r = a.optimize((m)->m.score()); //.run(25);
        assertEquals(5, r.data.attrCount());

        r.print();
        r.tree(3, 4).print();
        ImmutableList best = r.best();
        assertTrue(((Number) best.get(0)).doubleValue() >= 5f);

        r.data.print();
    }
//
//    @Test
//    public void testMultiObjective() {
//        Variables<Model> a = new Variables<>(Model::new).discover();
//
//
//
//        Lab.Result r = a.optimize((m->m), ((FloatFunction<Model>)(m->m.score())), m->m.score2()).run(25);
//        assertEquals(7, r.data.attrCount());
//
//        r.print();
//        r.tree(3, 6).print();
//
//        r.data.print();
//    }
}