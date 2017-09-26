package nars;

import jcog.Util;
import jcog.data.FloatParam;
import jcog.math.FloatNormalized;
import nars.concept.ActionConcept;
import nars.concept.GoalActionAsyncConcept;
import nars.concept.GoalActionConcept;
import nars.control.CauseChannel;
import nars.task.ITask;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import nars.truth.TruthFunctions;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import static jcog.Util.unitize;
import static nars.Op.BELIEF;
import static nars.Op.GOAL;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.c2wSafe;
import static nars.truth.TruthFunctions.w2c;

/**
 * Created by me on 9/30/16.
 */
public interface NAct {

    @NotNull Map<ActionConcept, CauseChannel<ITask>> actions();

    NAR nar();

    /**
     * master curiosity factor, for all actions
     */
    FloatParam curiosity();

    /**
     * latches to either one of 2 states until it shifts to the other one. suitable for representing
     * push-buttons like keyboard keys. by default with no desire the state is off.  the 'on' and 'off'
     * procedures will be called only as necessary (when state changes).  the off procedure will not be called immediately.
     * its initial state will remain indetermined until the first feedback is generated.
     */
    @Nullable
    default void actionToggle(@NotNull Term t, @NotNull Runnable on, @NotNull Runnable off) {
        //float THRESH = 0.5f;
//        GoalActionConcept m = new GoalActionConcept(s, this, (b, d) -> {
//            boolean next = d != null && d.freq() > THRESH;
//            return toggle(d, on, off, next);
//        });

        //m.resolution(0.5f);
        float deadZoneFreqRadius = 1f / 6;

        final boolean[] last = {false};
        actionBipolar(t, (float f) -> {

            //radius of center dead zone; diameter = 2x this

            if (f > deadZoneFreqRadius) {
                on.run();
                last[0] = true;
                return 1f;
            } else if (f < -deadZoneFreqRadius) {
                off.run();
                last[0] = false;
                return -1f;
            } else {
                return last[0] ? 1f : -1f;
            }
        });
        //m.resolution(1f);
        //return addAction(m);
    }

//    /** softmax-like signal corruption that emulates PWM (pulse-width modulation) modulated by desire frequency */
//    @Nullable default ActionConcept actionTogglePWM(@NotNull Compound s, @NotNull Runnable on, @NotNull Runnable off) {
//        ActionConcept m = new GoalActionConcept(s, this, (b, d) -> {
//            float df = d != null ? d.freq() : 0.5f;
//            boolean corrupt = nar().random().nextFloat() > Math.abs(df - 0.5f) * 2f;
//
//            boolean next = df > 0.5f;
//            if (corrupt) next = !next;
//
//            return toggle(on, off, next);
//        });
//
//        actions().add(m);
//        return m;
//    }

    @Nullable
    default Truth toggle(@Nullable Truth d, @NotNull Runnable on, @NotNull Runnable off, boolean next) {
        float freq;
        if (next) {
            freq = +1;
            on.run();
        } else {
            freq = 0f;
            off.run();
        }

        return $.t(freq,
                //d!=null ? d.conf() : nar().confMin.floatValue());
                nar().confDefault(BELIEF) /*d.conf()*/);
    }

    /**
     * selects one of 2 states until it shifts to the other one. suitable for representing
     * push-buttons like keyboard keys. by default with no desire the state is off.   the off procedure will not be called immediately.
     */
    @Nullable
    default void actionTriState(@NotNull Term s, @NotNull IntConsumer i) {
        actionTriState(s, (v) -> {
            i.accept(v);
            return true;
        });
    }

