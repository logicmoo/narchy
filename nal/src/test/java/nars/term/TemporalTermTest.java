package nars.term;

import nars.*;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.concept.util.DefaultConceptBuilder;
import nars.util.term.transform.Retemporalize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.TreeSet;

import static nars.$.*;
import static nars.Op.False;
import static nars.Op.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;
import static org.junit.jupiter.api.Assertions.*;


public class TemporalTermTest {


    final NAR n = NARS.shell();
    @Nullable
    final Term A = the("a");
    @Nullable
    final Term B = the("b");

    static Term ceptualStable(String s) throws Narsese.NarseseException {
        Term c = $(s);
        Term c1 = c.concept();
        
        Term c2 = c1.concept();
        assertEquals(c1, c2, () -> "unstable: irst " + c1 + "\n\t, then " + c2);
        return c1;
    }

    static void assertConceptual(String cexp, String c) throws Narsese.NarseseException {
        Term p = $(c);
        assertEquals(cexp, p.concept().toString());
    }

    @Test
    public void testCoNegatedSubtermConceptConj() throws Narsese.NarseseException {
        assertEquals("((x) &&+- (x))", n.conceptualize($("((x) &&+10 (x))")).toString());

        assertEquals("((--,(x)) &&+- (x))", n.conceptualize($("((x) &&+10 (--,(x)))")).toString());
        assertEquals("((--,(x)) &&+- (x))", n.conceptualize($("((x) &&-10 (--,(x)))")).toString());



    }

    @Test
    public void testCoNegatedSubtermConceptImpl() throws Narsese.NarseseException {
        assertEquals("((x) ==>+- (x))", n.conceptualize($("((x) ==>+10 (x))")).toString());
        assertEquals("((--,(x)) ==>+- (x))", n.conceptualize($("((--,(x)) ==>+10 (x))")).toString());

        Term xThenNegX = $("((x) ==>+10 (--,(x)))");
        assertEquals("((x) ==>+- (x))", n.conceptualize(xThenNegX).toString());

        assertEquals("((x) ==>+- (x))", n.conceptualize($("((x) ==>-10 (--,(x)))")).toString());

    }

    @Test
    public void testCoNegatedSubtermTask() throws Narsese.NarseseException {

        
        assertNotNull(Narsese.the().task("((x) &&+1 (--,(x))).", n));

        
        assertInvalidTask("((x) && (--,(x))).");
        assertInvalidTask("((x) &&+0 (--,(x))).");
    }

    @Test
    @Disabled
    public void testInvalidInheritanceOfEternalAndItsTemporal() throws Narsese.NarseseException {
        assertEquals(
                Op.True,
                $("((a &&+1 b)-->(a && b))")
        );

        assertEquals(
                Op.True,
                $("(--(a &&+1 b) --> --(a && b))")
        );

        assertEquals(
                Op.True,
                $("((a && b)-->(a &&+1 b))")
        );

        assertEquals(
                Op.True,
                $("((a && b)<->(a &&+1 b))")
        );
    }

    @Test
    public void testInvalidInheritanceOfEternalTemporalNegated() throws Narsese.NarseseException {
        assertEquals( 
                "((--,(a &&+1 b))-->(a&&b))",
                $("(--(a &&+1 b)-->(a && b))").toString()
        );
        assertEquals( 
                "((a &&+1 b)-->(--,(a&&b)))",
                $("((a &&+1 b) --> --(a && b))").toString()
        );

    }

