package nars.agent;

import jcog.signal.wave2d.ArrayBitmap2D;
import nars.$;
import nars.NAR;
import nars.NARS;
import nars.concept.TaskConcept;
import nars.sensor.Bitmap2DSensor;
import nars.term.Term;
import nars.truth.Truth;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static nars.$.$$;
import static org.junit.jupiter.api.Assertions.*;


public class CameraSensorTest {

    @Test
    public void test1() {
        int w = 2, h = 2;

        NAR n = NARS.tmp();
        float[][] f = new float[w][h];

        Bitmap2DSensor c = new Bitmap2DSensor($.the("x"),
                new ArrayBitmap2D(f), n);


        n.log();

        //check for cause applied
        final int[] causesDetected = {0};
        Term aPixel = $$("x(0,0)");
        short[] cause = new short[] { c.in.id };
        n.onTask(t -> {
           if (t.term().equals(aPixel)) {
               assertArrayEquals(cause, t.cause());
               causesDetected[0]++;
           }
        });


        System.out.println("all 0");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 0f;
        c.input();
        n.run(1);
        assertEquals(c, f, n.time(),n);


        n.run(3);


        System.out.println("all 1");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 1f;
        c.input();
        n.run(3);  
        assertEquals(c, f, n.time(),n);


        System.out.println("all 0");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 0f;
        c.input();
        n.run(1);
        assertEquals(c, f, n.time(), n);

        n.run(3); 

        System.out.println("all 1");
        for (int i = 0; i < w; i++) for (int j = 0; j < h; j++) f[i][j] = 1f;
        c.input();
        n.run(1);
        assertEquals(c, f, n.time(), n);

        assertTrue(causesDetected[0] > 0);



    }

    static final float tolerance = 0.47f;

    static void assertEquals(Bitmap2DSensor c, float[][] f, long when, NAR n) {
        for (int i = 0; i < c.width; i++) {
            for (int j = 0; j < c.height; j++) {
                TaskConcept p = c.get(i, j);
                Truth t = n.beliefTruth(p, when);
                if (t == null || Math.abs(f[i][j] - t.freq()) > tolerance) {
                    System.err.println("pixel " + p + " incorrect @ t=" + n.time());
                    n.beliefTruth(p, n.time());
                    p.beliefs().print(System.out);
                }
                assertNotNull(t, ()->p.term + " is null");
                Assertions.assertEquals(f[i][j], t.freq(), tolerance, ()->p + " has inaccurate result @ t=" + n.time());
            }
        }
    }

}