    /**
     * tri-state implemented as delta version memory of last state.
     * initial state is neutral.
     */
    @Nullable
    default void actionTriState(@NotNull Term cc, @NotNull IntPredicate i) {
        //final int[] state = {0};
        //new GoalActionConcept(cc, this, (b, d) -> {
        actionBipolar(cc, (float f) -> {

            f = f / 2f + 0.5f;

            //radius of center dead zone; diameter = 2x this
            float deadZoneFreqRadius = 1f / 6;
            int s;
            if (f > 0.5f + deadZoneFreqRadius)
                s = +1;
            else if (f < 0.5f - deadZoneFreqRadius)
                s = -1;
            else
                s = 0;

            if (i.test(s)) {

                //            int curState = state[0];
                //            state[0] = Math.min(Math.max(curState + deltaState, -1), +1);
                //
                //            //float f = curState != state[0] ? (deltaState > 0 ? 1f : 0f) : 0.5f /* had no effect */;
                switch (s) { //state[0]) {
                    case -1:
                        return -1f;
                    case 0:
                        return 0f;
                    case +1:
                        return +1f;
                    default:
                        throw new RuntimeException();
                }

            }

            return Float.NaN;
        });
        //m.resolution(1f);
        //return addAction(m);
    }

    default <A extends ActionConcept> A addAction(A c) {
        CauseChannel existing = actions().put(c, nar().newCauseChannel(c));
        assert (existing == null);
        nar().on(c);
        return c;
    }

    @Nullable
    default GoalActionConcept actionTriStateContinuous(@NotNull Term s, @NotNull IntPredicate i) {

        GoalActionConcept m = new GoalActionConcept(s, this, (b, d) -> {
            //radius of center dead zone; diameter = 2x this
            // 1f/4;
            //1f/3f;


            int ii;
            if (d == null) {
                ii = 0;
            } else {
                float f = d.freq();
                float deadZoneFreqRadius = 1f / 6;
                if (f > 0.5f + deadZoneFreqRadius)
                    ii = +1;
                else if (f < 0.5f - deadZoneFreqRadius)
                    ii = -1;
                else
                    ii = 0;
            }

            boolean accepted = i.test(ii);
            if (!accepted)
                ii = 0; //HACK

            float f;
            switch (ii) {
                case 1:
                    f = 1f;
                    break;
                case 0:
                    f = 0.5f;
                    break;
                case -1:
                    f = 0f;
                    break;
                default:
                    throw new RuntimeException();
            }

            return $.t(f, nar().confDefault(BELIEF));
        });
        //m.resolution.setValue(0.5f);

        return addAction(m);
    }

    @Nullable
    default ActionConcept actionTriStatePWM(@NotNull Term s, @NotNull IntConsumer i) {
        ActionConcept m = new GoalActionConcept(s, this, (b, d) -> {


            int ii;
            if (d == null) {
                ii = 0;
            } else {
                float f = d.freq();
                if (f == 1f) {
                    ii = +1;
                } else if (f == 0) {
                    ii = -1;
                } else if (f > 0.5f) {
                    ii = nar().random().nextFloat() <= ((f - 0.5f) * 2f) ? +1 : 0;
                } else if (f < 0.5f) {
                    ii = nar().random().nextFloat() <= ((0.5f - f) * 2f) ? -1 : 0;
                } else
                    ii = 0;
            }

            i.accept(ii);

            float f;
            switch (ii) {
                case 1:
                    f = 1f;
                    break;
                case 0:
                    f = 0.5f;
                    break;
                case -1:
                    f = 0f;
                    break;
                default:
                    throw new RuntimeException();
            }

            return
                    //d!=null ?
                    $.t(f,
                            //d.conf()
                            nar().confDefault(BELIEF)
                    )
                    //: null
                    ;
        });
        return addAction(m);
    }

