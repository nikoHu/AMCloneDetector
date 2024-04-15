package com.hdu.simhash;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.nio.CharBuffer;
import java.util.Set;

/**
 * a basic SimHash implementation
 *
 * @author rana
 *
 */
@SuppressWarnings("Duplicates")
public class SimHash {
    public static final int HASH_SIZE = 64;
    public static final long HASH_RANGE = 2 ^ HASH_SIZE;

    /**
     * use short cuts to obtains a speed optimized simhash calculation
     *
     * @param s
     *          input string
     * @return 64 bit simhash of input string
     */


    // byte gram
    private static final int FIXED_BGRAM_LENGTH = 8;
    // character gram
    private static final int FIXED_CGRAM_LENGTH = 4;

    public static long computeOptimizedSimHashForString(String s) {
        return computeOptimizedSimHashForString(CharBuffer.wrap(s));
    }

    public static long computeOptimizedSimHashForString(CharBuffer s) {

        LongOpenHashSet shingles = new LongOpenHashSet(Math.min(s.length(), 100000));

        int length = s.length();
        for (int i = 0; i < length - FIXED_CGRAM_LENGTH + 1; i++) {
            // extract an ngram
            long shingle = s.charAt(i);
            shingle <<= 16;
            shingle |= s.charAt(i + 1);
            shingle <<= 16;
            shingle |= s.charAt(i + 2);
            shingle <<= 16;
            shingle |= s.charAt(i + 3);

            shingles.add(shingle);
        }

        int v[] = new int[HASH_SIZE];
        byte longAsBytes[] = new byte[8];

        for (long shingle : shingles) {

            longAsBytes[0] = (byte) (shingle >> 56);
            longAsBytes[1] = (byte) (shingle >> 48);
            longAsBytes[2] = (byte) (shingle >> 40);
            longAsBytes[3] = (byte) (shingle >> 32);
            longAsBytes[4] = (byte) (shingle >> 24);
            longAsBytes[5] = (byte) (shingle >> 16);
            longAsBytes[6] = (byte) (shingle >> 8);
            longAsBytes[7] = (byte) (shingle);

            long longHash = FPGenerator.std64.fp(longAsBytes, 0, 8);
            for (int i = 0; i < HASH_SIZE; ++i) {
                boolean bitSet = ((longHash >> i) & 1L) == 1L;
                v[i] += (bitSet) ? 1 : -1;
            }
        }

        long simhash = 0;
        for (int i = 0; i < HASH_SIZE; ++i) {
            if (v[i] > 0) {
                simhash |= (1L << i);
            }
        }
        return simhash;
    }

    public static long computeOptimizedSimHashForBytes(byte[] data, int offset, int length) {

        LongOpenHashSet shingles = new LongOpenHashSet(Math.min(length / FIXED_BGRAM_LENGTH, 100000));

        for (int i = offset; i < length - FIXED_BGRAM_LENGTH + 1; i++) {
            int pos = i;
            // extract an ngram
            long shingle = data[pos++];
            shingle <<= 8;
            shingle |= data[pos++];
            shingle <<= 8;
            shingle |= data[pos++];
            shingle <<= 8;
            shingle |= data[pos++];
            shingle <<= 8;
            shingle |= data[pos++];
            shingle <<= 8;
            shingle |= data[pos++];
            shingle <<= 8;
            shingle |= data[pos++];
            shingle <<= 8;
            shingle |= data[pos];

            shingles.add(shingle);
        }

        int v[] = new int[HASH_SIZE];
        byte longAsBytes[] = new byte[8];

        for (long shingle : shingles) {

            longAsBytes[0] = (byte) (shingle >> 56);
            longAsBytes[1] = (byte) (shingle >> 48);
            longAsBytes[2] = (byte) (shingle >> 40);
            longAsBytes[3] = (byte) (shingle >> 32);
            longAsBytes[4] = (byte) (shingle >> 24);
            longAsBytes[5] = (byte) (shingle >> 16);
            longAsBytes[6] = (byte) (shingle >> 8);
            longAsBytes[7] = (byte) (shingle);

            long longHash = FPGenerator.std64.fp(longAsBytes, 0, 8);
            for (int i = 0; i < HASH_SIZE; ++i) {
                boolean bitSet = ((longHash >> i) & 1L) == 1L;
                v[i] += (bitSet) ? 1 : -1;
            }
        }

        long simhash = 0;
        for (int i = 0; i < HASH_SIZE; ++i) {
            if (v[i] > 0) {
                simhash |= (1L << i);
            }
        }
        return simhash;
    }

    public static long computeSimHashFromString(Set<String> shingles) {

        int v[] = new int[HASH_SIZE];
        // compute a set of shingles
        for (String shingle : shingles) {
            byte[] bytes = shingle.getBytes();
            long longHash = FPGenerator.std64.fp(bytes, 0, bytes.length);
            // long hash1 = hasher.hash(bytes, bytes.length, 0);
            // long hash2 = hasher.hash(bytes, bytes.length, (int)hash1);
            // long longHash = (hash1 << 32) | hash2;
            for (int i = 0; i < HASH_SIZE; ++i) {
                boolean bitSet = ((longHash >> i) & 1L) == 1L;
                v[i] += (bitSet) ? 1 : -1;
            }
        }
        long simhash = 0;
        for (int i = 0; i < HASH_SIZE; ++i) {
            if (v[i] > 0) {
                simhash |= (1L << i);
            }
        }

        return simhash;
    }

    /**
     * 将64位hash值切分成2份
     * @param hash
     * @return
     */
    public static long[] divide2(long hash){
        long[] res = new long[2];
        res[0] = hash & 0xffffffff00000000L;
        res[1] = hash & 0x00000000ffffffffL;
        return res;
    }

    public static long[] divide4(long hash){
        long[] res = new long[4];
        res[0] = hash & 0xffff000000000000L;
        res[1] = hash & 0x0000ffff00000000L;
        res[2] = hash & 0x00000000ffff0000L;
        res[3] = hash & 0x000000000000ffffL;
        return res;
    }

    public static int hammingDistance(long hash1, long hash2) {
        long bits = hash1 ^ hash2;
        int count = 0;
        while (bits != 0) {
            bits &= bits - 1;
            ++count;
        }
        return count;
    }

    public static long rotate(long hashValue) {
        return (hashValue << 1) | (hashValue >>> -1);
    }
}
