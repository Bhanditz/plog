package com.airbnb.plog.server.fragmentation

import com.airbnb.plog.Utils
import io.netty.buffer.Unpooled

class FragmentTest extends GroovyTestCase {
    void testRejectsTooSmallForHeader() {
        final validPayload = (0..1) + (5..2) + (6..19) + [0, 0, 0, 0]
        fragmentFromPayload(validPayload)
        final truncated = validPayload[0..22]
        shouldFail IllegalArgumentException, {
            fragmentFromPayload(truncated)
        }
    }

    void testRejectsIfNoFragments() {
        shouldFail IllegalArgumentException, {
            fragmentFromPayload((0..1) + [0, 0, 0, 0] + (6..24))
        }
    }

    void testRejectsIndexOutOfRange() {
        for (count in [1, 10, 100])
            for (index in [count, count + 1, 0xfffe, 0xffff])
                shouldFail IllegalArgumentException, {
                    final countH = (count & 0xff00) >> 8
                    final countL = count & 0xff
                    final indexH = (index & 0xff00) >> 8
                    final indexL = index & 0xff
                    fragmentFromPayload((0..1) + [countH, countL, indexH, indexL] + (6..24))
                }

        shouldFail IllegalArgumentException, {
            fragmentFromPayload((0..1) + [0xff, 0xff, 0xff, 0xff] + (6..24))
        }
    }

    void testRejectsMessageLengthOverflow() {
        shouldFail IllegalArgumentException, {
            fragmentFromPayload((0..1) + (5..2) + (6..11) + [0xff, 0xff, 0xff, 0xff] + (16..23))
        }
    }

    void testAlone() {
        assert fragmentFromPayload((0..1) + [0, 1, 0, 0] + (6..19) + [0, 0, 0, 0]).isAlone()
    }

    void testNotAlone() {
        assert !fragmentFromPayload((0..1) + [0, 10, 0, 5] + (6..19) + [0, 0, 0, 0]).isAlone()
    }

    void testNotAloneButFirst() {
        assert !fragmentFromPayload((0..1) + [0, 2, 0, 0] + (6..19) + [0, 0, 0, 0]).isAlone()
    }

    void testToString() {
        final fragment = fragmentFromPayload((0..1) + (5..2) + (6..19) + [0, 0, 0, 0] as byte[])
        final expected = 'Fragment(fragmentCount=1284, fragmentIndex=770, fragmentSize=1543, msgId=38789515787, totalLength=202182159, msgHash=269554195)'
        assert fragment.toString() == expected
    }

    void testTags() {
        assert fragmentFromPayload((0..1) + (5..2) + (6..19) + [0, 0, 0, 0] as byte[]).tags.empty
        assert fragmentFromPayload((0..1) + (5..2) + (6..19) + [0, 4, 0, 0] + Arrays.asList('foo\0'.bytes) as byte[]).tags == ['foo']
        assert fragmentFromPayload((0..1) + (5..2) + (6..19) + [0, 7, 0, 0] + Arrays.asList('foo\0bar'.bytes) as byte[]).tags == ['foo', 'bar']
    }

    private static io.netty.channel.socket.DatagramPacket datagramFromPayload(byte[] payload) {
        new io.netty.channel.socket.DatagramPacket(Unpooled.wrappedBuffer(payload), Utils.localAddr, Utils.clientAddr)
    }

    private static Fragment fragmentFromPayload(byte[] payload) {
        Fragment.fromDatagram(datagramFromPayload(payload))
    }

    private static Fragment fragmentFromPayload(Collection payload) {
        fragmentFromPayload(payload as byte[])
    }
}
