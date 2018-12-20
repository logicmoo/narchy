package jcog.random;

/*
 * DSI utilities
 *
 * Copyright (C) 2013-2017 Sebastiano Vigna
 *
 *  This library is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This library is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, see <http:
 *
 */


import org.apache.commons.math3.random.RandomGenerator;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 *
 * A fast, high-quality {@linkplain Random pseudorandom number generator} that
 * returns the sum of consecutive outputs of a handcrafted linear generator with 128 bits of state. It improves
 * upon {@link XorShift128PlusRandom xorshift128+}
 * under every respect: it is faster and has stronger statistical properties.
 * More details can be found on the <a href="http:
 * generators and the PRNG shootout</a> page.
 * <p>
 * <p><strong>Warning</strong>: the output of this generator might change in the near future.
 * <p>
 * <p>Note that this is
 * <strong>not</strong> a cryptographic-strength pseudorandom number generator, but its quality is
 * preposterously higher than {@link Random}'s, and its cycle length is
 * 2<sup>128</sup>&nbsp;&minus;&nbsp;1, which is more than enough for any single-thread application.
 * <p>
 * <p>By using the supplied {@link #jump()} method it is possible to generate non-overlapping long sequences
 * for parallel computations. This class provides also a {@link #split()} method to support recursive parallel computations, in the spirit of
 * Java 8's <a href="http:
 *
 * @see it.unimi.dsi.util
 * @see RandomGenerator
 */

@SuppressWarnings("javadoc")
public class XoRoShiRo128PlusRandom extends AtomicRandom {
    private static final AtomicLongFieldUpdater<XoRoShiRo128PlusRandom> A =
            AtomicLongFieldUpdater.newUpdater(XoRoShiRo128PlusRandom.class, "a");
    private static final AtomicLongFieldUpdater<XoRoShiRo128PlusRandom> B =
            AtomicLongFieldUpdater.newUpdater(XoRoShiRo128PlusRandom.class, "b");

    /**
     * The internal state of the algorithm.
     */
    private volatile long a, b;






    /**
     * Creates a new generator using a given seed.
     *
     * @param seed a seed for the generator.
     */
    public XoRoShiRo128PlusRandom(final long seed) {
        super(0);
        setSeed(seed);
    }

    /** seeds with system nano clock */
    public XoRoShiRo128PlusRandom() {
        this(System.nanoTime());
    }


    @Override protected long _nextLong() {
        final long s0 = A.get(this);
        long s1 = B.get(this);
        final long result = s0 + s1;
        s1 ^= s0;
        A.set(this, Long.rotateLeft(s0, 55) ^ s1 ^ s1 << 14);
        B.set(this, Long.rotateLeft(s1, 36));
        return result;
    }





    @Override
    public int nextInt() {
        return (int)nextLong();
    }

    @Override
    public int nextInt(final int n) {
//        return (int) nextLong(n);

        assert(n>0);
        return (((int)nextLong()) & 0b0111111111111111111111111111111) % n;
    }

    /**
     * Returns a pseudorandom uniformly distributed {@code long} value
     * between 0 (inclusive) and the specified value (exclusive), drawn from
     * this random number generator's sequence. The algorithm used to generate
     * the value guarantees that the result is uniform, provided that the
     * sequence of 64-bit values produced by this generator is.
     *
     * @param n the positive bound on the random number to be returned.
     * @return the next pseudorandom {@code long} value between {@code 0} (inclusive) and {@code n} (exclusive).
     */
    public long nextLong(final long n) {
        if (n <= 0) throw new IllegalArgumentException("illegal bound " + n + " (must be positive)");
        long t = nextLong();
        final long nMinus1 = n - 1;
        
        if ((n & nMinus1) == 0) return (t >>> Long.numberOfLeadingZeros(nMinus1)) & nMinus1;
        
        for (long u = t >>> 1; u + nMinus1 - (t = u % n) < 0; u = nextLong() >>> 1) ;
        return t;
    }

    @Override
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    /**
     * Returns the next pseudorandom, uniformly distributed
     * {@code double} value between {@code 0.0} and
     * {@code 1.0} from this random number generator's sequence,
     * using a fast multiplication-free method which, however,
     * can provide only 52 significant bits.
     * <p>
     * <p>This method is faster than {@link #nextDouble()}, but it
     * can return only dyadic rationals of the form <var>k</var> / 2<sup>&minus;52</sup>,
     * instead of the standard <var>k</var> / 2<sup>&minus;53</sup>. Before
     * version 2.4.1, this was actually the standard implementation of
     * {@link #nextDouble()}, so you can use this method if you need to
     * reproduce exactly results obtained using previous versions.
     * <p>
     * <p>The only difference between the output of this method and that of
     * {@link #nextDouble()} is an additional least significant bit set in half of the
     * returned values. For most applications, this difference is negligible.
     *
     * @return the next pseudorandom, uniformly distributed {@code double}
     * value between {@code 0.0} and {@code 1.0} from this
     * random number generator's sequence, using 52 significant bits only.
     * @since 2.4.1
     */
    public double nextDoubleFast() {
        return Double.longBitsToDouble(0x3FFL << 52 | nextLong() >>> 12) - 1.0;
    }

    @Override
    public float nextFloat() {
        return (nextLong() >>> 40) * 0x1.0p-24f;
    }

    @Override
    public boolean nextBoolean() {
        return nextLong() < 0;
    }

    @Override
    public void nextBytes(final byte[] bytes) {
        int i = bytes.length, n;
        while (i != 0) {
            n = Math.min(i, 8);
            for (long bits = nextLong(); n-- != 0; bits >>= 8) bytes[--i] = (byte) bits;
        }
    }









































    /**
     * Sets the seed of this generator.
     * <p>
     * <p>The argument will be used to seed a {@link SplitMix64RandomGenerator}, whose output
     * will in turn be used to seed this generator. This approach makes &ldquo;warmup&rdquo; unnecessary,
     * and makes the probability of starting from a state
     * with a large fraction of bits set to zero astronomically small.
     *
     * @param seed a seed for this generator.
     */
    @Override
    public void _setSeed(final long seed) {
        final XorShift1024StarRandom.SplitMix64RandomGenerator r = new XorShift1024StarRandom.SplitMix64RandomGenerator(seed);
        A.set(this, r.nextLong());
        B.set(this, r.nextLong());
    }





























}
