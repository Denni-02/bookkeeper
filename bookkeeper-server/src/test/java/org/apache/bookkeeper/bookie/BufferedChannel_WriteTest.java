package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.nio.ByteBuffer;


public class BufferedChannel_WriteTest extends BufferedChannel_TestBase {

    private ByteBuf buf;

    public BufferedChannel_WriteTest() throws Exception {
        super();
    }

    // Dopo ogni test libera buf
    @After
    public void releaseBufferIfNotNull() {
        if (buf != null && buf.refCnt() > 0) {
            buf.release();
        }
    }

    @Test
    public void writeWhenBufferIsNull() { // T1
        try {
            sut.write(null); // prova a scrivere nulle
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected: test passa
        } catch (Exception e) {
            fail("Expected NullPointerException but got: " + e);
        }
    }

    @Test 
    public void writeWithEmptyBuffer() throws Exception { // T2

        buf = ByteBufAllocator.DEFAULT.buffer(0); // buffer vuoto

        sut.write(buf); // scrivo

        // Assert
        assertEquals("Il write buffer deve rimanere vuoto", 0, sut.getNumOfBytesInWriteBuffer());
        assertEquals("La posizione del file deve rimanere 0", 0, sut.position());
    }

    @Test 
    public void writeWithNonEmptyBuffer() throws Exception { // T3
        
        String input = "abc";
        buf = ByteBufAllocator.DEFAULT.buffer(); // allocazione buffer
        buf.writeBytes(input.getBytes()); // all'inizio contiene 3 byte

        sut.write(buf); // scrivo

        // Assert
        assertEquals("Devono essere presenti 3 byte nel write buffer", 3, sut.getNumOfBytesInWriteBuffer());
        assertEquals("La posizione del canale deve essere avanzata di 3", 3, sut.position());
    }

    @Test
    public void testWriteWithFlushDisabledAndCopiedBytes() throws Exception { // T24
        // doRegularFlushes = false (perché unpersistedBytesBound == 0)
        BufferedChannel channel = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannelMock, 100, 100, 0);

        channel.write(wrap("1234567890")); // 10 byte

        // unpersistedBytes non deve essere aggiornato
        assertEquals(0, channel.getUnpersistedBytes());

        // forceWrite() non deve essere chiamato
        verify(fileChannelMock, never()).force(anyBoolean());
    }

    @Test
    public void testWriteWithFlushEnabledButBelowThreshold() throws Exception { // T25
        // doRegularFlushes = true, soglia alta (non raggiunta)
        BufferedChannel channel = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannelMock, 100, 100, 1000);

        channel.write(wrap("short")); // meno di 1000 byte

        // unpersistedBytes deve essere stato aggiornato (>0)
        assertEquals(5, channel.getUnpersistedBytes());

        // ma forceWrite() non deve essere chiamato
        verify(fileChannelMock, never()).force(anyBoolean());
    }

    @Test
    public void testWriteWithFlushEnabledAndAboveThreshold() throws Exception { // T26
        // doRegularFlushes = true, soglia bassa (subito superata)
        BufferedChannel channel = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannelMock, 100, 100, 5);

        channel.write(wrap("trigger flush and force")); // >5 byte

        // flush() causa chiamata a write → deve esserci stata
        verify(fileChannelMock, atLeastOnce()).write(any(ByteBuffer.class));

        // forceWrite() deve essere stato chiamato
        verify(fileChannelMock).force(false);
    }



}
