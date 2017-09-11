package nars;

import jcog.Util;
import jcog.data.FloatParam;
import nars.concept.ActionConcept;
import nars.concept.BeliefActionConcept;
import nars.concept.GoalActionAsyncConcept;
import nars.concept.GoalActionConcept;
import nars.control.CauseChannel;
import nars.term.Term;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * Created by me on 9/30/16.
 */
public interface NAct {

    @NotNull Map<ActionConcept, CauseChannel<Task>> actions();

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
    default ActionConcept actionToggle(@NotNull Term s, @NotNull Runnable on, @NotNull Runnable off) {
        float THRESH = 0.5f;
        GoalActionConcept m = new GoalActionConcept(s, this, (b, d) -> {
            boolean next = d != null && d.freq() > THRESH;
            return toggle(d, on, off, next);
        });
        //m.resolution(0.5f);
        return addAction(m);
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
    default GoalActionConcept actionTriState(@NotNull Term s, @NotNull IntConsumer i) {
        return actionTriState(s, (v) -> {
            i.accept(v);
            return true;
        });
    }

    /**
     * tri-state implemented as delta version memory of last state.
     * initial state is neutral.
     */
    @Nullable
    default GoalActionConcept actionTriState(@NotNull Term cc, @NotNull IntPredicate i) {
        //final int[] state = {0};
        GoalActionConcept m = new GoalActionConcept(cc, this, (b, d) -> {
            //radius of center dead zone; diameter = 2x this


            int s;
            if (d == null) {
                s = 0;
            } else {
                float f = d.freq();
                float deadZoneFreqRadius = 1f / 6;
                if (f > 0.5f + deadZoneFreqRadius)
                    s = +1;
                else if (f < 0.5f - deadZoneFreqRadius)
                    s = -1;
                else
                    s = 0;
            }

            if (i.test(s)) {

                //            int curState = state[0];
                //            state[0] = Math.min(Math.max(curState + deltaState, -1), +1);
                //
                //            //float f = curState != state[0] ? (deltaState > 0 ? 1f : 0f) : 0.5f /* had no effect */;
                float f;
                switch (s) { //state[0]) {
                    case -1:
                        f = 0f;
                        break;
                    case 0:
                        f = 0.5f;
                        break;
                    case +1:
                        f = 1f;
                        break;
                    default:
                        throw new RuntimeException();
                }

                return $.t( f, nar().confDefault(BELIEF));
            }

            return b;
        });
        //m.resolution(1f);
        return addAction(m);
    }

    default <A extends ActionConcept> A addAction(A c) {
        CauseChannel existing = actions().put(c, nar().newCauseChannel(c));
        assert(existing == null);
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
    default ActionConcept actionToggle(@NotNull Term s, @NotNull BooleanProcedure onChange) {
        return actionToggle(s, () -> onChange.value(true), () -> onChange.value(false));
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
        return addAction( new GoalActionConcept(s, this, update) );
    }

    /**
     * the supplied value will be in the range -1..+1. if the predicate returns false, then
     * it will not allow feedback through. this can be used for situations where the action
     * hits a limit or boundary that it did not pass through.
     * <p>
     * TODO make a FloatToFloatFunction variation in which a returned value in 0..+1.0 proportionally decreasese the confidence of any feedback
     */
    @NotNull
    default void actionBipolar(@NotNull Term s, @NotNull FloatToFloatFunction update) {
        Term pt = $.p(s, $.the("\"+\""));
        Term nt = $.p(s, $.the("\"-\""));

        float gg[] = new float[2];
        @NotNull BiConsumer<GoalActionAsyncConcept, Truth> u = (c, g) -> {
            boolean p;
            p = c.term().equals(pt);
            gg[ p ? 0 : 1] = g!=null ?
                    //g.expectation() - 0.5f : 0
                    g.freq() : 0.5f
            ;
            if (!p) {
                assert(gg[0] == gg[0]); //assert that positive has been set in this cycle
                float balance = update.valueOf((gg[0] - gg[1]));
                float pp, nn;
                if (balance > 0) {
                    pp = +balance; //0.5f + (balance/2f);
                    nn = 0;
                } else {
                    nn = -balance; //0.5f + (-balance/2f);
                    pp = 0;
                }
                gg[0] = gg[1] = Float.NaN; //reset for next cycle
                float conf = nar().confDefault(GOAL);
                ((GoalActionAsyncConcept)nar().concept(pt)).feedback($.t(pp, conf));
                ((GoalActionAsyncConcept)nar().concept(nt)).feedback($.t(nn, conf));
            }
        };

        GoalActionAsyncConcept p = new GoalActionAsyncConcept(pt, this, u);
        GoalActionAsyncConcept n = new GoalActionAsyncConcept(nt, this, u);
        addAction(p);
        addAction(n);
        //        return actionUnipolar(s, (f) -> {
//            if (f != f)
//                return Float.NaN;
//            else {
//                float y = (f - 0.5f) * 2f;
//                return (update.valueOf(y) / 2) + 0.5f;
//            }
//        });

//        addAction(new BeliefActionConcept(s, nar(), (t) -> {
//            float f;
//            if (t == null)
//                f = 0f;
//            else
//                f = (t.freq()-0.5f)*2f;
//            update.valueOf( f );
//        }));
    }

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

            if (f==f)
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
            return $.t(Util.unitize(ff), nar().confDefault(BELIEF));
        });
    }

}
