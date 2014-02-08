package com.airbnb.plog;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Getter;

import java.util.BitSet;

public class IncomingMultiPartMessage {
    private final ByteBuf payload;
    private final BitSet receivedFragments;
    @Getter
    private boolean complete = false;

    private IncomingMultiPartMessage(int totalLength, int fragmentCount) {
        this.payload = Unpooled.buffer(totalLength, totalLength);
        receivedFragments = new BitSet(fragmentCount + 1);
    }

    /**
     * Constructor for single-fragment messages, taking shortcuts
     */
    private IncomingMultiPartMessage(MultiPartMessageFragment singleFragment) {
        this.payload = Unpooled.wrappedBuffer(singleFragment.getPayload());
        this.receivedFragments = null;
        this.complete = true;
    }

    public static IncomingMultiPartMessage fromFragment(MultiPartMessageFragment fragment) {
        if (fragment.isAlone())
            return new IncomingMultiPartMessage(fragment);

        final IncomingMultiPartMessage msg =
                new IncomingMultiPartMessage(fragment.getTotalLength(), fragment.getFragmentCount());
        msg.ingestFragment(fragment);
        return msg;
    }

    public void ingestFragment(MultiPartMessageFragment fragment) {
        final int size = fragment.getFragmentSize();
        final int index = fragment.getFragmentIndex();
        synchronized (receivedFragments) {
            receivedFragments.set(index);
            if (receivedFragments.cardinality() == receivedFragments.size())
                complete = true;
        }
        payload.writeBytes(fragment.getPayload(), size * index, size);
    }

    public ByteBuf getPayload() {
        if (isComplete())
            return payload;
        else
            throw new IllegalStateException("Not complete");
    }

    public int length() {
        return payload.capacity();
    }
}