    public void assertInvalidTask(@NotNull String ss) {
        try {
            Narsese.the().task(ss, n);
            fail("");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testEventsWithRepeatParallel() throws Narsese.NarseseException {

        Term ab = $("(a&|b)");
        assertEquals("[0:a, 0:b]",
                ab.eventList().toString());
        assertEquals(2, ab.eventCount());

        Term abc = $("((a&|b) &&+5 (b&|c))");
        assertEquals(
                
                
                "[0:a, 0:b, 5:b, 5:c]",
                abc.eventList().toString());
        assertEquals(4, abc.eventCount());

    }

    @Test
    public void testStableConceptualization1() throws Narsese.NarseseException {
        Term c1 = ceptualStable("((((#1,(2,true),true)-->#2)&|((gameOver,(),true)-->#2)) &&+29 tetris(#1,(11,true),true))");
        assertEquals("(&&,((#1,(2,true),true)-->#2),tetris(#1,(11,true),true),((gameOver,(),true)-->#2))", c1.toString());
    }

    @Test public void testStableNormalizationAndConceptualizationComplex() {
        String s = "(((--,checkScore(#1))&|#1) &&+14540 ((--,((#1)-->$2)) ==>+17140 (isRow(#1,(0,false),true)&|((#1)-->$2))))";
        Term t = $$(s);
        assertEquals(s, t.toString());
        assertEquals(s, t.normalize().toString());
        assertEquals(s, t.normalize().normalize().toString());
        String c = "(&&,((--,((#1)-->$2)) ==>+- (isRow(#1,(0,false),true)&&((#1)-->$2))),(--,checkScore(#1)),#1)";
        assertEquals(c, t.concept().toString());
        assertEquals(c, t.concept().concept().toString());
        Concept x = new DefaultConceptBuilder().build(t);
        assertTrue(x instanceof TaskConcept);
    }







    @Test
    public void testConceptualizationWithoutConjReduction() throws Narsese.NarseseException {
        String s = "((--,((happy-->#1) &&+345 (#1,zoom))) &&+1215 (--,((#1,zoom) &&+10 (happy-->#1))))";
        assertEquals("((--,((happy-->#1)&&(#1,zoom))) &&+- (--,((happy-->#1)&&(#1,zoom))))",
                $(s).concept().toString());
    }

    @Test
    public void testConceptualizationWithoutConjReduction2() throws Narsese.NarseseException {
        String s = "(((--,((--,(joy-->tetris))&|#1)) &&+30 #1) &&+60 (joy-->tetris))";
        assertEquals("(((--,((--,(joy-->tetris))&&#1)) &&+- #1)&&(joy-->tetris))",
                $(s).concept().toString());
    }

    @Test
    public void testStableConceptualization6a() throws Narsese.NarseseException {
        Term s = $.$("((tetris($1,#2) &&+290 tetris(isRow,(8,false),true))=|>(tetris(checkScore,#2)&|tetris($1,#2)))");
        assertEquals("((tetris(isRow,(8,false),true)&&tetris($1,#2)) ==>+- (tetris(checkScore,#2)&&tetris($1,#2)))", s.concept().toString());
    }

    @Test
    public void testStableConceptualization2() throws Narsese.NarseseException {
        Term c1 = ceptualStable("(((a)&&(b))&|do(that))");
        assertEquals(
                "(((a)&&(b)) &&+- do(that))",
                
                c1.toString());
    }

    @Test
    public void testStableConceptualization0() throws Narsese.NarseseException {
        Term c1 = ceptualStable("((a &&+5 b) &&+5 c)");
        assertEquals("(&&,a,b,c)", c1.toString());
    }

    @Test
    public void testStableConceptualization4() throws Narsese.NarseseException {
        Term c1 = ceptualStable("((--,((#1-->happy)&|(#1-->neutral)))&|(--,(#1-->sad)))");
        assertEquals("((--,((#1-->happy)&&(#1-->neutral)))&&(--,(#1-->sad)))", c1.toString());
    }

    @Test
    public void testStableConceptualization5() throws Narsese.NarseseException {
        Term c1 = ceptualStable("((((--,a)&&b) &&+- a)&&(--,b))");
        assertEquals("((((--,a)&&b) &&+- a)&&(--,b))", c1.toString());
    }

    @Test
    public void testStableConceptualization6() throws Narsese.NarseseException {
        assertEquals("(&&,(--,(\"-\"-->move)),(--,(joy-->cart)),(happy-->cart),(\"+\"-->move))",
                ceptualStable("((((--,(\"-\"-->move))&|(happy-->cart)) &&+334 (\"+\"-->move)) &&+5 (--,(joy-->cart)))").toString());
    }

    @Test
    public void testEventsWithXTERNAL() throws Narsese.NarseseException {
        
        assertEquals("[0:(x &&+- y)]", $("(x &&+- y)").eventList().toString());
        assertEquals("[0:(x &&+- y), 1:z]", $("((x &&+- y) &&+1 z)").eventList().toString());
    }

    @Test
    public void testEventsWithDTERNAL() throws Narsese.NarseseException {
        
        assertEquals("[0:(x&&y)]", $("(x && y)").eventList().toString());
        assertEquals("[0:(x&&y), 1:z]", $("((x && y) &&+1 z)").eventList().toString());
    }

    @Test
    public void testAtemporalization() throws Narsese.NarseseException {
        Term t = $("((x) ==>+10 (y))");
        Concept c = n.conceptualize(t);
        assertEquals("((x)==>(y))", c.toString());
    }

    @Test
    public void testAtemporalization2() throws Narsese.NarseseException {

        assertEquals("((--,y) &&+- y)", $.<Compound>$("(y &&+3 (--,y))").temporalize(Retemporalize.retemporalizeAllToXTERNAL).toString());
    }

    @Test
    public void testAtemporalization3a() throws Narsese.NarseseException {

        assertEquals(
                "(--,((x &&+- $1) ==>+- ((--,y) &&+- $1)))",
                $.<Compound>$("(--,(($1&&x) ==>+1 ((--,y) &&+2 $1)))").temporalize(Retemporalize.retemporalizeAllToXTERNAL).toString());
        assertEquals(
                "(--,((x&&$1) ==>+- ((--,y)&&$1)))",
                $.<Compound>$("(--,(($1&&x) ==>+1 ((--,y) &&+2 $1)))").root().toString());
    }

    @Test
    public void testAtemporalization3b() throws Narsese.NarseseException {

        Compound x = $("((--,(($1&&x) ==>+1 ((--,y) &&+2 $1))) &&+3 (--,y))");
        Term y = x.temporalize(Retemporalize.retemporalizeAllToXTERNAL);
        assertEquals("((--,((x &&+- $1) ==>+- ((--,y) &&+- $1))) &&+- (--,y))", y.toString());

    }

    @Test
    public void testAtemporalization4() throws Narsese.NarseseException {
        

        assertEquals("((x&&$1) ==>+- (y&&$1))",
                $("((x&&$1) ==>+- (y&&$1))").root().toString());
    }

    @Disabled
    @Test /* TODO decide the convention */
    public void testAtemporalization5() throws Narsese.NarseseException {
        for (String s : new String[]{"(y &&+- (x ==>+- z))", "((x ==>+- y) &&+- z)"}) {
            Term c = $(s);
            assertTrue(c instanceof Compound);
            assertEquals("((x &&+- y) ==>+- z)",
                    c.toString());
            assertEquals("((x && y) ==>+- z)",
                    c.root().toString());



        }
    }

    @Test
    public void testConjSorting() throws Narsese.NarseseException {
        Term ea = $("(x&&$1)");
        assertEquals("(x&&$1)", ea.toString());
        Term eb = $("($1&&x)");
        assertEquals("(x&&$1)", eb.toString());
        Term pa = $("(x&|$1)");
        assertEquals("(x&|$1)", pa.toString());
        Term pb = $("($1&|x)");
        assertEquals("(x&|$1)", pb.toString());
        Term xa = $("($1 &&+- x)");
        assertEquals("(x &&+- $1)", xa.toString());
        Term xb = $("(x &&+- $1)");
        assertEquals("(x &&+- $1)", xb.toString());

        assertEquals(ea, eb);
        assertEquals(ea.dt(), eb.dt());
        assertEquals(ea.subterms(), eb.subterms());

        assertEquals(pa, pb);
        assertEquals(pa.dt(), pb.dt());
        assertEquals(ea.subterms(), pa.subterms());
        assertEquals(ea.subterms(), pb.subterms());

        assertEquals(xa, xb);
        assertEquals(xa.dt(), xb.dt());
        assertEquals(ea.subterms(), xa.subterms());
        assertEquals(ea.subterms(), xb.subterms());
    }

    @Test
    public void testAtemporalization6() throws Narsese.NarseseException {
        Compound x0 = $("(($1&&x) ==>+1 ((--,y) &&+2 $1)))");
        assertEquals("((x&&$1) ==>+1 ((--,y) &&+2 $1))", x0.toString());

    }

    @Test
    public void testAtemporalizationSharesNonTemporalSubterms() throws Narsese.NarseseException {

        Task a = n.inputTask("((x) ==>+10 (y)).");
        Task c = n.inputTask("((x) ==>+9 (y)).");
        Task b = n.inputTask("((x) <-> (y)).");
        n.run();

        @NotNull Term aa = a.term();
        assertNotNull(aa);

        @Nullable Concept na = a.concept(n, true);
        assertNotNull(na);

        @Nullable Concept nc = c.concept(n, true);
        assertNotNull(nc);

        assertSame(na, nc);

        assertSame(na.sub(0), nc.sub(0));




        assertEquals(b.concept(n, true).sub(0), c.concept(n, true).sub(0));

    }

    @Test
    public void testHasTemporal() throws Narsese.NarseseException {
        assertTrue($("(?x &&+1 y)").isTemporal());
    }

    @Test
    public void testParseOperationInFunctionalForm2() throws Narsese.NarseseException {
        assertEquals(
                "(((a)&&(b))&|do(that))",
                
                $("(do(that) &&+0 ((a)&&(b)))").toString());

        Termed nt = $("(((that)-->do) &&+0 ((a)&&(b)))");
        assertEquals(
                "(((a)&&(b))&|do(that))",
                
                nt.toString());

        
        assertEquals(
                
                "(((a)&&(b)) &&+- do(that))",
                n.conceptualize(nt).toString(), () -> nt + " conceptualized");

        

    }

    @Test
    public void testAnonymization2() throws Narsese.NarseseException {
        Termed nn = $("((do(that) &&+1 (a)) ==>+2 (b))");
        assertEquals("((do(that) &&+1 (a)) ==>+2 (b))", nn.toString());


        assertEquals("((do(that)&&(a))==>(b))", n.conceptualize(nn).toString());

        

    }

    @Test
    public void testRetemporalization1() throws Narsese.NarseseException {
        assertEquals("a(x,(--,((--,((6-->ang) &&+1384 (6-->ang)))&&(6-->ang))))",
                $("a(x,(--,((--,((6-->ang) &&+1384 (6-->ang))) &&+- (6-->ang))))").temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL).toString()
        );
    }
    @Test
    public void testConjEtePara() {
        assertEquals("((a&|b)&&(b&|c))",
                $$("((a&|b)&&(b&|c))").toString()
        );
    }
    @Test
    public void testCommutiveTemporalityConjEquiv() {



        testParse("((#1-->$2) &&-20 ({(row,3)}-->$2))", "(({(row,3)}-->$1) &&+20 (#2-->$1))");
    }

    @Test
    public void testCommutiveTemporalityConjEquiv2() {
        testParse("(({(row,3)}-->$2) &&+20 (#1-->$2))", "(({(row,3)}-->$1) &&+20 (#2-->$1))");
    }

    @Test
    public void testCommutiveTemporalityConj2() {
        testParse("(goto(a) &&+5 ((SELF,b)-->at))", "(goto(a) &&+5 at(SELF,b))");
    }

    @Test
    public void testCommutiveConjTemporal() throws Narsese.NarseseException {
        Term x = $("(a &&+1 b)");
        assertEquals("a", x.sub(0).toString());
        assertEquals("b", x.sub(1).toString());
        assertEquals(+1, x.dt());
        assertEquals("(a &&+1 b)", x.toString());

        Term y = $("(a &&-1 b)");
        assertEquals("a", y.sub(0).toString());
        assertEquals("b", y.sub(1).toString());
        assertEquals(-1, y.dt());
        assertEquals("(b &&+1 a)", y.toString());

        Term z = $("(b &&+1 a)");
        assertEquals("a", z.sub(0).toString());
        assertEquals("b", z.sub(1).toString());
        assertEquals(-1, z.dt());
        assertEquals("(b &&+1 a)", z.toString());

        Term w = $("(b &&-1 a)");
        assertEquals("a", w.sub(0).toString());
        assertEquals("b", w.sub(1).toString());
        assertEquals(+1, w.dt());
        assertEquals("(a &&+1 b)", w.toString());

        assertEquals(y, z);
        assertEquals(x, w);

    }

    @Test
    public void testCommutiveTemporality1() {
        testParse("(goto(a)&&((SELF,b)-->at))", "(at(SELF,b)&&goto(a))");
        testParse("(goto(a)&|((SELF,b)-->at))", "(at(SELF,b)&|goto(a))");
        testParse("(at(SELF,b) &&+5 goto(a))", "(at(SELF,b) &&+5 goto(a))");
    }

    @Test
    public void testCommutiveTemporality2() {
        testParse("(at(SELF,b)&&goto(a))");
        testParse("(at(SELF,b)&|goto(a))");
        testParse("(at(SELF,b) &&+5 goto(a))");
        testParse("(goto(a) &&+5 at(SELF,b))");
    }

    @Test
    public void testCommutiveTemporalityDepVar0() throws Narsese.NarseseException {
        Term t0 = $("((SELF,#1)-->at)").term();
        Term t1 = $("goto(#1)").term();
        Term[] a = Terms.sorted(t0, t1);
        Term[] b = Terms.sorted(t1, t0);
        assertEquals(
                Op.terms.newSubterms(a),
                Op.terms.newSubterms(b)
        );
    }

    @Test
    public void testCommutiveTemporalityDepVar1() {
        testParse("(goto(#1) &&+5 at(SELF,#1))");
    }

    @Test
    public void testCommutiveTemporalityDepVar2() {
        testParse("(goto(#1) &&+5 at(SELF,#1))", "(goto(#1) &&+5 at(SELF,#1))");
        testParse("(goto(#1) &&-5 at(SELF,#1))", "(at(SELF,#1) &&+5 goto(#1))");
    }

    void testParse(String s) {
        testParse(s, null);
    }

    void testParse(String input, String expected) {
        Termed t = null;
        try {
            t = $(input);
        } catch (Narsese.NarseseException e) {
            fail(e);
        }
        if (expected == null)
            expected = input;
        assertEquals(expected, t.toString());
    }

    @Test
    public void testCommutiveTemporalityConcepts() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        n.log();

        n.input("(goto(#1) &&+5 ((SELF,#1)-->at)).");
        

        n.input("(goto(#1) &&-5 ((SELF,#1)-->at)).");
        

        n.input("(goto(#1) &&+0 ((SELF,#1)-->at)).");
        
        n.input("(((SELF,#1)-->at) &&-3 goto(#1)).");
        
        
        
        


        n.run(2);

        TaskConcept a = (TaskConcept) n.conceptualize("(((SELF,#1)-->at) && goto(#1)).");
        Concept a0 = n.conceptualize("(goto(#1) && ((SELF,#1)-->at)).");
        assertNotNull(a);
        assertSame(a, a0);


        a.beliefs().print();

        assertTrue(a.beliefs().size() >= 4);
    }

