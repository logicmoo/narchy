package nars.concept.action;

import jcog.Util;
import jcog.math.FloatAveraged;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.agent.NAct;
import nars.agent.NSense;
import nars.concept.TaskConcept;
import nars.concept.sensor.AbstractSensor;
import nars.control.channel.CauseChannel;
import nars.link.TemplateTermLinker;
import nars.link.TermLinker;
import nars.task.ITask;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.PreciseTruth;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.util.List;
import java.util.Random;

import static nars.Op.BELIEF;
import static nars.Op.SETe;
import static nars.agent.NAct.NEG;
import static nars.agent.NAct.PLUS;

/** sensor that integrates and manages a pair of oppositely polarized AsyncActionConcept to produce a net result.
 * implements Sensor but actually manages Actions internally. */
public class BiPolarAction extends AbstractSensor {

    private final CauseChannel<ITask> feedback;
    private final Polarization model;
    private final FloatToFloatFunction motor;

    /** model for computing the net result from the current truth inputs */
    public interface Polarization {

        /** produce a value in -1..+1 range, or NaN if undetermined */
        float update(Truth pos, Truth neg, long prev, long now);
    }

    public final AsyncActionConcept pos, neg;

    public BiPolarAction(BooleanToObjectFunction<Term> id, Polarization model, FloatToFloatFunction motor, NAR nar) {
        this(id.valueOf(true), id.valueOf(false), model, motor, nar);
    }
    public BiPolarAction(Term id, Polarization model, FloatToFloatFunction motor, NAR nar) {
        this(posOrNeg -> $.p(id, posOrNeg ? PLUS : NEG), model, motor, nar);
    }

    //TODO BooleanObjectFunction<Term> term namer
    public BiPolarAction(Term pos, Term neg, Polarization model, FloatToFloatFunction motor, NAR nar) {
        super(SETe.the(pos, neg), nar);

        feedback = nar.newChannel(id);

        TermLinker sharedLinker = TemplateTermLinker.of(id); //so they cross-activate
        this.pos = new AsyncActionConcept(pos, sharedLinker, nar);
        this.neg = new AsyncActionConcept(neg, sharedLinker, nar);

        this.model = model;
        this.motor = motor;

    }

    protected Truth truther(TaskConcept x, long prev, long now, long next, NAR nar) {
        //a.
        {
            Truth y = x.goals().truth(prev, now, null, nar);
            if (y != null)
                return y;
        }

//        //b.
//        {
//            Truth y = x.goals().truth(prev, next, null, nar);
//            if (y != null)
//                return y;
//        }

        //c.
//        {
//            Truth y = x.beliefs().truth(now, next, null, nar); //self fulfilling prophecy
//            if (y!=null)
//                return y;
//        }

        return null;
    }

    @Override
    public void update(long prev, long now, long next, NAR nar) {
        Truth p = truther(pos, prev, now, next, nar);
        Truth n = truther(neg, prev, now, next, nar);
        float x = model.update(p, n, prev, now);
        if (x==x)
            x = Util.clamp(x, -1f, +1f);

        float y = motor.valueOf(x);

        //TODO configurable feedback model

        PreciseTruth Nb, Ng, Pb, Pg;

        if (y == y) {

            y = Util.clamp(y, -1, +1);

            float yp, yn;
            yp = 0.5f + y / 2f;
            yn = 1f - yp;

//            if ((p == null && n == null) /* curiosity */ || (p!=null && n!=null) /* both active */) {
//                float zeroThresh = ScalarValue.EPSILON;
//                if (y >= zeroThresh) {
//                    yp = y;
//                    yn = Float.NaN;
//                } else if (y <= -zeroThresh) {
//                    yn = -y;
//                    yp = Float.NaN;
//                } else {
//                    yp = yn = Float.NaN;
//                }
//            }  else if (p!=null) {
//                yp = Math.max(0, y);
//                yn = Float.NaN;
//            } else if (n!=null) {
//                yn = Math.max(0, -y);
//                yp = Float.NaN;
//            } else {
//                throw new UnsupportedOperationException();
//            }


            float feedbackConf = nar.confDefault(BELIEF);

            Pb = yp == yp ? $.t(yp, feedbackConf) : null;
            Nb = yn == yn ? $.t(yn, feedbackConf) : null;

            Pg = null;
            Ng = null;

        } else {
            Pb = Nb = Pg = Ng = null;
        }


        feedback.input(pos.feedback(Pb, Pg, now, next, nar), neg.feedback(Nb, Ng, now, next, nar));
    }

    @Override
    public Iterable<Termed> components() {
        return List.of(pos, neg);
    }


    /** offers a few parameters */
    public static class DefaultPolarization implements Polarization {

        final float[] lastX = new float[] { 0};

        final FloatSupplier curiosity; //HACK
        private final NAR nar;

        boolean freqOrExp;

        /** how much coherence can shrink the amplitude of the resulting bipolar signal. 0 means not at all, +1 means fully attenuable */
        private float coherenceRange = 0.5f;
        private final boolean fair;

        boolean latch = true;

        /** adjustable q+ lowpass filters */
        final FloatAveraged fp = new FloatAveraged(0.99f, true);
        /** adjustable q- lowpass filters */
        final FloatAveraged fn = new FloatAveraged(0.99f, true);

        public DefaultPolarization(boolean fair, NSense s) {
            this.fair = fair;
            curiosity = ((NAct) s).curiosity();
            freqOrExp = true;
            this.nar = s.nar();
        }

        @Override
        public float update(Truth pos, Truth neg, long prev, long now) {
            float pq = q(pos);
            float nq = q(neg);

            //fill in missing NaN values
            float pg, ng;
            if (pq!=pq) pg = latch ? fp.floatValue() : 0;
            else pg = fp.valueOf(pq);
            if (nq!=nq) ng = latch ? fn.floatValue() : 0;
            else ng = fn.valueOf(nq);


            float x = (pg - ng);

            //System.out.println(pg + "|" + ng + "=" + x);

            if (fair) {
                float pe = c(pos), ne = c(neg);
                float cMax = Math.max(pe, ne);
                float cMin = Math.min(pe, ne);
                float coherence = (cMax > Float.MIN_NORMAL) ? cMin / cMax : 0;

                assert(coherence <= 1f): "strange coherence=" + coherence;

                x = Util.lerp(coherenceRange, coherence * x, x);


            }

            if (Float.isFinite(x)) x = Util.clamp(motorization(x), -1, +1);
            else x = Float.NaN;

            lastX[0] = x;

            return x;

        }

        /** calculates the effective output motor value returned by the model.
         * input and output to this function are in the domain/range of -1..+1
         * a default linear response is implemented here. */
        public float motorization(float input) {
            return input;
        }

        /** confidence/evidence strength.  could be truth conf or evidence, zero if null. used in determining coherence */
        public float c(Truth t) {
            return t != null ? t.conf() : 0;
        }

        /** "Q" desire/value function. produces the scalar summary of the goal truth desire that will be
         * used in the difference comparison. return NaN or value in range -1..+1 */
        public float q(Truth t) {
            Random rng = nar.random();
            float cur = curiosity.asFloat();

            if (cur > 0 && rng.nextFloat() <= cur/2 /* shared between both */) {
                return (rng.nextFloat() - 0.5f) * 2;
            }

            return t != null ? ((freqOrExp ? t.freq() : t.expectation()) - 0.5f)*2 : Float.NaN;
        }

    }
}
