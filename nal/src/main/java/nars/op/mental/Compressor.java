package nars.op.mental;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import nars.$;
import nars.IO;
import nars.NAR;
import nars.Task;
import nars.budget.Budget;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import nars.time.Tense;
import net.byteseek.automata.factory.MutableStateFactory;
import net.byteseek.automata.trie.TrieFactory;
import net.byteseek.io.reader.ByteArrayReader;
import net.byteseek.matcher.automata.ByteMatcherTransitionFactory;
import net.byteseek.matcher.automata.SequenceMatcherTrie;
import net.byteseek.matcher.multisequence.ListMultiSequenceMatcher;
import net.byteseek.matcher.multisequence.MultiSequenceMatcher;
import net.byteseek.matcher.multisequence.TrieMultiSequenceMatcher;
import net.byteseek.matcher.sequence.ByteSequenceMatcher;
import net.byteseek.matcher.sequence.SequenceMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by me on 2/11/17.
 */
public class Compressor extends Abbreviation implements RemovalListener<Compound, Compressor.Abbr> {

    final MutableStateFactory<SequenceMatcher> stateFactory = new MutableStateFactory<>();
    final ByteMatcherTransitionFactory<SequenceMatcher> transitionFactory = new ByteMatcherTransitionFactory<>();
    final TrieFactory<SequenceMatcher> smFactory = sequences -> new SequenceMatcherTrie(sequences, stateFactory, transitionFactory);

    private MultiSequenceMatcher encoder;
    private MultiSequenceMatcher decoder;

    final Cache<Compound, Abbr> code;

    final Map<SequenceMatcher, Abbr> dc = new ConcurrentHashMap(); //HACK
    final Map<SequenceMatcher, Abbr> ec = new ConcurrentHashMap(); //HACK


    /* static */ class Abbr {
        public final Term decompressed;
        public final AliasConcept compressed;
        final SequenceMatcher encode;
        final SequenceMatcher decode;
        private final byte[] encoded;
        private final byte[] decoded;

        Abbr(Compound decompressed, String compressed, NAR nar) {

            this.compressed = AliasConcept.get(compressed, decompressed, nar);

            this.decompressed = decompressed;

            this.encoded = IO.asBytes(this.compressed.term());
            this.decoded = IO.asBytes(decompressed);
            this.decode = new ByteSequenceMatcher(encoded);
            this.encode = new ByteSequenceMatcher(decoded);

            //HACK
            ec.put(encode, this);
            dc.put(decode, this);

            nar.on(this.compressed);
        }
    }


    public Compressor(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int pendingCapacity, int maxCodes) {
        super(n, termPrefix, volMin, volMax, selectionRate, pendingCapacity);

        code = Caffeine.newBuilder().maximumSize(maxCodes).removalListener(this).executor(n.exe).build();
    }

    @Override
    public void onRemoval(Compound key, Abbr value, RemovalCause cause) {
        ec.remove(value.encode);
        dc.remove(value.decode);
    }


    @Override
    protected void abbreviate(@NotNull Compound abbreviated, @NotNull Budget b) {
        final boolean[] changed = {false};

        Abbr abb = code.get(abbreviated, (a) -> {

            String compr = newSerialTerm();

            //System.out.println("compress CODE: " + a + " to " + compr);

            changed[0] = true;
            return new Abbr(a.term(), compr, nar);
        });
        if (changed[0]) {
            nar.believe($.sim(abb.compressed, abb.decompressed), Tense.Eternal, 1f, 0.99f);
            recompile();
        }

    }

    final AtomicBoolean busy = new AtomicBoolean();

    private void recompile() {

        if (busy.compareAndSet(false, true)) {
            nar.runLater(() -> {
                if (busy.compareAndSet(true, false)) {
                    decoder = matcher(x -> x.decode);
                    encoder = matcher(x -> x.encode);
                }
            });
        }

    }


    private MultiSequenceMatcher matcher(Function<Abbr, SequenceMatcher> which) {

        List<SequenceMatcher> mm = code.asMap().values().stream().map(which).collect(Collectors.toList());
        switch (mm.size()) {
            case 0:
                return null;

            case 1:
                return new ListMultiSequenceMatcher(mm);

            default:
                return new TrieMultiSequenceMatcher(smFactory, mm);

        }
    }

    @NotNull
    public Task encode(Task tt) {
        return transcode(tt, true);
    }

    @NotNull
    public Task decode(Task tt) {
        return transcode(tt, false);
    }

    public Task transcode(Task tt, boolean en) {

        Term i = tt.term();
        Term o = transcode(i, en);
        if (o != i) {

            try {
                //HACK wrap in product if atomic
//                if (!(o instanceof Compound))
//                    o = $.p(o);

                //System.out.println("  compress: " + i + " to " + o);

                if (o instanceof Compound) {
                    @Nullable MutableTask rr = MutableTask.clone(tt, (Compound) o);
                    if (rr!=null)
                        return rr;
                }
            } catch (Throwable t) {
                //HACK ignore
            }
        }

        return tt;

    }

    @NotNull
    public Term transcode(Term t, boolean en) {
        if (!(t instanceof Compound))
            return t;
        else {
            byte[] ii = IO.asBytes(t);
            byte[] oo = transcode(ii, en);
            return ii != oo ? IO.termFromBytes(oo, nar.concepts) : t;
        }
    }

    @NotNull
    public Term encode(Term t) {
        return transcode(t, true);
    }

    @NotNull
    public Term decode(Term t) {
        return transcode(t, false);
    }

    public byte[] transcode(byte[] b, boolean en) {

        MultiSequenceMatcher coder = en ? this.encoder : this.decoder;
        if (coder == null)
            return b;

        //byte[] bOriginal = b;

        int i = 0;


        ByteArrayReader wr = new ByteArrayReader(b);

        compare: do {

            try {

                /*Array*/List<SequenceMatcher> mm = (List<SequenceMatcher>) encoder.allMatches(wr, i);
                if (mm.size() > 1) {
                    System.out.println(mm);
                }
                if (!mm.isEmpty()) {
                    for (int i1 = 0, mmSize = mm.size(); i1 < mmSize; i1++) {
                        SequenceMatcher m = mm.get(i1);
                        Abbr c = abbr(m, en);
                        if (c != null) {
                            //substitute
                            final byte[] to = en ? c.encoded : c.decoded;
                            final byte[] from = en ? c.decoded : c.encoded;

                            final byte[] prev = b;
                            int change = to.length - from.length;
                            final byte[] next = new byte[b.length + change];

                            System.arraycopy(prev, 0, next, 0, i);
                            System.arraycopy(to, 0, next, i, to.length);
                            int l = b.length;
                            if (i < l)
                                System.arraycopy(prev, i + from.length, next, i + to.length, l - (i + from.length));

                            b = next;
                            i = 0; //start from beginning
                            wr = new ByteArrayReader(b);
                            break compare;
                        }

                    }
                }

                i++;


            } catch (IOException e) {
                e.printStackTrace();
            }

        } while (i < b.length);


        return b;

    }

    private Abbr abbr(SequenceMatcher m, boolean en) {
        return (en ? this.ec : this.dc).get(m);
    }


    public static void main(String[] args) {
        Default n = new Default();
        Compressor c = new Compressor(n, "_", 2, 8, 0.5f, 8, 32);

        n.onTask(x -> {
            Task y = c.encode(x);
            if (!y.equals(x))
                System.out.println(y);
        });
        n.log();
        n.input("a:b. b:(a,c, 1, 1, 2). c:(d,a). d:e.");
        n.run(800);


    }
}