    @Test
    public void testCommutiveTemporalityConcepts2() throws Narsese.NarseseException {
        NAR n = NARS.shell();

        for (String op : new String[]{"&&"}) {
            Concept a = n.conceptualize($("(x " + op + "   y)"));
            Concept b = n.conceptualize($("(x " + op + "+1 y)"));

            assertSame(a, b);

            Concept c = n.conceptualize($("(x " + op + "+2 y)"));

            assertSame(b, c);

            Concept d = n.conceptualize($("(x " + op + "-1 y)"));

            assertSame(c, d);

            Term e0 = $("(x " + op + "+- y)");
            assertEquals("(x " + op + "+- y)", e0.toString());
            Concept e = n.conceptualize(e0);

            assertSame(d, e);

            Term f0 = $("(y " + op + "+- x)");
            assertEquals("(x " + op + "+- y)", f0.toString());
            assertEquals("(x" + op + "y)", f0.root().toString());

            Concept f = n.conceptualize(f0);
            assertSame(e, f, e + "==" + f);

            
            Concept g = n.conceptualize($("(x " + op + "+- x)"));
            assertEquals("(x " + op + "+- x)", g.toString());

            
            Concept h = n.conceptualize($("(x " + op + "+- (--,x))"));
            assertEquals("((--,x) " + op + "+- x)", h.toString());


        }

    }

