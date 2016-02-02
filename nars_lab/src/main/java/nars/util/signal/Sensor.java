package nars.util.signal;

import com.gs.collections.api.block.function.primitive.FloatFunction;
import com.gs.collections.api.block.function.primitive.FloatToFloatFunction;
import nars.NAR;
import nars.Symbols;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.util.data.Util;

import java.util.function.Consumer;

/**
 * Created by me on 2/2/16.
 */
public class Sensor implements Consumer<NAR> {

    /**
     * resolution of the output freq value
     */
    static final float freqResolution = 0.05f;

    private final Term term;
    private final FloatFunction<Term> value;
    private final FloatToFloatFunction freq;
    private final NAR nar;
    private final float pri;
    private final float dur;
    private float conf;
    private float prevF;

    boolean inputIfSame = false;
    int maxTimeBetweenUpdates = 0;
    //TODO int minTimeBetweenUpdates..

    private long lastInput;

    public Sensor(NAR n, Term t, FloatFunction<Term> value, FloatToFloatFunction valueToFreq) {
        this(n, t, value, valueToFreq, n.memory.getDefaultConfidence(Symbols.JUDGMENT),
                n.memory.DEFAULT_JUDGMENT_PRIORITY, n.memory.DEFAULT_JUDGMENT_DURABILITY);
    }

    public Sensor(NAR n, Term t, FloatFunction<Term> value, FloatToFloatFunction valueToFreq, float conf, float pri, float dur) {
        this.nar = n;
        this.term = t;
        n.onFrame(this);
        this.value = value;
        this.freq = valueToFreq;
        this.conf = conf;

        this.pri = pri;
        this.dur = dur;
        this.lastInput = n.time() - 1;

        this.prevF = Float.NaN;
    }

    @Override
    public void accept(NAR nar) {

        int timeSinceLastInput = (int)(nar.time() - lastInput);


        double nextD = value.floatValueOf(term);
        if (!Double.isFinite(nextD))
            return; //allow the value function to prevent input by returning NaN
        float next = (float) nextD;

        float fRaw = freq.valueOf(next);
        if (!Float.isFinite(fRaw))
            return; //allow the frequency function to prevent input by returning NaN



        float f = Util.round(fRaw, freqResolution);
        if (inputIfSame || (f != prevF) || (maxTimeBetweenUpdates !=0 && timeSinceLastInput>= maxTimeBetweenUpdates)) {
            Task t = input(f);
            this.lastInput = t.creation();
        }

        this.prevF = f;

        //this.prevValue = next;
    }

    private Task input(float f) {
        Task t = new MutableTask(term).belief().truth(f, conf).present(nar.time()).budget(pri, dur);
        nar.input(t);
        return t;
    }

    /** sets default confidence */
    public Sensor conf(float conf) {
        this.conf = conf;
        return this;
    }

    /** sets minimum time between updates, even if nothing changed. zero to disable this */
    public Sensor maxTimeBetweenUpdates(int minTimeBetweenUpdates) {
        this.maxTimeBetweenUpdates = minTimeBetweenUpdates;
        return this;
    }

    //        public void on() {
//
//        }
//        public void off() {
//
//        }
}
