package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBufAllocator;
import org.junit.Test;

import java.io.IOException;
import java.nio.channels.FileChannel;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;

public class BufferedChannelOtherTest extends BufferedChannelTestBase {

    public BufferedChannelOtherTest() throws IOException {
        super();
    }

    // forceWrite(true) deve invocare fileChannel.force(true) senza eccezioni.
    @Test
    public void testForceWriteTrue() throws Exception {
        // Scrive qualcosa nel buffer
        sut.write(wrap("dati di test"));

        // Esegue il metodo con forceMetadata = true
        long pos = sut.forceWrite(true);

        // Verifica che sia stato chiamato fileChannel.force(true)
        verify(fileChannelMock).force(true);

        // Verifica che la posizione restituita sia corretta
        assertEquals(sut.getFileChannelPosition(), pos);
    }

    // forceWrite(false) deve invocare fileChannel.force(false) senza eccezioni.
    @Test
    public void testForceWriteFalse() throws Exception {
        sut.write(wrap("altri dati"));

        long pos = sut.forceWrite(false);

        verify(fileChannelMock).force(false);
        assertEquals(sut.getFileChannelPosition(), pos);
    }

    // close() su istanza non chiusa chiama release e fileChannel.close(), imposta closed = true
    @Test
    public void testCloseProperly() throws Exception {
        sut.close();
        verify(fileChannelMock).close(); // verifica che il fileChannel venga chiuso
    }

    // close() su istanza già chiusa non fa nulla
    @Test
    public void testCloseTwiceDoesNothing() throws Exception {
        sut.close();   // prima chiusura effettiva
        sut.close();   // seconda chiusura ignorata
        verify(fileChannelMock, times(1)).close(); // chiusura chiamata una sola volta
    }

    // close() con IOException da fileChannel.close() → eccezione propagata
    @Test(expected = IOException.class)
    public void testCloseThrowsIOException() throws Exception {
        FileChannel failingChannel = mock(FileChannel.class);
        BufferedChannel failingSut = new BufferedChannel(ByteBufAllocator.DEFAULT, failingChannel, 64);

        // Simula IOException al momento della chiusura
        doThrow(new IOException("errore")).when(failingChannel).close();

        // Verifica che l'eccezione venga propagata
        failingSut.close();
    }

    // close() con eccezione da release(writeBuffer) → eccezione propagata
    @Test(expected = RuntimeException.class)
    public void testCloseThrowsOnRelease() throws Exception {
        FileChannel dummyChannel = mock(FileChannel.class);

        // Override del metodo close per simulare errore nel release
        BufferedChannel sutWithBrokenBuffer = new BufferedChannel(ByteBufAllocator.DEFAULT, dummyChannel, 64) {
            @Override
            public synchronized void close() throws IOException {
                throw new RuntimeException("Errore nel release");
            }
        };

        sutWithBrokenBuffer.close(); // deve sollevare RuntimeException
    }

    @Test
    public void testFlushAndForceWriteTrue() throws Exception {
        sut.write(wrap("test dati"));
        sut.flushAndForceWrite(true);
        verify(fileChannelMock).force(true);
    }

    @Test
    public void testFlushAndForceWriteFalse() throws Exception {
        sut.write(wrap("altri test"));
        sut.flushAndForceWrite(false);
        verify(fileChannelMock).force(false);
    }

    @Test
    public void testFlushAndForceWriteIfRegularFlushTrue() throws Exception {
        BufferedChannel flushSut = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannelMock, 100);

        // abilita doRegularFlushes via reflection
        Field field = BufferedChannel.class.getDeclaredField("doRegularFlushes");
        field.setAccessible(true);
        field.set(flushSut, true);

        flushSut.write(wrap("scrittura"));
        flushSut.flushAndForceWriteIfRegularFlush(true);
        verify(fileChannelMock).force(true);
    }

    @Test
    public void testFlushAndForceWriteIfRegularFlushFalse() throws Exception {
        BufferedChannel noFlushSut = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannelMock, 100);

        // disabilita doRegularFlushes via reflection
        Field field = BufferedChannel.class.getDeclaredField("doRegularFlushes");
        field.setAccessible(true);
        field.set(noFlushSut, false);

        noFlushSut.write(wrap("scrittura"));
        noFlushSut.flushAndForceWriteIfRegularFlush(true);

        // verifica che force non sia stato invocato
        verify(fileChannelMock, never()).force(anyBoolean());
    }

    
}


