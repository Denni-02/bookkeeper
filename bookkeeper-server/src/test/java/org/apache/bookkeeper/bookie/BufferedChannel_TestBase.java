package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.mockito.Mockito;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

public class BufferedChannel_TestBase {

    protected final FileChannel fileChannelMock; // mock di FileChannel, simulato con Mockito
    protected final BufferedChannel sut; // System Under Test
    protected final int bufferSize = 100; // dimensione buffer usata per i test

    private byte[] writtenBytes = new byte[0]; // byte scritti nel mock del FileChannel
    private long position = 0; // posizione corrente nel FileChannel

    public BufferedChannel_TestBase() throws IOException {
        
        // Inizializzazione mock e sut
        fileChannelMock = Mockito.mock(FileChannel.class);
        sut = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannelMock, bufferSize);
        
        // Setting mock
        setupWriteMock();
        setupReadMock();
        setupPositionMock();
        setupSizeMock();
    }

    // Simula comportamento dell'invocazione di write() su fileChannelMock
    private void setupWriteMock() throws IOException {
        Mockito.when(fileChannelMock.write(Mockito.any(ByteBuffer.class))).thenAnswer(invocation -> {
            
            ByteBuffer src = invocation.getArgument(0); // estrae dati che devono essere scritti nel file
            int len = src.remaining(); //  quanti byte ci sono ancora da scrivere

            // Crea array e copia dati dal buffer
            byte[] incoming = new byte[len];
            src.get(incoming);

            // Crea array che unisce i vecchi byte e quelli nuovi 
            byte[] newData = new byte[writtenBytes.length + len];
            System.arraycopy(writtenBytes, 0, newData, 0, writtenBytes.length);
            System.arraycopy(incoming, 0, newData, writtenBytes.length, len);

            writtenBytes = newData; // nuovo contenuto file 
            position += len; // avanzamento puntatore

            return len; // restituisci numero byte scritti come metodo reale
        });
    }

    // Simula lettura da FileChannel
    private void setupReadMock() throws IOException {

        Mockito.when(fileChannelMock.read(Mockito.any(ByteBuffer.class), Mockito.anyLong())).thenAnswer(invocation -> {
            ByteBuffer target = invocation.getArgument(0); // estrai buffer dest in cui vuoi scrivere i dati letti
            long pos = invocation.getArgument(1); // estrai posizione da cui leggere nel file simulato

            // Simula controlli metodo read reale
            if (pos < 0) throw new IllegalArgumentException();
            if (pos >= writtenBytes.length) return -1; // EOF simulato
    

            int start = (int) pos; // dove comincia la lettura nell’array writtenBytes
            int available = writtenBytes.length - start; // byte dispnibili da pos in poi
            int requested = target.remaining(); // spazio disponibile nella destinazione

            // Viene letto il minimo tra i due valori precedenti (short read)
            int toRead = Math.min(available, requested);
            // Scrivi nel target a partire da start una quantità di byte pari a toRead
            target.put(writtenBytes, start, toRead);

            return toRead; // ritorna byte letti
        });
    }


    // Restituisce un buffer vuoto di capacità capacity
    protected static ByteBuf emptyBuf(int capacity) {
        return ByteBufAllocator.DEFAULT.buffer(capacity);
    }

    // Configura metodo position() del mock simulando comportameno FileChannel
    private void setupPositionMock() throws IOException {
        Mockito.when(fileChannelMock.position()).thenAnswer(inv -> position);
    }

    // Simula il metodo size() del FileChannel, restituisce dimensione file
    private void setupSizeMock() throws IOException {
        Mockito.when(fileChannelMock.size()).thenAnswer(inv -> (long) writtenBytes.length);
    }

    // Converte stringa in ByteBuf
    protected static ByteBuf wrap(String content) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));
        return buf;
    }

    // Estrae i primi len byte da un ByteBuf, li converte in String
    protected static String extractString(int len, ByteBuf buf) {
        byte[] data = new byte[len];
        buf.getBytes(0, data);
        return new String(data, StandardCharsets.UTF_8);
    }
}