    @Test
    public void parseTemporalRelation() throws Narsese.NarseseException {
        
        assertEquals("(x ==>+5 y)", $("(x ==>+5 y)").toString());
        assertEquals("(x &&+5 y)", $("(x &&+5 y)").toString());

        assertEquals("(x ==>-5 y)", $("(x ==>-5 y)").toString());

        assertEquals("((before-->x) ==>+5 (after-->x))", $("(x:before ==>+5 x:after)").toString());
    }

    @Test
    public void temporalEqualityAndCompare() throws Narsese.NarseseException {
        assertNotEquals($("(x ==>+5 y)"), $("(x ==>+0 y)"));
        assertNotEquals($("(x ==>+5 y)").hashCode(), $("(x ==>+0 y)").hashCode());
        assertNotEquals($("(x ==> y)"), $("(x ==>+0 y)"));
        assertNotEquals($("(x ==> y)").hashCode(), $("(x ==>+0 y)").hashCode());

        assertEquals($("(x ==>+0 y)"), $("(x ==>-0 y)"));
        assertNotEquals($("(x ==>+5 y)"), $("(y ==>-5 x)"));


        assertEquals(0, $("(x ==>+0 y)").compareTo($("(x ==>+0 y)")));
        assertEquals(-1, $("(x ==>+0 y)").compareTo($("(x ==>+1 y)")));
        assertEquals(+1, $("(x ==>+1 y)").compareTo($("(x ==>+0 y)")));
    }

