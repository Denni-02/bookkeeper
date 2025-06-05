package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BufferedChannelWriteTest extends BufferedChannelTestBase {

    private ByteBuf buf;

    public BufferedChannelWriteTest() throws Exception {
        super();
    }

    @AfterEach
    void releaseBufferIfNotNull() {
        if (buf != null && buf.refCnt() > 0) {
            buf.release();
        }
    }

    @Test
    void writeWhenBufferIsNull() {
        assertThrows(NullPointerException.class, () -> {
            sut.write(null);
        });
    }

    @Test
    void writeWithEmptyBuffer() throws Exception {
        // Arrange: creo un buffer vuoto
        buf = ByteBufAllocator.DEFAULT.buffer(0); // capacity = 0

        // Act: eseguo il metodo da testare
        sut.write(buf);

        // Assert: nessun byte è stato scritto
        assertEquals(0, sut.getNumOfBytesInWriteBuffer(), "Il write buffer deve rimanere vuoto");
        assertEquals(0, sut.position(), "La posizione del file deve rimanere 0");
    }

    @Test
    void writeWithNonEmptyBuffer() throws Exception {
        // Arrange: creo un buffer con 3 byte ("abc")
        String input = "abc";
        buf = ByteBufAllocator.DEFAULT.buffer();
        buf.writeBytes(input.getBytes());

        // Act: eseguo il metodo da testare
        sut.write(buf);

        // Assert
        assertEquals(3, sut.getNumOfBytesInWriteBuffer(), "Devono essere presenti 3 byte nel write buffer");
        assertEquals(3, sut.position(), "La posizione del canale deve essere avanzata di 3");
    }


}