    @Nullable
    default void actionToggle(@NotNull Term s, @NotNull BooleanProcedure onChange) {
        actionToggle(s, () -> onChange.value(true), () -> onChange.value(false));
    }
//
//    @Nullable
//    default ActionConcept actionToggleRapid(@NotNull Compound s, @NotNull BooleanProcedure onChange, int minPeriod) {
//        return actionToggleRapid(s, () -> onChange.value(true), () -> onChange.value(false), minPeriod);
//    }
//
//    /**
//     * rapid-fire pushbutton with a minPeriod after which it is reset to off, allowing
//     * re-triggering to ON while the true state remains enabled
//     * <p>
//     * TODO generalize to actionPWM (pulse width modulation) with controllable reset period (ex: by frequency, or conf etc)
//     */
//    @Nullable
//    default ActionConcept actionToggleRapid(@NotNull Compound term, @NotNull Runnable on, @NotNull Runnable off, int minPeriod) {
//
//        if (minPeriod < 1)
//            throw new UnsupportedOperationException();
//
//        final long[] reset = {Tense.ETERNAL}; //last enable time
//        final int[] state = {0}; // 0: unknown, -1: false, +1: true
//
//        ActionConcept m = new GoalActionConcept(term, this, (b, d) -> {
//
//            boolean next = d != null && d.freq() >= 0.5f;
//
//            float alpha = nar().confDefault(BELIEF);
//            int v;
//            int s;
//            if (!next) {
//                reset[0] = Tense.ETERNAL;
//                s = -1;
//                v = 0;
//            } else {
//
//                long lastReset = reset[0];
//                long now = nar().time();
//                if (lastReset == Tense.ETERNAL) {
//                    reset[0] = now;
//                    s = -1;
//                } else {
//                    if ((now - lastReset) % minPeriod == 0) {
//                        s = -1;
//                    } else {
//                        s = +1;
//                    }
//                }
//                v = 1;
//            }
//
//            if (state[0] != s) {
//                if (s < 0)
//                    off.run();
//                else
//                    on.run();
//                state[0] = s;
//            }
//
//            return $.t(v, alpha);
//        });
//
//        actions().add(m);
//        return m;
//    }

    /**
     * the supplied value will be in the range -1..+1. if the predicate returns false, then
     * it will not allow feedback through. this can be used for situations where the action
     * hits a limit or boundary that it did not pass through.
     * <p>
     * TODO make a FloatToFloatFunction variation in which a returned value in 0..+1.0 proportionally decreasese the confidence of any feedback
     */
    @NotNull
    default GoalActionConcept action(@NotNull String s, @NotNull GoalActionConcept.MotorFunction update) throws Narsese.NarseseException {
        return action($.$(s), update);
    }

    @NotNull
    default GoalActionConcept action(@NotNull Term s, @NotNull GoalActionConcept.MotorFunction update) {
        return addAction(new GoalActionConcept(s, this, update));
    }

    default void actionBipolar(@NotNull Term s, @NotNull FloatToFloatFunction update) {
        actionBipolarExpectation(s, update);
        //actionBipolarExpectationNormalized(s, update);
        //actionBipolarGreedy(s, update);
        //actionBipolarMutex3(s, update);
    }

    default void actionBipolarExpectationNormalized(@NotNull Term s, @NotNull FloatToFloatFunction update) {
        float v[] = new float[1];
        FloatNormalized f = new FloatNormalized(() -> v[0]).relax(0.995f);
        actionBipolarExpectation(s, (x) -> {
            v[0] = Math.abs(x); //HACK
            float y = f.asFloat() * Math.signum(x);
            float z = update.valueOf(y);
            return z; //limit to below input
            //u;
        });
    }