    @Test
    public void testReversibilityOfCommutive() throws Narsese.NarseseException {
        for (String c : new String[]{"&&"/*, "<=>"*/}) {
            assertEquals("(a " + c + "+5 b)", $("(a " + c + "+5 b)").toString());
            assertEquals("(b " + c + "+5 a)", $("(b " + c + "+5 a)").toString());
            assertEquals("(a " + c + "+5 b)", $("(b " + c + "-5 a)").toString());
            assertEquals("(b " + c + "+5 a)", $("(a " + c + "-5 b)").toString());

            assertEquals($("(b " + c + "-5 a)"), $("(a " + c + "+5 b)"));
            assertEquals($("(b " + c + "+5 a)"), $("(a " + c + "-5 b)"));
            assertEquals($("(a " + c + "-5 b)"), $("(b " + c + "+5 a)"));
            assertEquals($("(a " + c + "+5 b)"), $("(b " + c + "-5 a)"));
        }
    }

    @Test
    public void testCommutiveWithCompoundSubterm() throws Narsese.NarseseException {
        Term a = $("(((--,(b0)) &&+0 (pre_1)) &&+10 (else_0))");
        Term b = $("((else_0) &&-10 ((--,(b0)) &&+0 (pre_1)))");
        assertEquals(a, b);

        Term c = seq($("((--,(b0)) &&+0 (pre_1))"), 10, $("(else_0)"));
        Term d = seq($("(else_0)"), -10, $("((--,(b0)) &&+0 (pre_1))"));






        assertEquals(b, c);
        assertEquals(c, d);
        assertEquals(a, c);
        assertEquals(a, d);
    }

    @Test
    public void testConceptualization() throws Narsese.NarseseException {

        Term t = $("(x==>y)");
        Term x = t.root();
        assertEquals(DTERNAL, x.dt());
        assertEquals("(x==>y)", x.toString());

        n.input("(x ==>+0 y).", "(x ==>+1 y).").run(2);

        TaskConcept xImplY = (TaskConcept) n.conceptualize(t);
        assertNotNull(xImplY);

        assertEquals("(x==>y)", xImplY.toString());

        assertEquals(3, xImplY.beliefs().size());

        int indexSize = n.concepts.size();
        n.concepts.print(System.out);

        n.input("(x ==>+1 y). :|:"); 
        n.run();

        

        assertEquals(4, xImplY.beliefs().size());

        n.concepts.print(System.out);
        assertEquals(indexSize, n.concepts.size()); 

        n.conceptualize("(x==>y)").print();
    }

    @Test
    public void testEmbeddedChangedRoot() throws Narsese.NarseseException {
        assertEquals("(a==>(b&&c))",
                $("(a ==> (b &&+1 c))").root().toString());
    }

