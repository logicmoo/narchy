package nars.nal.nal4;

import nars.util.NALTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

public class NAL4NewTest extends NALTest {


    public static final int CYCLES = 450;

    @Test
    public void structural_transformationExt_forward() {
        test
        .believe("((acid,base) --> reaction)", 1.0f, 0.9f) //en("An acid and a base can have a reaction.");
        .mustBelieve(CYCLES, "(acid --> (reaction,/,base))", 1.0f, 0.9f) //en("Acid can react with base.");
        .mustBelieve(CYCLES, "(base --> (reaction,acid,/))", 1.0f, 0.9f); //en("A base is something that has a reaction with an acid.");
    }
    @Test
    public void structural_transformationExt_reverse() {
        test
        .believe("(acid --> (reaction,/,base))", 1.0f, 0.9f)
        .mustBelieve(CYCLES, "((acid,base) --> reaction)", 1.0f, 0.9f);
    }

    @Test
    public void structural_transformationInt() {
        test
        .believe("(neutralization --> (acid,base))", 1.0f, 0.9f) //en("Neutralization is a relation between an acid and a base. ");
        .mustBelieve(CYCLES, "((neutralization,\\,base) --> acid)", 1.0f, 0.9f) //en("Something that can neutralize a base is an acid.");
        .mustBelieve(CYCLES, "((neutralization,acid,\\) --> base)", 1.0f, 0.9f) //en("Something that can be neutralized by an acid is a base.");
        ;
    }
    @Test
    public void structural_transformationInt_reverse() {
        test
                .believe("((neutralization,\\,base) --> acid)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "(neutralization --> (acid,base))", 1.0f, 0.9f)
        ;
    }

    @Disabled
    @Test
    public void testCompositionFromProductInh() throws nars.Narsese.NarseseException {
        //((A..+) --> Z), (X --> Y), contains(A..+,X), task("?") |- ((A..+) --> (substitute(A..+,X,Y))), (Belief:BeliefStructuralDeduction, Punctuation:Belief)

        test
                .believe("(soda --> acid)", 1.0f, 0.9f)
                .ask("((drink,soda) --> ?death)")
                .mustBelieve(CYCLES, "((drink,soda) --> (drink,acid))", 1.0f, 0.81f);
    }

    @Disabled
    @Test
    public void testCompositionFromProductSim() throws nars.Narsese.NarseseException {

        test
                .log()
                .believe("(soda <-> deadly)", 1.0f, 0.9f)
                .ask("((soda,food) <-> #x)")
                .mustBelieve(CYCLES, "((soda,food) <-> (deadly,food))", 1.0f, 0.81f);
    }

    @Test
    public void testIntersectionOfProductSubterms1() {

        test
                .believe("f(x)", 1.0f, 0.9f)
                .believe("f(y)", 1.0f, 0.9f)
                .mustBelieve(CYCLES, "f:((x)&(y))", 1.0f, 0.81f);
    }

    @Test
    public void testIntersectionOfProductSubterms2() {

        test
                .believe("f(x,z)", 1.0f, 0.9f)
                .believe("f(y,z)", 1.0f, 0.9f)
                .mustBelieve(CYCLES * 16, "f:((x,z)&(y,z))", 1.0f, 0.81f);
    }


    @Test
    @Disabled
    public void testNeqComRecursiveConstraint() {

        /*
        SHOULD NOT HAPPEN:
        $.02;.09$ ((o-(i-happy))-->happy). 497⋈527 %.55;.18% {497⋈527: æ0IáËÑþKn;æ0IáËÑþKM;æ0IáËÑþKÄ;æ0IáËÑþKÉ;æ0IáËÑþKÌ} (((%1-->%2),(%1-->%3),neqCom(%2,%3)),((%3-->%2),((Abduction-->Belief),(Weak-->Goal),(Backward-->Permute))))
            $.04;.75$ happy(L). 497⋈512 %.55;.75% {497⋈512: æ0IáËÑþKÄ}
            $.05;.53$ ((L)-->(o-(i-happy))). 527 %.54;.53% {527: æ0IáËÑþKn;æ0IáËÑþKM;æ0IáËÑþKÉ;æ0IáËÑþKÌ} Dynamic
        */

        test
                .log()
                .believe("happy(L)", 1f, 0.9f)
                .believe("((L)-->(o-(i-happy)))", 1f, 0.9f)
                .mustNotOutput(CYCLES, "((o-(i-happy))-->happy)", BELIEF, ETERNAL);
    }


}