    default void actionBipolarExpectation(@NotNull Term s, @NotNull FloatToFloatFunction update) {
        Term pt =
                //$.inh( $.the("\"+\""), s);
                $.p(s, s.neg());
        Term nt =
                //$.inh($.the("\"-\""), s);
                $.p(s.neg(), s);


        //boolean highpass = true;


        final float f[] = new float[2];
        final float c[] = new float[2];
        final float exp[] = new float[2];

        @NotNull BiConsumer<GoalActionAsyncConcept, Truth> u = (action, g) -> {

            boolean p = action.term().equals(pt);
            float f0, c0;

            NAR n = nar();


            float cur = curiosity().floatValue();
            Random rng = n.random();

            float restConf =
                    //n.confMin.floatValue() * 2;
                    //nar().confDefault(GOAL)/2;
                    nar().confDefault(GOAL);

            //n.confDefault(BELIEF);
            //0;

            int ip = p ? 0 : 1;
            f[ip] = g!=null ? g.freq() : 0f;
            c[ip] = g!=null ? g.conf() : 0f;
            exp[ip] = g != null ? g.expectation() : 0f;
            //evi[ip] = g != null ? g.evi(): 0f;

            if (!p) {


                float ew;


                float x; //0..+1
                if (cur > 0 && rng.nextFloat() <= cur) {
                    x = (TruthFunctions.expectation(
                        rng.nextFloat(),
                        restConf
                    ) - 0.5f)*2f;
                    c[0] = c[1] = restConf;
                } else {

//                    int winner =
////                            //Util.decideRoulette(2, (i) -> cc[i], n.random());
////
//                        //SOFTMAX
//                        Util.decideSoftmax(2,
//                                //(i) -> Math.abs(exp[i]),
//                                (i) -> Math.abs(evi[i]),
//                                0.4f, n.random());

                    //GREEDY
                    //exp[0] > exp[1] ? 0 : 1;

                    //absolute
//                    x = (Math.max(0.5f,
//                             exp[winner] - 0.5f
//                    ) - 0.5f) * 2f; //0..+1

                    //compare positive vs negative
                    x =
                            ((f[0]-0.5f) - (f[1]-0.5f));  //-1..+1
                            //((exp[0]-0.5f) - (exp[1]-0.5f));  //-1..+1
                            //(1 - Math.abs(exp[0] - exp[1])) * (f[0]-0.5f) - (f[1]-0.5f);  //-1..+1 discounted by difference in expectation
                }



                PreciseTruth N, P;

                float y = update.valueOf(x); //-1..+1

                //inverse expectation
                float conf = y == y ?
                            //restConf/2f,
                            //Math.abs(y) * Math.abs(x)
                            //Math.abs(y) * restConf
                            //Math.abs(y)
                            //w2c((c2wSafe(c[0]) + c2wSafe(c[1]))/2f)
                            //Math.max(c[0],c[1])
                            Math.min(c[0],c[1])
                        : 0;

                //w2c(Math.abs(y) * c2w(restConf));
                if (conf >= nar().confMin.floatValue()) {
                    //Math.max(cc[winner], nar().confMin.floatValue());
                    //0.5f + cc[winner] * ((winner == 0 ? y : -y)) * 0.5f;

                    P = $.t(1, conf);
                    N = $.t(0.5f, conf);
                } else {
                    //P = N = $.t(0.5f, conf);
                    N = P = null;
                }


                PreciseTruth pb = y >= 0 ? P : N;
                PreciseTruth pg =
                        //pb;
                        //null;
                        pb!=null ? pb.eviMult(0.5f) : null;
                ((GoalActionAsyncConcept) n.concept(pt)).feedback(pb, pg, n);
                PreciseTruth nb = y >= 0 ? N : P;
                PreciseTruth ng =
                        //nb;
                        //null;
                        nb!=null ? nb.eviMult(0.5f) : null;
                ((GoalActionAsyncConcept) n.concept(nt)).feedback(nb, ng, n);
            }
        };

        GoalActionAsyncConcept p = new GoalActionAsyncConcept(pt, this, u);
        GoalActionAsyncConcept n = new GoalActionAsyncConcept(nt, this, u);

        addAction(p);
        addAction(n);

    }