    @Test
    public void testEmbeddedChangedRootSeqToMerged() throws Narsese.NarseseException {
        Term x = $("(b &&+1 (c &&+1 d))");
        assertEquals("(&&,b,c,d)", x.root().toString());
    }

    @Test
    public void testEmbeddedChangedRootVariations() throws Narsese.NarseseException {
        {
            
            Term x = $("(a ==> (b &&+1 (c && d)))");
            assertEquals("(a==>(b &&+1 (c&&d)))", x.toString());
            assertEquals("(a ==>+- (&&,b,c,d))", x.root().toString());
        }
        {
            Term x = $("(a ==> (b &&+1 (c &| d)))");
            assertEquals("(a ==>+- (&&,b,c,d))", x.root().toString());
        }

        Term x = $("(a ==> (b &&+1 (c &&+1 d)))");
        assertEquals("(a ==>+- (&&,b,c,d))", x.root().toString());
    }

    @Test
    public void testSubtermTimeRecursive() throws Narsese.NarseseException {
        Compound c = $("(hold:t2 &&+1 (at:t1 &&+3 ([opened]:t1 &&+5 open(t1))))");
        assertEquals("(((t2-->hold) &&+1 (t1-->at)) &&+3 ((t1-->[opened]) &&+5 open(t1)))", c.toString());
        assertEquals(0, c.subTime($("hold:t2")));
        assertEquals(1, c.subTime($("at:t1")));
        assertEquals(4, c.subTime($("[opened]:t1")));
        assertEquals(9, c.subTime($("open(t1)")));
        assertEquals(9, c.dtRange());
    }

    @Test
    public void testSubtermTimeNegAnon() throws Narsese.NarseseException {
        
        String needle = "(--,noid(_0,#1))";
        String haystack = "(&|,(--,noid(_0,#1)),(\"+\"-->(X-->noid)),noid(#1,#1))";
        assertEquals(0, $(haystack).subTimeSafe($(needle)));
        assertEquals(0, $(haystack).anon().subTimeSafe($(needle).anon()));
    }









    @Test
    public void testSubtermRepeat() throws Narsese.NarseseException {
        assertEquals(0, $("(x &&+1 x)").subTimeSafe($("x")));
        assertEquals(1, $("(x &&+1 x)").subTimeSafe($("x"), 1));
    }

    @Test
    public void testTransformedImplDoesntActuallyOverlap() throws Narsese.NarseseException {
        assertEquals("(((#1 &&+7 (_1,_2)) &&+143 (_1,_2)) ==>+7 (_1,_2))",
                $("(((#1 &&+7 (_1,_2)) &&+143 (_1,_2)) ==>+- (_1,_2))").dt(7).toString());
    }

    @Test
    public void testConjEarlyLate() throws Narsese.NarseseException {
        {
            Term yThenZ = $("(y &&+1 z)");
            assertEquals("y", yThenZ.sub(Op.conjEarlyLate(yThenZ, true)).toString());
            assertEquals("z", yThenZ.sub(Op.conjEarlyLate(yThenZ, false)).toString());
        }
        {
            Term yThenZ = $("(y &&+0 z)");
            assertEquals("y", yThenZ.sub(Op.conjEarlyLate(yThenZ, true)).toString());
            assertEquals("z", yThenZ.sub(Op.conjEarlyLate(yThenZ, false)).toString());
        }

        {
            Term zThenY = $("(z &&+1 y)");
            assertEquals("z", zThenY.sub(Op.conjEarlyLate(zThenY, true)).toString());
            assertEquals("y", zThenY.sub(Op.conjEarlyLate(zThenY, false)).toString());
        }

    }

    @Test
    public void subtermTimeWithConjInImpl() throws Narsese.NarseseException {
        Term t = $("(((a &&+5 b) &&+5 c) ==>-5 d)");
        assertEquals(DTERNAL, t.subTimeSafe($("(a &&+5 b)")));
        assertEquals(DTERNAL, t.subTimeSafe($("d")));
        assertEquals(DTERNAL, t.subTimeSafe($("a")));
        assertEquals(DTERNAL, t.subTimeSafe($("b")));
        assertEquals(DTERNAL, t.subTimeSafe($("c")));
        assertEquals(DTERNAL, t.subTimeSafe($("x")));
    }

    @Test
    public void testSubtermTimeRecursiveWithNegativeCommutive() throws Narsese.NarseseException {
        Compound b = $("(a &&+5 b)");
        assertEquals(0, b.subTime(A));
        assertEquals(5, b.subTime(B));

        Compound c = $("(a &&-5 b)");
        assertEquals(5, c.subTime(A));
        assertEquals(0, c.subTime(B));

        Compound d = $("(b &&-5 a)");
        assertEquals(0, d.subTime(A));
        assertEquals(5, d.subTime(B));













    }

