package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.mockito.Mockito;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BufferedChannelTestBase {

    protected final FileChannel fileChannelMock;
    protected final BufferedChannel sut; // System Under Test
    protected final int bufferSize = 100;

    private byte[] writtenBytes = new byte[0];
    private long position = 0;

    public BufferedChannelTestBase() throws IOException {
        fileChannelMock = Mockito.mock(FileChannel.class);
        sut = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannelMock, bufferSize);

        setupWriteMock();
        setupReadMock();
        setupPositionMock();
        setupSizeMock();
    }

    private void setupWriteMock() throws IOException {
        Mockito.when(fileChannelMock.write(Mockito.any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer src = invocation.getArgument(0);
            int len = src.remaining();
            byte[] incoming = new byte[len];
            src.get(incoming);

            byte[] newData = new byte[writtenBytes.length + len];
            System.arraycopy(writtenBytes, 0, newData, 0, writtenBytes.length);
            System.arraycopy(incoming, 0, newData, writtenBytes.length, len);

            writtenBytes = newData;
            position += len;

            return len;
        });
    }

    private void setupReadMock() throws IOException {

        Mockito.when(fileChannelMock.read(Mockito.any(ByteBuffer.class), Mockito.anyLong())).thenAnswer(invocation -> {
            ByteBuffer target = invocation.getArgument(0);
            long pos = invocation.getArgument(1);

            if (pos < 0) throw new IllegalArgumentException();

            if (pos >= writtenBytes.length) {
                return -1; // EOF simulato
            }

            int start = (int) pos;
            int available = writtenBytes.length - start;
            int requested = target.remaining();

            // Simula "short read" se i byte non bastano
            int toRead = Math.min(available, requested);
            target.put(writtenBytes, start, toRead);

            return toRead;
        });
    }


    protected static ByteBuf emptyBuf(int capacity) {
        return ByteBufAllocator.DEFAULT.buffer(capacity);
    }


    private void setupPositionMock() throws IOException {
        Mockito.when(fileChannelMock.position()).thenAnswer(inv -> position);
    }

    private void setupSizeMock() throws IOException {
        Mockito.when(fileChannelMock.size()).thenAnswer(inv -> (long) writtenBytes.length);
    }

    protected static ByteBuf wrap(String content) {
        ByteBuf buf = ByteBufAllocator.DEFAULT.buffer();
        buf.writeBytes(content.getBytes(StandardCharsets.UTF_8));
        return buf;
    }

    protected static String extractString(int len, ByteBuf buf) {
        byte[] data = new byte[len];
        buf.getBytes(0, data);
        return new String(data, StandardCharsets.UTF_8);
    }
}
