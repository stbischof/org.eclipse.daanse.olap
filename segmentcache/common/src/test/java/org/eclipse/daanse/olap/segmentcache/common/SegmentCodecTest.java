/*
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Bischof (bipolis.org) - initial
 */
package org.eclipse.daanse.olap.segmentcache.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;

import org.junit.jupiter.api.Test;

class SegmentCodecTest {

    @Test
    void foreignClassesAreRefused() {
        // a foreign Serializable rides the Java-serialized payload form,
        // where the input filter refuses it
        byte[] bytes = SegmentCodec.write(new File("x"));
        assertThatThrownBy(() -> SegmentCodec.readHeader(bytes))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void foreignBytesAndOtherVersionsAreRefused() {
        assertThatThrownBy(() -> SegmentCodec.readHeader(new byte[] { 1, 2, 3 }))
                .isInstanceOf(IllegalStateException.class);
        byte[] wrongVersion = SegmentCodec.write("x");
        wrongVersion[4] = 99;
        assertThatThrownBy(() -> SegmentCodec.readHeader(wrongVersion))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * A stamped frame read through the plain value channel is rejected by
     * name: its node-id length bytes are not a payload tag, and the reader
     * says which channel the frame belongs to.
     */
    @Test
    void stampedAndEventFramesAreRefusedInThePlainChannel() {
        byte[] stamped = SegmentCodec.writeStamped("node-1", "value");
        assertThatThrownBy(() -> SegmentCodec.readHeader(stamped))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("value channel");
    }

    @Test
    void largePayloadsCompressAndRoundTrip() {
        String payload = "z".repeat(64 * 1024);
        byte[] wire = SegmentCodec.writeStamped("node-1", payload);
        assertThat(wire.length).isLessThan(payload.length() / 2);
        assertThat(SegmentCodec.readStampedValue(wire).value()).isEqualTo(payload);
    }

    @Test
    void emptyKeyIsRefused() {
        assertThatThrownBy(() -> new SegmentCodec.SegmentKey(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @org.junit.jupiter.api.Test
    void frameVersionAndIdMarkerShareTheWireGeneration() {
        byte[] framed = SegmentCodec.write("x");
        org.junit.jupiter.api.Assertions.assertEquals(
                org.eclipse.daanse.olap.spi.SegmentWire.GENERATION, framed[4]);
    }

    @Test
    void stampedFrameRoundTripsSmallAndLarge() {
        byte[] small = SegmentCodec.writeStamped("node-a", "value");
        assertThat(small[5] & 2).isNotZero();  // FLAG_STAMPED
        assertThat(small[5] & 1).isZero();     // small: plain
        SegmentCodec.StampedValue read = SegmentCodec.readStampedValue(small);
        assertThat(read.nodeId()).isEqualTo("node-a");
        assertThat(read.value()).isEqualTo("value");

        String big = "y".repeat(10_000);
        byte[] large = SegmentCodec.writeStamped("node-b", big);
        assertThat(large[5] & 2).isNotZero();
        assertThat(large[5] & 1).isNotZero();  // large: deflated
        assertThat(large.length).isLessThan(big.length());
        SegmentCodec.StampedValue readLarge = SegmentCodec.readStampedValue(large);
        assertThat(readLarge.nodeId()).isEqualTo("node-b");
        assertThat(readLarge.value()).isEqualTo(big);
    }

    @Test
    void eventFrameRoundTripsAndKeepsInnerFrameVerbatim() {
        byte[] inner = SegmentCodec.write("x".repeat(10_000)); // deflated inner frame
        byte[] wire = SegmentCodec.writeEvent("node-a", true, inner);
        assertThat(wire[5] & 4).isNotZero(); // FLAG_EVENT
        assertThat(wire[5] & 1).isZero();    // never deflated itself

        SegmentCodec.EventValue read = SegmentCodec.readEventValue(wire);
        assertThat(read.nodeId()).isEqualTo("node-a");
        assertThat(read.created()).isTrue();
        assertThat(read.header()).isEqualTo(inner);

        byte[] small = SegmentCodec.write("small");
        SegmentCodec.EventValue removed = SegmentCodec.readEventValue(
                SegmentCodec.writeEvent("node-b", false, small));
        assertThat(removed.created()).isFalse();
        assertThat(removed.header()).isEqualTo(small);
    }

    @Test
    void truncatedEventFrameIsRefused() {
        byte[] wire = SegmentCodec.writeEvent("node-a", true, SegmentCodec.write("v"));
        byte[] truncated = java.util.Arrays.copyOf(wire, 7);
        assertThatThrownBy(() -> SegmentCodec.readEventValue(truncated))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void probeBoundaryFramesRoundTrip() {
        // payloads around the 512-byte compression probe
        for (int size : new int[] {500, 511, 512, 513, 600}) {
            String value = "z".repeat(size);
            byte[] wire = SegmentCodec.writeStamped("n", value);
            assertThat(SegmentCodec.readStampedValue(wire).value()).isEqualTo(value);
        }
    }
}