    @Test
    public void testSubtermConjInConj() throws Narsese.NarseseException {
        String g0 = "(((x) &&+1 (y)) &&+1 (z))";
        Compound g = $(g0);
        assertEquals(g0, g.toString());
        assertEquals(0, g.subTime($("(x)")));
        assertEquals(1, g.subTime($("(y)")));
        assertEquals(2, g.subTime($("(z)")));

        Compound h = $("((z) &&+1 ((x) &&+1 (y)))");
        assertEquals(0, h.subTime($("(z)")));
        assertEquals(1, h.subTime($("(x)")));
        assertEquals(2, h.subTime($("(y)")));

        Compound i = $("((y) &&+1 ((z) &&+1 (x)))");
        assertEquals(0, i.subTime($("(y)")));
        assertEquals(1, i.subTime($("(z)")));
        assertEquals(2, i.subTime($("(x)")));

        Compound j = $("((x) &&+1 ((z) &&+1 (y)))");
        assertEquals(0, j.subTime($("(x)")));
        assertEquals(1, j.subTime($("(z)")));
        assertEquals(2, j.subTime($("(y)")));
    }

    @Test
    public void testDTRange() throws Narsese.NarseseException {
        assertEquals(1, $("((z) &&+1 (y))").dtRange());
    }

    @Test
    public void testDTRange2() throws Narsese.NarseseException {
        Term t = $("((x) &&+1 ((z) &&+1 (y)))");
        assertEquals(2, t.dtRange());
    }

    @Test
    public void testDTRange3() throws Narsese.NarseseException {
        assertEquals(4, $("((x) &&+1 ((z) &&+1 ((y) &&+2 (w))))").dtRange());
        assertEquals(4, $("(((z) &&+1 ((y) &&+2 (w))) &&+1 (x))").dtRange());
    }

    @Test
    public void testNonCommutivityImplConcept() throws Narsese.NarseseException {

        NAR n = NARS.shell();
        n.input("((x) ==>+5 (y)).", "((y) ==>-5 (x)).");
        n.run(5);

        TreeSet d = new TreeSet(Comparator.comparing(Object::toString));
        n.conceptsActive().forEach(x -> d.add(x.id));

        
        assertTrue(d.contains($("((x)==>(y))")));
        assertTrue(d.contains($("((y)==>(x))")));
    }

    @Test
    public void testCommutivity() throws Narsese.NarseseException {

        assertTrue($("(b && a)").isCommutative());
        assertTrue($("(b &| a)").isCommutative());
        assertFalse($("(b &&+1 a)").isCommutative());
        assertTrue($("(b &&+- a)").isCommutative());


        Term abc = $("((a &| b) &| c)");
        assertEquals("(&|,a,b,c)", abc.toString());
        assertTrue(abc.isCommutative());

    }

    @Test
    public void testInvalidConjunction() throws Narsese.NarseseException {

        Compound x = $("(&&,(#1-->I),(#1-->{i141}),(#2-->{i141}))");
        assertNotNull(x);
        assertEquals(Null, x.dt(-1));
        assertEquals(Null, x.dt(+1));
        assertNotEquals(Null, x.dt(0));
        assertNotEquals(Null, x.dt(DTERNAL));
        assertNotEquals(Null, x.dt(XTERNAL));
    }

    @Test
    public void testConjRoot() throws Narsese.NarseseException {
        






        
        Term a = $("(x && y)");

        Term b = $("(x &&+1 y)");
        assertEquals("(x&&y)", b.root().toString());

        Term c = $("(x &&+1 x)");
        assertEquals("(x &&+- x)", c.root().toString());

        Term cn = $("(x &&+1 --x)");
        assertEquals("((--,x) &&+- x)", cn.root().toString());

        Term d = $("(x &&+1 (y &&+1 z))");
        assertEquals("(&&,x,y,z)", d.root().toString());

    }

    @Test
    public void testImplRootDistinct() throws Narsese.NarseseException {

        Term f = $("(x ==> y)");
        assertEquals("(x==>y)", f.root().toString());

        Term g = $("(y ==>+1 x)");
        assertEquals("(y==>x)", g.root().toString());

    }

    @Test
    public void testImplRootRepeat() throws Narsese.NarseseException {
        Term h = $("(x ==>+1 x)");
        assertEquals("(x ==>+- x)", h.root().toString());
    }

    @Test
    public void testImplRootNegate() throws Narsese.NarseseException {
        Term i = $("(--x ==>+1 x)");
        assertEquals("((--,x) ==>+- x)", i.root().toString());

    }








    @Disabled
    @Test
    public void testEqualsAnonymous3() throws Narsese.NarseseException {
        






        
        







        assertEquals($.<Compound>$("(x && (y ==> z))").temporalize(Retemporalize.retemporalizeAllToXTERNAL),
                $.<Compound>$("(x &&+1 (y ==>+1 z))").temporalize(Retemporalize.retemporalizeAllToXTERNAL));
        






        assertEquals("((x &&+1 z) ==>+1 w)",
                $("(x &&+1 (z ==>+1 w))").toString());

        assertEquals($.<Compound>$("((x &&+- z) ==>+- w)").temporalize(Retemporalize.retemporalizeAllToXTERNAL),
                $.<Compound>$("(x &&+1 (z ==>+1 w))").temporalize(Retemporalize.retemporalizeAllToXTERNAL));
    }