    default void actionBipolarGreedy(@NotNull Term s, @NotNull FloatToFloatFunction update) {
        Term pt =
                //$.inh( $.the("\"+\""), s);
                $.p(s, s.neg());
        Term nt =
                //$.inh($.the("\"-\""), s);
                $.p(s.neg(), s);

        /** default neutral "resting" frequency; generally some value [0..0.5] */
        final float restFreq = 0f;

        final float ff[] = new float[2];
        final float cc[] = new float[2];
        @NotNull BiConsumer<GoalActionAsyncConcept, Truth> u = (c, g) -> {

            boolean p = c.term().equals(pt);
            float f0, c0;

            NAR n = nar();


            float cur = curiosity().floatValue();
            Random rng = n.random();

            float restConf =
                    //n.confMin.floatValue();
                    n.confDefault(GOAL) * 0.5f;
            //n.confDefault(BELIEF);


            f0 = g != null ? g.freq() : restFreq;
            c0 = g != null ? g.conf() : restConf; //n.confMin.floatValue();


            int ip = p ? 0 : 1;
            ff[ip] = f0;
            cc[ip] = c0;


            if (!p) {

                int winner =
                        //Util.decideRoulette(2, (i) -> cc[i], n.random());
                        Util.decideSoftmax(2, (i) -> c2w(cc[i]), 0.6f, n.random());
                //cc[0] > cc[1] ? 0 : 1; //GREEDY

                int loser = 1 - winner;

                //curiosity applied to winner, how ironic
                if (cur > 0 && rng.nextFloat() <= cur) {
                    ff[winner] = //rng.nextFloat(); //bipolar
                            0.5f + 0.5f * rng.nextFloat(); //unipolar, [0.5,1.0]
                    cc[winner] = restConf;
                }

                float loserBoost = (0.5f - Math.min(0.5f, ff[loser]));
                float winnerBase = Math.max(0, (ff[winner] - 0.5f));
                float x =
                        //(ff[winner] - 0.5f) * 2f;
                        winnerBase * 2f; //skip negative half, expand to full range
                //winnerBase + loserBoost;
                //Util.unitize(Util.max(winnerBase, loserBoost));
                //Util.unitize(Util.or(winnerBase, loserBoost));
                float y = update.valueOf(winner == 0 ? x : -x); //invert depending on which polarity won

                float conf =
                        //cc[winner];
                        //Util.max(cc[0], cc[1]);
                        nar().confDefault(BELIEF);

                Truth w = y == y ? $.t(
                        (winner == 0 ? y : -y) / 2f + 0.5f, //un-map to unipolar frequency range
                        conf) : null;

                Truth l;
                if (w != null) {
                    l = $.t(restFreq, conf);
                } else {
                    w = l = //null;
                            $.t(restFreq, conf);
                }
                ((GoalActionAsyncConcept) n.concept(winner == 0 ? pt : nt)).feedback(w, null, n);
                ((GoalActionAsyncConcept) n.concept(winner == 1 ? pt : nt)).feedback(l, null, n);
            }
        };

        GoalActionAsyncConcept p = new GoalActionAsyncConcept(pt, this, u);
        GoalActionAsyncConcept n = new GoalActionAsyncConcept(nt, this, u);

        addAction(p);
        addAction(n);


//        float cm =
//                //0.01f;
//                2 * nar().confMin.floatValue(); //HACK wont change if the parameter changes during runtime
//        nar().believe(p.term(), restFreq, cm);
//        nar().believe(n.term(), restFreq, cm);
//        nar().goal(p.term(), restFreq, cm);
//        nar().goal(n.term(), restFreq, cm);

    }

//    default void actionBipolarMutex3(@NotNull Term s, @NotNull FloatToFloatFunction update) {
//        Term pt =
//                //$.inh( $.the("\"+\""), s);
//                $.p(s, s.neg());
//        Term nt =
//                //$.inh($.the("\"-\""), s);
//                $.p(s.neg(), s);
//
//        final float ff[] = new float[2];
//        final float cc[] = new float[2];
//        @NotNull BiConsumer<GoalActionAsyncConcept, Truth> u = (c, g) -> {
//
//            boolean p = c.term().equals(pt);
//            float f0, c0;
//
//            NAR n = nar();
//
//            float cur = curiosity().floatValue();
//            if (cur > 0 && n.random().nextFloat() <= cur) {
//                f0 = n.random().nextFloat();
//                c0 = n.confDefault(BELIEF);
//            } else {
//                f0 = g != null ? g.freq() : 0f;
//                c0 = g != null ? g.conf() : n.confMin.floatValue();
//            }
//
////            if (f0 < 0.5f)
////                f0 = 0f;
//
//            int ip = p ? 0 : 1;
//            ff[ip] = f0;
//            cc[ip] = c0;
//
//            if (!p) {
//
//                float e0 = c2w(cc[0]);
//                float e1 = c2w(cc[1]);
//                float x =
//                        Util.clamp((ff[0] - ff[1]), -1, +1);
//                //Util.clamp((ff[0] * e0 - ff[1] * e1)/(e0+e1), -1, +1);
//
//                float y = update.valueOf(x);
//                if (y == y) {
//
//                    float momentum = 1f;
//
//                    Truth r;
//                    r = $.t(y / 2f + 0.5f, w2c(momentum * (e0 + e1)));
//
//
//                    ((GoalActionAsyncConcept) n.concept(pt)).feedback(r);
//                    ((GoalActionAsyncConcept) n.concept(nt)).feedback(r.neg());
//                } else {
//                    ((GoalActionAsyncConcept) n.concept(pt)).feedback(null);
//                    ((GoalActionAsyncConcept) n.concept(nt)).feedback(null);
//                }
//            }
//
//        };
//
//        GoalActionAsyncConcept p = new GoalActionAsyncConcept(pt, this, u);
//        GoalActionAsyncConcept n = new GoalActionAsyncConcept(nt, this, u);
//
//        addAction(p);
//        addAction(n);
//
//        float cm =
//                //0.01f;
//                2 * nar().confMin.floatValue(); //HACK wont change if the parameter changes during runtime
//        nar().believe(p.term(), 0f, cm);
//        nar().believe(n.term(), 0f, cm);
//        nar().goal(p.term(), 0f, cm);
//        nar().goal(n.term(), 0f, cm);
//
//    }
//
//    default void actionBipolarMutex2(@NotNull Term s, @NotNull FloatToFloatFunction update) {
//        Term pt =
//                //$.inh( $.the("\"+\""), s);
//                $.p(s, $.the("\"+\""));
//        Term nt =
//                //$.inh($.the("\"-\""), s);
//                $.p(s, $.the("\"-\""));
//
//        final float ff[] = new float[2];
//        @NotNull BiConsumer<GoalActionAsyncConcept, Truth> u = (c, g) -> {
//            boolean p;
//            p = c.term().equals(pt);
//            float gf = g != null ?
//                    (g.expectation() - 0.5f) * 2f : 0f
//                    //(g.freq() - 0.5f) * 2f : 0f
//                    //g.freq() : 0f
//                    //(g.freq() > 0.5f ? (g.freq() - 0.5f)*2f : 0f) : 0f
//                    ;
//
//            NAR n = nar();
//
//            float cur = curiosity().floatValue();
//            if (cur > 0 && n.random().nextFloat() <= cur) {
//                gf = n.random().nextFloat();
//            }
//
//            ff[p ? 0 : 1] = gf;
//
//
//            if (!p) {
//                assert (ff[0] == ff[0]); //assert that positive has been set in this cycle
//
//                float dx =
//                        Math.abs(ff[0]) + Math.abs(ff[1]);
//                //(float) Math.sqrt(ff[0] * (1f-ff[1]));
//                float x;
//                if (Util.equals(0, dx, Param.TRUTH_EPSILON)) {
//                    x = ff[0] > ff[1] ? 1f : 0f;
//                } else {
//                    x = (((ff[0]) / dx) - 0.5f) * 2f;
//                    x = Util.clamp(x, -1, +1);
//                }
//
//                float y = update.valueOf(x);
//                if (y != y)
//                    y = 0;
//                else {
////                    float momentum = 0.5f;
////                    y *= momentum;
//                }
//
////                //y = Util.clamp(y, -1f/2, +1f/2);
////
////                float pp, nn;
////                if (y > 0) {
////                    pp = +y; //0.5f + (balance/2f);
////                    nn = 1f-pp; //-pp; // 0;
//////                    if (pp > 0.5f) {
//////                        nn = -(pp - 0.5f);
//////                        pp = 0.5f;
//////                    }
////                } else {
////                    nn = -y; //0.5f + (-balance/2f);
////                    pp = 1f-nn; //-nn; //0;
//////                    if (nn > 0.5f) {
//////                        pp = -(nn - 0.5f);
//////                        nn = 0.5f;
//////                    }
////                }
////
//////                pp = Util.clamp(pp, -0.5f, +0.5f) + 0.5f;
//////                nn = Util.clamp(nn, -0.5f, +0.5f) + 0.5f;
////
////                pp = Util.clamp(pp, 0f, +1);
////                //if (pp < Param.TRUTH_EPSILON) pp = 0; else pp = pp/2f + 0.5f;
////                nn = Util.clamp(nn, 0f, +1);
////                //if (nn < Param.TRUTH_EPSILON) nn = 0; else nn = nn/2f + 0.5f;
//
//
//                float pp, nn;
//                if (y > 0) {
//                    pp = y / 2f + 0.5f;
//                    nn = 1f - pp;
//                } else {
//                    nn = -y / 2f + 0.5f;
//                    pp = 1f - nn;
//                }
//                //float sf = Util.equals(x, 0f, Param.TRUTH_EPSILON) ? 0f : (y/x);
////                float pp = ff[0] * sf;
////                float nn = ff[1] * sf;
//                ff[0] = ff[1] = Float.NaN; //reset for next cycle
//                float conf =
//                        //(cc[0] + cc[1])/2f;
//                        n.confDefault(GOAL);
//                //if (conf >= n.confMin.floatValue()) {
//                ((GoalActionAsyncConcept) n.concept(pt)).feedback($.t(pp, conf));
//                ((GoalActionAsyncConcept) n.concept(nt)).feedback($.t(nn, conf));
//                //}
//            }
//        };
//
//        GoalActionAsyncConcept p = new GoalActionAsyncConcept(pt, this, u);
//        GoalActionAsyncConcept n = new GoalActionAsyncConcept(nt, this, u);
//        addAction(p);
//        addAction(n);
//
//        //        return actionUnipolar(s, (f) -> {
////            if (f != f)
////                return Float.NaN;
////            else {
////                float y = (f - 0.5f) * 2f;
////                return (update.valueOf(y) / 2) + 0.5f;
////            }
////        });
//
////        addAction(new BeliefActionConcept(s, nar(), (t) -> {
////            float f;
////            if (t == null)
////                f = 0f;
////            else
////                f = (t.freq()-0.5f)*2f;
////            update.valueOf( f );
////        }));
//    }
//
//
//    default void actionBipolarMutex(@NotNull Term s, @NotNull FloatToFloatFunction update) {
//        Term pt =
//                //$.inh( $.the("\"+\""), s);
//                $.p(s, $.the("\"+\""));
//        Term nt =
//                //$.inh($.the("\"-\""), s);
//                $.p(s, $.the("\"-\""));
//
//        final float ff[] = new float[2];
//        final float cc[] = new float[2];
//        @NotNull BiConsumer<GoalActionAsyncConcept, Truth> u = (c, g) -> {
//            boolean p;
//            p = c.term().equals(pt);
//            ff[p ? 0 : 1] = g != null ?
//                    //(g.expectation() - 0.5f)*2f: 0f
//                    //(g.freq() - 0.5f) * 2f : 0f
//                    g.freq() : 0f
//            //(g.freq() > 0.5f ? (g.freq() - 0.5f)*2f : 0f) : 0f
//            ;
//
//            NAR n = nar();
//            cc[p ? 0 : 1] = (g != null) ? g.conf() : 0;
//
//            if (!p) {
//                assert (ff[0] == ff[0]); //assert that positive has been set in this cycle
//
//
//                float x;
//
//                //curiosity noise
//                float cur = curiosity().floatValue();
//                if (cur > 0 && n.random().nextFloat() <= cur) {
//                    //x = Util.clamp(x + (n.random().nextFloat())*4f - 2f, -1f, +1f);
//                    //x = n.random().nextFloat() * 2f - 1f;
//                    x = Util.clamp((float) n.random().nextGaussian(), -1f, +1f);
//                    //x = (n.random().nextBoolean() ? +1 : -1f) * (0.5f + Util.clamp((float) Math.abs(n.random().nextGaussian()), 0f, 0.5f));
//                } else {
//                    x = (ff[0] - ff[1]);
//
////                    x *= (Math.abs(ff[0] - ff[1])/2f);
//
////                    float range =
////                            //Math.abs(ff[0] - ff[1]);
////                            //Math.abs(ff[0]) + Math.abs(ff[1]);
////                            Math.max(Math.abs(ff[0]), Math.abs(ff[1]));
////                    if (range >= Param.TRUTH_EPSILON)
////                        x /= range; //normalize against its own range
////                    else
////                        x = 0; //no difference anyway
//                }
//
//
//                float y = update.valueOf(x);
//                if (y != y)
//                    y = 0;
//                else {
////                    float momentum = 0.5f;
////                    y *= momentum;
//                }
//
//                //y = Util.clamp(y, -1f/2, +1f/2);
//
//                float pp, nn;
//                if (y > 0) {
//                    pp = +y; //0.5f + (balance/2f);
//                    nn = 0; //-pp; // 0;
////                    if (pp > 0.5f) {
////                        nn = -(pp - 0.5f);
////                        pp = 0.5f;
////                    }
//                } else {
//                    nn = -y; //0.5f + (-balance/2f);
//                    pp = 0; //-nn; //0;
////                    if (nn > 0.5f) {
////                        pp = -(nn - 0.5f);
////                        nn = 0.5f;
////                    }
//                }
//
////                pp = Util.clamp(pp, -0.5f, +0.5f) + 0.5f;
////                nn = Util.clamp(nn, -0.5f, +0.5f) + 0.5f;
//
//                pp = Util.clamp(pp, 0f, +1);
//                if (pp < Param.TRUTH_EPSILON) pp = 0;
//                else pp = pp / 2f + 0.5f;
//                nn = Util.clamp(nn, 0f, +1);
//                if (nn < Param.TRUTH_EPSILON) nn = 0;
//                else nn = nn / 2f + 0.5f;
//
//                ff[0] = ff[1] = Float.NaN; //reset for next cycle
//                float conf =
//                        //(cc[0] + cc[1])/2f;
//                        n.confDefault(GOAL);
//                //if (conf >= n.confMin.floatValue()) {
//                ((GoalActionAsyncConcept) n.concept(pt)).feedback($.t(pp, conf));
//                ((GoalActionAsyncConcept) n.concept(nt)).feedback($.t(nn, conf));
//                //}
//            }
//        };
//
//        GoalActionAsyncConcept p = new GoalActionAsyncConcept(pt, this, u);
//        GoalActionAsyncConcept n = new GoalActionAsyncConcept(nt, this, u);
//        addAction(p);
//        addAction(n);
//
//        //        return actionUnipolar(s, (f) -> {
////            if (f != f)
////                return Float.NaN;
////            else {
////                float y = (f - 0.5f) * 2f;
////                return (update.valueOf(y) / 2) + 0.5f;
////            }
////        });
//
////        addAction(new BeliefActionConcept(s, nar(), (t) -> {
////            float f;
////            if (t == null)
////                f = 0f;
////            else
////                f = (t.freq()-0.5f)*2f;
////            update.valueOf( f );
////        }));
//    }

