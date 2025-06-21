package com.java.redis.internal.datastore;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * BloomFilterValue: a basic Bloom filter RedisValue implementation.
 *
 * Features:
 * - Uses a per-instance random seed (via SecureRandom) for hashing to mitigate adversarial attacks.
 * - Computes optimal bit-array size and number of hash functions from capacity and error rate.
 * - Uses MurmurHash3 x64_128 for hashing; derives k hash positions via the (h1, h2) combination method.
 * - Thread-safe bit updates via AtomicLongArray.
 * - Supports serialization/deserialization for RDB persistence.
 */
public class BloomFilterValue implements RedisValue {
    // Random generator for seeds. Reuse one instance to avoid reseeding overhead.
    private static final SecureRandom GLOBAL_SECURE_RANDOM = new SecureRandom();

    // Per-filter parameters:
    private final long seed;              // random 64-bit seed for hashing
    private final long bitSize;           // total number of bits (m), rounded to multiple of 64
    private final int numHashFunctions;   // number of hash functions (k)
    private final AtomicLongArray bitArray; // bit array storage (length = bitSize / 64)

    /**
     * Constructor: create a Bloom filter expecting up to 'capacity' elements
     * with target false-positive probability 'errorRate'.
     *
     * @param capacity   Expected maximum number of inserted elements (n). Must be > 0.
     * @param errorRate  Desired false-positive probability (p), 0 < p < 1.
     * @throws IllegalArgumentException if capacity <= 0 or errorRate not in (0,1).
     */
    public BloomFilterValue(long capacity, double errorRate) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("BloomFilter capacity must be positive");
        }
        if (!(errorRate > 0.0 && errorRate < 1.0)) {
            throw new IllegalArgumentException("BloomFilter errorRate must be in (0, 1)");
        }
        // Per-instance random seed
        this.seed = GLOBAL_SECURE_RANDOM.nextLong();

        // Compute optimal bit-size m:
        //   m = ceil((−n * ln p) / (ln2)^2), then round up to multiple of 64 bits
        this.bitSize = computeBitSize(capacity, errorRate);

        // Compute optimal k = ceil((m / n) * ln 2)
        this.numHashFunctions = computeNumHashFunctions(this.bitSize, capacity);

        // Allocate bit array: one long per 64 bits
        int arrayLength = (int) (bitSize / 64);
        this.bitArray = new AtomicLongArray(arrayLength);
    }

    /**
     * Private constructor for deserialization, given all fields directly.
     */
    private BloomFilterValue(long seed, long bitSize, int numHashFunctions, AtomicLongArray bitArray) {
        this.seed = seed;
        this.bitSize = bitSize;
        this.numHashFunctions = numHashFunctions;
        this.bitArray = bitArray;
    }

    /**
     * Compute the Bloom filter bit-array size (m) in bits, rounded up to a multiple of 64.
     * Uses m_raw = - (capacity * ln(errorRate)) / (ln 2)^2, then ceil and round to 64-bit boundary.
     *
     * Formula reference: bits-per-element bpe = -ln(p)/(ln2)^2, so m = n * bpe,
     * then round up: m = ceil(raw) then to multiple of 64 .
     *
     * @param capacity   Expected number of elements (n)
     * @param errorRate  Desired false-positive probability (p)
     * @return bitSize m, multiple of 64
     */
    private static long computeBitSize(long capacity, double errorRate) {
        double ln2 = Math.log(2);
        // bits per element
        double bpe = -Math.log(errorRate) / (ln2 * ln2);
        double rawSize = capacity * bpe;
        long bits = (long) Math.ceil(rawSize);
        // Round up to multiple of 64 bits
        long words = (bits + 63) / 64;
        return words * 64;
    }

    /**
     * Compute the optimal number of hash functions k = ceil((m / n) * ln 2).
     *
     * @param bitSize   Total bits (m), as computed via computeBitSize.
     * @param capacity  Expected number of elements (n).
     * @return number of hash functions k (>=1)
     */
    private static int computeNumHashFunctions(long bitSize, long capacity) {
        if (capacity <= 0) {
            return 1;
        }
        double kReal = (bitSize / (double) capacity) * Math.log(2);
        int k = (int) Math.ceil(kReal);
        return (k > 0 ? k : 1);
    }

    /**
     * Add an element to the Bloom filter.
     *
     * @param element  Byte-array representation of the element (e.g., raw bytes or UTF-8).
     * @return true if at least one bit was changed from 0 to 1 (i.e., element was definitely not present);
     *         false if all bits were already set (i.e., element may have been present).
     */
    public boolean add(byte[] element) {
        // Compute 128-bit hash via MurmurHash3 x64_128, using our seed.
        long[] hash128 = murmurhash3_x64_128(element, 0, element.length, seed);
        long h1 = hash128[0];
        long h2 = hash128[1];

        boolean bitsChanged = false;
        long m = this.bitSize;
        // For i from 0 to k-1, compute combined hash: (h1 + i * h2) mod 2^64, then mod m
        for (int i = 0; i < numHashFunctions; i++) {
            // Compute combined hash; use unsigned arithmetic via masking
            long combined = h1 + i * h2;
            // As Java long is signed, mask to unsigned:
            long unsigned = combined ^ Long.MIN_VALUE; // trick: interpret as unsigned via consistent mapping
            // Compute position in [0, m):
            long pos = modUnsigned64(combined, m);
            int wordIndex = (int) (pos >>> 6); // pos / 64
            int bitIndex = (int) (pos & 0x3F);  // pos % 64
            long mask = 1L << bitIndex;

            // Atomically set the bit:
            while (true) {
                long oldWord = bitArray.get(wordIndex);
                if ((oldWord & mask) != 0L) {
                    // bit already set; no change for this position
                    break;
                }
                long newWord = oldWord | mask;
                if (bitArray.compareAndSet(wordIndex, oldWord, newWord)) {
                    bitsChanged = true;
                    break;
                }
                // else: retry
            }
        }
        return bitsChanged;
    }

    /**
     * Check whether an element might be contained in the Bloom filter.
     *
     * @param element  Byte-array representation of the element.
     * @return true if all bits for this element are set (i.e., may be present, with false positives possible);
     *         false if any bit is not set (definitely not present).
     */
    public boolean mightContain(byte[] element) {
        long[] hash128 = murmurhash3_x64_128(element, 0, element.length, seed);
        long h1 = hash128[0];
        long h2 = hash128[1];
        long m = this.bitSize;

        for (int i = 0; i < numHashFunctions; i++) {
            long pos = modUnsigned64(h1 + i * h2, m);
            int wordIndex = (int) (pos >>> 6);
            int bitIndex = (int) (pos & 0x3F);
            long mask = 1L << bitIndex;
            long word = bitArray.get(wordIndex);
            if ((word & mask) == 0L) {
                return false;
            }
        }
        return true;
    }

    /**
     * Serialize the Bloom filter state to DataOutput (for RDB persistence).
     * Writes:
     *   seed (long), bitSize (long), numHashFunctions (int), array length (int), then each long in bitArray.
     *
     * @param out  DataOutput stream to write to.
     * @throws IOException on I/O error.
     */
    public void serialize(DataOutput out) throws IOException {
        out.writeLong(seed);
        out.writeLong(bitSize);
        out.writeInt(numHashFunctions);
        int len = bitArray.length();
        out.writeInt(len);
        for (int i = 0; i < len; i++) {
            out.writeLong(bitArray.get(i));
        }
    }

    /**
     * Deserialize a BloomFilterValue from DataInput (for RDB loading).
     * Reads fields in the same order as serialize(): seed, bitSize, numHashFunctions, array length, then bit words.
     *
     * @param in  DataInput stream to read from.
     * @return a reconstructed BloomFilterValue instance.
     * @throws IOException on I/O error.
     */
    public static BloomFilterValue deserialize(DataInput in) throws IOException {
        long seed = in.readLong();
        long bitSize = in.readLong();
        int numHashFunctions = in.readInt();
        int len = in.readInt();
        AtomicLongArray arr = new AtomicLongArray(len);
        for (int i = 0; i < len; i++) {
            long word = in.readLong();
            arr.set(i, word);
        }
        return new BloomFilterValue(seed, bitSize, numHashFunctions, arr);
    }

    /**
     * Compute x mod m treating x as unsigned 64-bit, returning a value in [0, m).
     * Since Java lacks unsigned long, we ensure correct modulo by using Long.divideUnsigned or manual.
     *
     * @param x  signed long, to treat as unsigned
     * @param m  positive modulus (bitSize)
     * @return unsigned x mod m
     */
    private static long modUnsigned64(long x, long m) {
        // Java 8+: use Long.divideUnsigned, Long.remainderUnsigned
        return Long.remainderUnsigned(x, m);
    }

    // ===== MurmurHash3 x64_128 implementation =====
    // Public-domain implementation based on Austin Appleby’s MurmurHash3.
    // Returns a 2-element long array: [h1, h2].
    //
    // Note: This implementation treats the seed as the initial h1; h2 initialized to seed as well.
    // Adapted to work on byte[] inputs.
    private static long[] murmurhash3_x64_128(byte[] data, int offset, int len, long seed) {
        final int nblocks = len >>> 4; // len / 16
        long h1 = seed;
        long h2 = seed;
        final long c1 = 0x87c37b91114253d5L;
        final long c2 = 0x4cf5ad432745937fL;

        // Body
        for (int i = 0; i < nblocks; i++) {
            int i16 = offset + (i << 4);
            long k1 = getLittleEndianLong(data, i16);
            long k2 = getLittleEndianLong(data, i16 + 8);

            // mix k1
            k1 *= c1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= c2;
            h1 ^= k1;
            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729;

            // mix k2
            k2 *= c2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= c1;
            h2 ^= k2;
            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5;
        }

        // Tail
        int tailStart = offset + (nblocks << 4);
        long k1 = 0L;
        long k2 = 0L;
        int tailLen = len & 15; // len % 16
        switch (tailLen) {
            case 15: k2 ^= ((long) data[tailStart + 14] & 0xFFL) << 48;
            case 14: k2 ^= ((long) data[tailStart + 13] & 0xFFL) << 40;
            case 13: k2 ^= ((long) data[tailStart + 12] & 0xFFL) << 32;
            case 12: k2 ^= ((long) data[tailStart + 11] & 0xFFL) << 24;
            case 11: k2 ^= ((long) data[tailStart + 10] & 0xFFL) << 16;
            case 10: k2 ^= ((long) data[tailStart + 9] & 0xFFL) << 8;
            case 9:  k2 ^= ((long) data[tailStart + 8] & 0xFFL);
                k2 *= c2; k2 = Long.rotateLeft(k2, 33); k2 *= c1; h2 ^= k2;
            case 8:  k1 ^= ((long) data[tailStart + 7] & 0xFFL) << 56;
            case 7:  k1 ^= ((long) data[tailStart + 6] & 0xFFL) << 48;
            case 6:  k1 ^= ((long) data[tailStart + 5] & 0xFFL) << 40;
            case 5:  k1 ^= ((long) data[tailStart + 4] & 0xFFL) << 32;
            case 4:  k1 ^= ((long) data[tailStart + 3] & 0xFFL) << 24;
            case 3:  k1 ^= ((long) data[tailStart + 2] & 0xFFL) << 16;
            case 2:  k1 ^= ((long) data[tailStart + 1] & 0xFFL) << 8;
            case 1:  k1 ^= ((long) data[tailStart] & 0xFFL);
                k1 *= c1; k1 = Long.rotateLeft(k1, 31); k1 *= c2; h1 ^= k1;
                break;
            default:
                // no tail
        }

        // Finalization
        h1 ^= len;
        h2 ^= len;
        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);

        h1 += h2;
        h2 += h1;

        return new long[]{h1, h2};
    }

    /** Helpers for MurmurHash3 **/

    // Read 8 bytes from data starting at offset in little-endian order
    private static long getLittleEndianLong(byte[] data, int offset) {
        return ((long) data[offset] & 0xFF)
                | (((long) data[offset + 1] & 0xFF) << 8)
                | (((long) data[offset + 2] & 0xFF) << 16)
                | (((long) data[offset + 3] & 0xFF) << 24)
                | (((long) data[offset + 4] & 0xFF) << 32)
                | (((long) data[offset + 5] & 0xFF) << 40)
                | (((long) data[offset + 6] & 0xFF) << 48)
                | (((long) data[offset + 7] & 0xFF) << 56);
    }

    // Finalization mix - force all bits avalanche
    private static long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }

    /** Optional: getters for introspection, metrics, or info commands **/

    /**
     * @return the bitSize (m) of this Bloom filter.
     */
    public long getBitSize() {
        return bitSize;
    }

    /**
     * @return the expected capacity (n) implied by bitSize and numHashFunctions?
     * Note: we do not store capacity directly; you may store it if needed.
     */
    public int getNumHashFunctions() {
        return numHashFunctions;
    }

    /**
     * @return the seed used in hashing.
     */
    public long getSeed() {
        return seed;
    }

    /**
     * @return approximate memory usage in bytes: bitArray.length() * 8 bytes.
     */
    public long getEstimatedMemoryUsageBytes() {
        return ((long) bitArray.length()) * Long.BYTES;
    }
}