    @Test
    public void testEqualsAnonymous4() {
        
        






        
        






        

        






        
        






        
    }

    @Test
    public void testEqualAtemporally5() {
        
        






        
        






        
    }

    @Test
    public void testRetermporalization1() throws Narsese.NarseseException {

        String st = "((--,(happy)) && (--,((--,(o))&&(happy))))";
        Compound t = $(st);
        assertEquals("(--,(happy))", t.toString());
        Term xe = t.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        assertEquals("(--,(happy))", xe.toString());

        


    }

    @Test
    public void testConjSeqConceptual2() throws Narsese.NarseseException {
        Term t = $("((--,((--,(--a &&+1 --b)) &&+1 a)) &&+1 a)");
        assertEquals("((--,((--,((--,a) &&+1 (--,b))) &&+1 a)) &&+1 a)", t.toString());

        Term r = t.root();
        {
            assertEquals("((--,((||,a,b)&&a)) &&+- a)", r.toString());
        }

        {
            Term c = t.concept();
            assertTrue(c instanceof Compound);
            assertEquals(r, c);
        }
    }

    @Test
    public void testConjSeqConceptual1() throws Narsese.NarseseException {
        assertConceptual("((--,(nario,zoom))&&happy)", "((--,(nario,zoom)) && happy)");
        assertConceptual("((--,(nario,zoom))&&happy)", "--((--,(nario,zoom)) && happy)");
        assertConceptual("((--,(nario,zoom))&&happy)", "((--,(nario,zoom)) &&+- happy)");
        assertConceptual("(((--,(nario,zoom))&&happy) &&+- (--,(x,(--,x))))", "(((--,(nario,zoom)) &&+- happy) &&+- (--,(x,(--,x))))");

        String c =
                
                "(&&,(--,(nario,zoom)),vx,vy)";
        assertConceptual(
                c, "((vx &&+97 vy) &&+156 (--,(nario,zoom)))");
        assertConceptual(
                c, "((vx &&+97 vy) &&+100 (--,(nario,zoom)))");
        assertConceptual(
                c,
                "((vx &&+97 vy) &&-100 (--,(nario,zoom)))");
    }

    @Test
    void testXternalConjCommutiveAllowsPosNeg() {
        String s = "( &&+- ,(--,x),x,y)";
        assertEquals(s,
                Op.CONJ.compound(XTERNAL, new Term[]{the("x"), the("x").neg(), the("y")}).toString());
        assertEquals(s,
                Op.CONJ.compound(XTERNAL, new Term[]{the("y"), the("x"), the("x").neg()}).toString()); 
    }








    @Test
    public void testConceptual2() throws Narsese.NarseseException {

        Term x = $("((--,(vy &&+- happy)) &&+- (happy &&+- vy))");
        assertTrue(x instanceof Compound);
        Term y = $("((--,(vy &&+84 happy))&&(happy&|vy))");
        assertEquals(
                
                "((--,(vy &&+84 happy))&&(vy&|happy))",
                y.toString());
        assertEquals(
                
                "((--,(vy&&happy)) &&+- (vy&&happy))",
                y.concept().toString());

    }

    @Test
    public void testRetermporalization2() throws Narsese.NarseseException {
        String su = "((--,(happy)) &&+- (--,((--,(o))&&+-(happy))))";
        Compound u = $(su);
        assertEquals("((--,((--,(o)) &&+- (happy))) &&+- (--,(happy)))", u.toString());

        Term ye = u.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        assertEquals("(--,(happy))", ye.toString());

        Term yz = u.temporalize(Retemporalize.retemporalizeXTERNALToZero);
        assertEquals("(--,(happy))", yz.toString());

    }



































    @Test
    public void testImpossibleSubtermWrong() throws Narsese.NarseseException {
        Term sooper = $("(cam(0,0) &&+3 ({(0,0)}-->#1))");
        Term sub = $("cam(0,0)");
        assertTrue(sooper.contains(sub));
        assertTrue(!sooper.impossibleSubTerm(sub));
        assertEquals(0, sooper.subTimeSafe(sub));

        
        
    }

    @Test public void testDiffOfTemporalConj() {
        Term x = $$("((x&|y)~(y &&+1 x))");
        assertEquals(False, x); 
        Term xNeg = $$("(--(x&|y)~(y &&+1 x))");
        assertEquals(False, xNeg); 

    }
    @Test public void testValidTaskTerm() {
        String s = "believe(x,(believe(x,(--,(cam(9,$1) ==>-78990 (ang,$1))))&|(cam(9,$1) ==>+570 (ang,$1))))";
        Term ss = $$(s);
        assertTrue(Task.validTaskCompound(ss, true));
        assertTrue(Task.validTaskTerm(ss));

    }
}