    /**
     * update function receives a value in 0..1.0 corresponding directly to the present goal frequency
     */
    @NotNull
    default GoalActionConcept actionUnipolar(@NotNull Term s, @NotNull FloatToFloatFunction update) {
        final float[] lastValue = {0.5f};
        boolean latch = false;
        return action(s, (b, d) -> {
            float o = (d != null) ?
                    //d.expectation()
                    d.freq()
                    : (latch ? lastValue[0] : Float.NaN);

            float f = update.valueOf(o == o ? o : 0);
            if (f != f)
                f = lastValue[0];
            else
                lastValue[0] = f;

            //return $.t(f, nar().confDefault(BELIEF));

            if (f == f)
                return $.t(f,
                        //d!=null ? d.conf() : nar().confMin.floatValue()
                        nar().confDefault(BELIEF)
                );
            else
                return null;

        });
    }

    /**
     * supplies values in range -1..+1, where 0 ==> expectation=0.5
     */
    @NotNull
    default GoalActionConcept actionExpUnipolar(@NotNull Term s, @NotNull FloatToFloatFunction update) {
        final float[] x = {0f}, xPrev = {0f};
        //final FloatNormalized y = new FloatNormalized(()->x[0]);
        return action(s, (b, d) -> {
            float o = (d != null) ?
                    //d.freq()
                    d.expectation() - 0.5f
                    : xPrev[0]; //0.5f /*Float.NaN*/;
            float ff;
            if (o >= 0f) {
                //y.relax(0.9f);
                //x[0] = o;
                float fb = update.valueOf(o /*y.asFloat()*/);
                if (fb != fb) {
                    //f = returxPrev[0];
                    return null;
                } else {
                    xPrev[0] = fb;
                }
                ff = (fb / 2f) + 0.5f;
            } else {
                ff = 0f;
            }
            return $.t(unitize(ff), nar().confDefault(BELIEF));
        });
    }

}
