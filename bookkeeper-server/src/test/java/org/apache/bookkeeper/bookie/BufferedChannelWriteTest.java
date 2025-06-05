package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class BufferedChannelWriteTest extends BufferedChannelTestBase {

    private ByteBuf buf;

    public BufferedChannelWriteTest() throws Exception {
        super();
    }

    @After
    public void releaseBufferIfNotNull() {
        if (buf != null && buf.refCnt() > 0) {
            buf.release();
        }
    }

    @Test
    public void writeWhenBufferIsNull() {
        try {
            sut.write(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // expected
        } catch (Exception e) {
            fail("Expected NullPointerException but got: " + e);
        }
    }

    @Test
    public void writeWithEmptyBuffer() throws Exception {
        // Arrange
        buf = ByteBufAllocator.DEFAULT.buffer(0);

        // Act
        sut.write(buf);

        // Assert
        assertEquals("Il write buffer deve rimanere vuoto", 0, sut.getNumOfBytesInWriteBuffer());
        assertEquals("La posizione del file deve rimanere 0", 0, sut.position());
    }

    @Test
    public void writeWithNonEmptyBuffer() throws Exception {
        // Arrange
        String input = "abc";
        buf = ByteBufAllocator.DEFAULT.buffer();
        buf.writeBytes(input.getBytes());

        // Act
        sut.write(buf);

        // Assert
        assertEquals("Devono essere presenti 3 byte nel write buffer", 3, sut.getNumOfBytesInWriteBuffer());
        assertEquals("La posizione del canale deve essere avanzata di 3", 3, sut.position());
    }
}
