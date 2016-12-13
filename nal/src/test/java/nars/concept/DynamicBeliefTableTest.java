package nars.concept;

import nars.$;
import nars.NAR;
import nars.concept.dynamic.DynamicBeliefTable;
import nars.nar.Default;
import nars.term.Compound;
import nars.truth.DynTruth;
import nars.truth.Truth;
import org.junit.Test;

import static nars.$.$;
import static org.junit.Assert.*;

/**
 * Created by me on 10/27/16.
 */
public class DynamicBeliefTableTest {

    @Test
    public void testDynamicConjunction2() {
        NAR n = new Default();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.run(1);
        Truth now = n.concept($("(a:x && a:y)"), true).belief(n.time());
        assertEquals($.t(1f,0.81f),now);

        //n.ask("(a:x && a:y)")
    }

    @Test
    public void testDynamicConjunction3() {
        NAR n = new Default();
        n.believe("a:x", 1f, 0.9f);
        n.believe("a:y", 1f, 0.9f);
        n.believe("a:z", 1f, 0.9f);
        n.run(1);

        {
            Concept cc = n.concept($("(&&, a:x, a:y, a:z)"), true);
            Truth now = cc.belief(n.time());
            assertEquals($.t(1f, 0.73f), now);
            //the truth values were provided despite the belief tables being empty:
            assertTrue(cc.beliefs().isEmpty());
        }

        //test unknown:
        {
            Concept ccn = n.concept($("(&&, a:x, a:w)"), true);
            Truth nown = ccn.belief(n.time());
            assertNull(nown);
        }

        //test negation:
        {
            Concept ccn = n.concept($("(&&, a:x, (--, a:y), a:z)"), true);
            Truth nown = ccn.belief(n.time());
            assertEquals(ccn.toString(), $.t(0f, 0.73f), nown);
        }

        //test change after a component's revision:
        {
            n.believe("a:y", 0, 0.95f);
            n.run(1);
            Concept ccn = n.concept("(&&, a:x, a:y, a:z)");
            Truth now2 = ccn.belief(n.time());
            assertTrue(now2.freq() < 0.4f);
        }

    }

    @Test
    public void testDynamicConjunction2Temporal() {
        NAR n = new Default();
        n.believe($("(x)"), (long)0, 1f, 0.9f);
        n.believe($("(y)"), (long)4, 1f, 0.9f);
        n.run(2);
        CompoundConcept cc = (CompoundConcept) n.concept($("((x) && (y))"), true);
//        cc.print();
//
//
//        System.out.println("Instantaneous");
//        for (int i = -4; i <= n.time()+4; i++) {
//            System.out.println( i + ": " + cc.belief(i) );
//        }

        DynamicBeliefTable xtable = (DynamicBeliefTable) ((cc).beliefs());
        {
            Compound template = $("((x) &&+4 (y))");
            System.out.println(template);

            DynTruth xt = xtable.truth(0, template, true);
            assertNotNull(xt);
            assertEquals($.t(1f, 0.81f), xt.truth());

            for (int i = -4; i <= n.time() + 4; i++) {
                System.out.println(i + ": " + xtable.truth(i, template.dt(), true) + " " + xtable.generate(template, i));
            }
        }

        {
            long when = 0;
            for (int i = 0; i <= 8; i++) {
                Compound template = $("((x) &&+"+ i + " (y))");
                System.out.println( xtable.truth(when, template.dt(), true) + " " + xtable.generate(template, when));
            }

            assertEquals(0.81f, xtable.generate($("((x) &&+4 (y))"), 0).conf(), 0.01f); //best match to the input
            assertEquals(0.45f, xtable.generate($("((x) &&+2 (y))"), 0).conf(), 0.01f);
            assertEquals(0.23f, xtable.generate($("((x) &&+0 (y))"), 0).conf(), 0.01f);
        }


    }

    @Test public void testDynamicProductImageExtensional() {
        NAR n = new Default();

        n.believe($("f(x,y)"), (long)0, 1f, 0.9f);

        CompoundConcept prod = (CompoundConcept) n.concept($("f(x, y)"), false);
        Truth t = prod.belief(n.time());
        System.out.println(t);

        CompoundConcept imgX = (CompoundConcept) n.concept($("(x --> (/,f,_,y))"), true);
        assertTrue(imgX.beliefs().isEmpty());
        assertEquals(t, imgX.belief(n.time()));


        CompoundConcept imgY = (CompoundConcept) n.concept($("(y --> (/,f,x,_))"), true);
        assertTrue(imgY.beliefs().isEmpty());
        assertEquals(t, imgY.belief(n.time()));



        n.run(16); //by now, structural decomposition should have also had the opportunity to derive the image

        Truth t2 = prod.belief(n.time());

        assertEquals(t2, imgX.belief(n.time()));
        assertEquals(t2, imgY.belief(n.time()));

    }

}