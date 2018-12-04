package nars.attention;

import nars.NAR;
import nars.control.DurService;

/** abstract attention economy model */
public abstract class Attention extends DurService {


    public Activator activating;

    public Forgetting forgetting;


    protected Attention(Activator activating, Forgetting forgetting) {
        super((NAR)null);
        this.activating = activating;
        this.forgetting = forgetting;
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        on(
            nar.onCycle(this::cycle)
        );
    }

    @Override
    protected void run(NAR n, long dt) {

        forgetting.update(nar);

    }

    private void cycle() {

        activating.update(nar);
    }
}