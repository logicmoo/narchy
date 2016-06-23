package nars.gui;

import com.jogamp.opengl.*;
import nars.Global;
import nars.NAR;
import nars.concept.Concept;
import nars.truth.Truth;
import nars.truth.TruthWave;
import nars.util.JoglSpace2D;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.lang.Math.PI;
import static nars.nal.Tense.ETERNAL;


public class BeliefTableChart extends JoglSpace2D {

    final List<? extends Concept> c;
    final List<TruthWave> beliefs;
    final List<TruthWave> goals;

    final AtomicBoolean redraw;


    private final NAR nar;
    private long now;

    public BeliefTableChart(NAR n, Concept c) {
        this(n, Collections.singletonList(c));
    }

    public BeliefTableChart(NAR n, List<? extends Concept> c) {
        super();
        this.c =c;
        this.nar = n;

        redraw = new AtomicBoolean(false);

        beliefs = Global.newArrayList();
        goals = Global.newArrayList();
        for (int i = 0; i < c.size(); i++) {
            beliefs.add(new TruthWave(0));
            goals.add(new TruthWave(0));
        }

        //setAutoSwapBufferMode(true);

        n.onFrame(nn -> {
            update();
        });

        redraw.set(true);

    }


    protected void draw(GL2 gl, int n, float W, float H, long minT, long maxT) {

        float gew = H;
        float geh = H;

        float tew = W-H;
        float teh = H;

        TruthWave beliefs = this.beliefs.get(n);
        TruthWave goals = this.goals.get(n);




        try {
            if (!beliefs.isEmpty())
                renderTable(minT, maxT, now, gl, gew, geh, tew, teh, beliefs, beliefRenderer);
            if (!goals.isEmpty())
                renderTable(minT, maxT, now, gl, gew, geh, tew, teh, goals, goalRenderer);
        } catch (Throwable t) {
            //HACK
            t.printStackTrace();
        }

        //render current belief and goal states as crosshairs on eternal grid
        Truth bc = beliefs.current;
        long t = nar.time();

        float angleSpeed = 0.5f;
        if (bc!=null) {
            beliefTheta += bc.motivation() * angleSpeed;
            gl.glColor3f(1f,0.5f,0);
            drawCrossHair(gl, gew, geh, bc, t, beliefTheta);
        }

        Truth gc = goals.current;
        if (gc!=null) {
            goalTheta += gc.motivation() * angleSpeed;
            gl.glColor3f(0.5f,1f,0);
            drawCrossHair(gl, gew, geh, gc, t, goalTheta);
        }


        gl.glLineWidth(1f);
        gl.glColor4f(1f, 1f, 1f, 0.3f);
        strokeRect(gl, 0, 0, gew, geh);
        strokeRect(gl, gew, 0, tew, teh);

    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {

        GL2 gl = (GL2)glAutoDrawable.getGL();

        if (!redraw.compareAndSet(true, false)) {
            return;
        }

        //swapBuffers();

        float W = getWidth();
        float H = getHeight();

        //clear
        gl.glColor4f(0,0,0, 0.5f);
        gl.glRectf(0,0,W,H);

        int num = c.size();
        float dy = H / num;
        gl.glPushMatrix();

        //compute bounds from combined min/max of beliefs and goals so they align correctly
        long minT = Long.MAX_VALUE;
        long maxT = Long.MIN_VALUE;


        for (int i = 0; i < num; i++) {
            TruthWave b = this.beliefs.get(i);
            if (!b.isEmpty()) {
                long start = b.start();
                if (start!=ETERNAL) {
                    minT = Math.min(start, minT);
                    maxT = Math.max(b.end(), maxT);
                }
            }
            TruthWave g = this.goals.get(i);
            if (!g.isEmpty()) {

                long start = g.start();
                if (start != ETERNAL) {
                    minT = Math.min(start, minT);
                    maxT = Math.max(g.end(), maxT);
                }

            }
        }
        for (int i = 0; i < num; i++) {
            draw(gl, i, W, dy, minT, maxT);
            gl.glTranslatef(0,dy,0);
        }
        gl.glPopMatrix();

    }


//    final static ColorMatrix beliefColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.6f + 0.38f * c, 0.2f, 1f, 0.39f + 0.6f * c)
//    );
//    final static ColorMatrix goalColors = new ColorMatrix(8, 8, (f, c) ->
//            new Color(0.2f + 0.4f * c, 1f, 0.2f, 0.39f + 0.6f * c)
//    );

    //horizontal block
    final static TaskRenderer beliefRenderer = (ge, pri, c, w, h, x, y) -> {
        ge.glColor4f(0.3f + 0.7f * c, 0.25f, 0.25f, 0.25f + 0.5f * pri);
        rect(ge, x - w / 2, y - h / 4, w, h / 2);
    };
    //vertical block
    final static TaskRenderer goalRenderer = (ge, pri, c, w, h, x, y) -> {
        ge.glColor4f(0.25f, 0.3f + 0.7f * c, 0.25f, 0.25f + 0.5f * pri);
        rect(ge, x - w / 4, y - h / 2, w / 2, h);
    };


    double beliefTheta, goalTheta = PI/2;

    public void drawCrossHair(GL2 gl, float gew, float geh, Truth truth, long t, double theta) {
        float w = 4;
        gl.glLineWidth(w);

        float conf = truth.conf();


        float bcx = eternalX(gew, padding, w, conf);
        float bcy = yPos(geh, padding, w, truth.freq());

        //ge.strokeLine(bcx, border, bcx, geh - border);
        //ge.strokeLine(border, bcy, gew - border, bcy);
        double r = gew * (0.1 + (0.15 * conf));

        double dx0 = Math.cos(theta) * r;
        double dy0 = Math.sin(theta) * r;

        line(gl, dx0+bcx, dy0+bcy, -dx0+bcx, -dy0+bcy);
        double hpi = PI / 2.0;
        double dx1 = Math.cos(theta +hpi) * r;
        double dy1 = Math.sin(theta +hpi) * r;
        line(gl, dx1+bcx, dy1+bcy, -dx1+bcx, -dy1+bcy);
    }

    final float padding = 4;

    public void update() {



        this.now = nar.time();
        for (int i = 0; i < this.c.size(); i++) {
            Concept c = this.c.get(i);
            beliefs.get(i).set(c.beliefs(), now);
            goals.get(i).set(c.goals(), now);
        }

        ready();

    }

    @Override
    public void reshape(GLAutoDrawable ad, int i, int i1, int i2, int i3) {
        super.reshape(ad, i, i1, i2, i3);
        ready();
    }

    public void ready() {
        redraw.set(true);
    }

    private void renderTable(long minT, long maxT, long now, GL2 gl, float gew, float geh, float tew, float teh, TruthWave table, TaskRenderer r) {


        //Present axis line
        if ((now <= maxT) && (now >= minT)) {
            float nowLineWidth = 3;
            float nx = xTime(tew, padding, minT, maxT, now, nowLineWidth);

            gl.glColor4f(1f,1f,1f, 0.5f);
            rect(gl, gew + nx - nowLineWidth / 2f, 0, nowLineWidth, teh);
        }

        /** drawn "pixel" dimensions*/

        table.forEach((freq, conf, o, pri) -> {

            boolean eternal = !Float.isFinite(o);
            float eh, x;
            float padding = this.padding;
            float pw = 1f + gew/(1f/conf)/4f;//10 + 10 * conf;
            float ph = 1f + geh/(1f/conf)/4f;//10 + 10 * conf;

            if (eternal) {
                eh = geh;

                x = eternalX(gew, padding, pw, conf);
                //g = ge;
            } else {
                eh = teh;
                x = xTime(tew, padding, minT, maxT, o, pw);
                //g = te;
            }
            float y = yPos(eh, padding, ph, freq );
            if (!eternal)
                x += gew + padding;

            r.renderTask(gl, pri, conf, pw, ph, x, y);
        });

    }

    private static float yPos(float eh, float b, float h, float f) {
        return b + (eh - b - h) * (1 - f);
    }

    private static float eternalX(float width, float b, float w, float cc) {
        return b + (width - b - w) * cc;
    }

    @FunctionalInterface
    interface TaskRenderer {
        void renderTask(GL2 gl, float pri, float c, float w, float h, float x, float y);
    }


    private static float xTime(float tew, float b, float minT, float maxT, float o, float w) {
        float p = minT == maxT ? 0.5f : (o - minT) / (maxT - minT);
        return b + p * (tew - b - w);
    }



}
