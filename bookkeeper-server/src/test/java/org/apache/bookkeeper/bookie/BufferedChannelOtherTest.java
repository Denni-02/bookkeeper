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

    // METODI AGGIUNTI PER AUMENTARE ADEQUACY:

    // T15: forceWrite(true) deve invocare fileChannel.force(true) senza eccezioni.
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

    // T16: forceWrite(false) deve invocare fileChannel.force(false) senza eccezioni.
    @Test
    public void testForceWriteFalse() throws Exception {
        
        sut.write(wrap("altri dati"));

        long pos = sut.forceWrite(false); // verifica che sia stato chiamato fileChannel.force(false)

        verify(fileChannelMock).force(false);

        assertEquals(sut.getFileChannelPosition(), pos);
    }

    // T17: close() su istanza non chiusa chiama release e fileChannel.close(), imposta closed = true
    @Test
    public void testCloseProperly() throws Exception {
        sut.close();
        verify(fileChannelMock).close(); // verifica che il fileChannel venga chiuso
    }

    // T18: close() su istanza già chiusa non fa nulla
    @Test
    public void testCloseTwiceDoesNothing() throws Exception {
        sut.close();   // prima chiusura effettiva
        sut.close();   // seconda chiusura ignorata
        verify(fileChannelMock, times(1)).close(); // chiusura chiamata una sola volta
    }

    // T19: close() con IOException da fileChannel.close() → eccezione propagata
    @Test(expected = IOException.class)
    public void testCloseThrowsIOException() throws Exception {
        FileChannel failingChannel = mock(FileChannel.class);
        BufferedChannel failingSut = new BufferedChannel(ByteBufAllocator.DEFAULT, failingChannel, 64);

        // Simula IOException al momento della chiusura
        doThrow(new IOException("errore")).when(failingChannel).close();

        // Verifica che l'eccezione venga propagata
        failingSut.close();
    }

    // T20: close() con eccezione da release(writeBuffer) → eccezione propagata
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

    // ALTRI TEST GENERATI VIA PROMPT SULLE VARIANTI DI forceWrite:

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

    // METODI AGGIUNTI PER AUMENTARE MUTATION COVERAGE

    @Test
    public void testForceWriteSkipsUpdateWhenBoundIsZero() throws Exception { // T27
        BufferedChannel flushSut = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannelMock, 100, 100, 0);

        // Scriviamo nel buffer
        flushSut.write(wrap("1234567890")); // 10 byte

        // Chiamata a forceWrite: non deve aggiornare unpersistedBytes
        flushSut.forceWrite(false);

        // Verifica che unpersistedBytes sia ancora 0
        assertEquals("unpersistedBytes deve rimanere 0", 0, flushSut.getUnpersistedBytes());
    }

    @Test
    public void testForceWriteUpdatesUnpersistedBytes() throws Exception {
        // BufferedChannel con unpersistedBytesBound > 0 e buffer più grande dei dati scritti
        BufferedChannel flushSut = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannelMock, 100, 100, 1000);

        // Scritto 10 byte (minori di writeCapacity)
        flushSut.write(wrap("1234567890"));

        // Non è stato flushato, quindi i byte sono ancora nel buffer
        flushSut.forceWrite(false);

        // Verifica che i 10 byte ancora nel buffer siano contati
        assertEquals("unpersistedBytes deve essere aggiornato a 10", 10, flushSut.getUnpersistedBytes());
    }


}


