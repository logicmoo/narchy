package nars.agent;

import com.google.common.collect.Iterators;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.attention.AttBranch;
import nars.concept.sensor.Signal;
import nars.term.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public class SimpleReward extends Reward {

    private static final Logger logger = LoggerFactory.getLogger(SimpleReward.class);

    public final Signal concept;

    public SimpleReward(Term id, FloatSupplier r, NAgent a) {
        super(a, r);
        NAR nar = nar();
//        TermLinker linker = new TemplateTermLinker((TemplateTermLinker) TemplateTermLinker.of(id)) {
//            @Override
//            public void sample(Random rng, Function<? super Term, SampleReaction> each) {
//                if (rng.nextFloat() < 0.9f) {
//                    if ((each.apply(a.actions.get(rng.nextInt(a.actions.size())).term())).stop)
//                        return;
//                }
//                super.sample(rng, each);
//            }
//        };
        concept = new Signal(id, () -> reward, /*linker, */nar) {
            @Override
            protected AttBranch newAttn(Term term) {
                AttBranch b = new AttBranch(term, this.components());
                return b;
            }
        };
        concept.attn.parent(attn);

//        ((BeliefTables)concept.goals()).tables.add(0, new EmptyBeliefTable() {
//            @Override public void add(Remember r, NAR nar) {
//                Task i = r.input;
//
//                if (i.isNegative()) {
//                    logger.info("goal contradicts reward:\n{}", i.proof());
//                    r.forget(i);
//                }
//            }
//        });

        alwaysWantEternally(concept.term);
//        agent.//alwaysWant
//                alwaysWantEternally
//                    (concept.term, nar.confDefault(GOAL));
//        agent.alwaysQuestionDynamic(()->{
//            int dt =
//                    //0;
//                    nar.dur();
//            Random rng = nar.random();
//            return IMPL.the(agent.actions.get(rng).term().negIf(rng.nextBoolean()), dt, concept.term());
//        }, true);
    }




    @Override
    public final Iterator<Signal> iterator() {
        return Iterators.singletonIterator(concept);
    }

    @Override
    public Term term() {
        return concept.term();
    }


    @Override
    protected void updateReward(long prev, long now) {
        concept.sense(prev, now, nar());
    }
}
