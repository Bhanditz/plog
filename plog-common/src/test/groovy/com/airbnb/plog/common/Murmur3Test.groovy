package com.airbnb.plog.common

import com.google.common.hash.Hashing
import io.netty.buffer.Unpooled

class Murmur3Test extends GroovyTestCase {
    private void compareForBytes(byte[] input) {
        final guavaHash = Hashing.murmur3_32().hashBytes(input).asInt()
        final plogHash = Murmur3.hash32(Unpooled.wrappedBuffer(input))
        assert plogHash == guavaHash, input
    }

    void testGuavaCompat() {
        final raw = 'abcdefghijklmnopqrstuvwxyz'.bytes

        for (seed in [0, 1, 10])
            for (len in 0..20) {
                final model = Arrays.copyOf(raw, len)
                final guavaHash = Hashing.murmur3_32(seed).hashBytes(model).asInt()
                final plogHash = Murmur3.hash32(Unpooled.wrappedBuffer(model), 0, len, seed)
                assert plogHash == guavaHash
            }
    }

    void testOffsetsAndLength() {
        final raw = 'abcdef'.bytes
        final bb = Unpooled.wrappedBuffer(raw)

        for (from in 0..raw.length)
            for (to in from..raw.length) {
                final model = Arrays.copyOfRange(raw, from, to)
                final guavaHash = Hashing.murmur3_32().hashBytes(model).asInt()
                final plogHash = Murmur3.hash32(bb, from, to - from, 0)
                assert plogHash == guavaHash
            }
    }

    void testCaseFoundInUpshot() {
        compareForBytes([
                0xdc, 0x00, 0x12, 0x00, 0xcf, 0xa0, 0xf9, 0x81, 0xfa, 0x1d, 0x6c, 0xac, 0x85, 0xcf, 0x4b, 0x4d, 0x64, 0x85, 0x4e, 0x3c,
                0x50, 0x93, 0xda, 0x00, 0x60, 0x53, 0x45, 0x4c, 0x45, 0x43, 0x54, 0x20, 0x20, 0x60, 0x71, 0x75, 0x65, 0x73, 0x74, 0x69,
                0x6f, 0x6e, 0x32, 0x5f, 0x70, 0x6f, 0x73, 0x74, 0x73, 0x60, 0x2e, 0x2a, 0x20, 0x46, 0x52, 0x4f, 0x4d, 0x20, 0x60, 0x71,
                0x75, 0x65, 0x73, 0x74, 0x69, 0x6f, 0x6e, 0x32, 0x5f, 0x70, 0x6f, 0x73, 0x74, 0x73, 0x60, 0x20, 0x57, 0x48, 0x45, 0x52,
                0x45, 0x20, 0x60, 0x71, 0x75, 0x65, 0x73, 0x74, 0x69, 0x6f, 0x6e, 0x32, 0x5f, 0x70, 0x6f, 0x73, 0x74, 0x73, 0x60, 0x2e,
                0x60, 0x69, 0x64, 0x60, 0x20, 0x3d, 0x20, 0x31, 0x20, 0x4c, 0x49, 0x4d, 0x49, 0x54, 0x20, 0x31, 0x20, 0x2f, 0x2a, 0x2a,
                0x2f, 0xcf, 0x00, 0x00, 0x01, 0x46, 0x18, 0x2e, 0x75, 0x74, 0xcf, 0x00, 0x00, 0x01, 0x46, 0x18, 0x2e, 0x75, 0x76, 0xce,
                0x00, 0x1a, 0xf6, 0x25, 0xa9, 0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x68, 0x6f, 0x73, 0x74, 0xb1, 0x76, 0x61, 0x67, 0x72, 0x61,
                0x6e, 0x74, 0x40, 0x6c, 0x6f, 0x63, 0x61, 0x6c, 0x68, 0x6f, 0x73, 0x74, 0xb3, 0x61, 0x69, 0x72, 0x62, 0x65, 0x64, 0x33,
                0x5f, 0x64, 0x65, 0x76, 0x65, 0x6c, 0x6f, 0x70, 0x6d, 0x65, 0x6e, 0x74, 0xcd, 0x14, 0xa6, 0xa9, 0x6c, 0x6f, 0x63, 0x61,
                0x6c, 0x68, 0x6f, 0x73, 0x74, 0xcd, 0x61, 0xfd, 0x2f, 0x91, 0xb4, 0x6d, 0x6f, 0x6e, 0x6f, 0x72, 0x61, 0x69, 0x6c, 0x2d,
                0x64, 0x65, 0x76, 0x65, 0x6c, 0x6f, 0x70, 0x6d, 0x65, 0x6e, 0x74, 0xc0, 0xc0, 0x01
        ] as byte[])
    }

    void testZeroes() {
        compareForBytes([0, 0, 0, 0] as byte[])
    }

    void testOnes() {
        compareForBytes([0xff, 0xff, 0xff, 0xff] as byte[])
    }

    void testSemiRandomStress() {
        final rand = new Random(0)
        for (i in 0..100_000) {
            final len = rand.nextInt(100)
            final target = new byte[len]
            rand.nextBytes(target)

            compareForBytes(target)
        }
    }
}
