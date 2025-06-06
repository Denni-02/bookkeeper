package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BufferedChannelFewShotTest {

    @Mock
    private FileChannel mockFileChannel;

    @Mock
    private ByteBufAllocator mockAllocator;

    private BufferedChannel bufferedChannel;

    private ByteBuf dummyWriteBuf;

    private final int capacity = 1024;

    @Before
    public void setup() throws IOException {
        dummyWriteBuf = Unpooled.directBuffer(capacity);
        when(mockAllocator.directBuffer(capacity)).thenReturn(dummyWriteBuf);
        when(mockFileChannel.position()).thenReturn(0L);
        bufferedChannel = new BufferedChannel(mockAllocator, mockFileChannel, capacity);
    }

    @Test
    public void testClear() {
        dummyWriteBuf.writeBytes(new byte[]{1, 2, 3, 4});
        assertEquals(4, bufferedChannel.getNumOfBytesInWriteBuffer());
        bufferedChannel.clear();
        assertEquals(0, bufferedChannel.getNumOfBytesInWriteBuffer());
    }

    @Test
    public void testForceWrite_withForceMetadataTrue() throws IOException {
        dummyWriteBuf.writeBytes(new byte[]{1, 2, 3});
        long expectedPosition = bufferedChannel.getFileChannelPosition();

        long result = bufferedChannel.forceWrite(true);

        assertEquals(expectedPosition, result);
        verify(mockFileChannel).force(true);
    }

    @Test
    public void testForceWrite_withForceMetadataFalse() throws IOException {
        dummyWriteBuf.writeBytes(new byte[]{1, 2, 3});
        long expectedPosition = bufferedChannel.getFileChannelPosition();

        long result = bufferedChannel.forceWrite(false);

        assertEquals(expectedPosition, result);
        verify(mockFileChannel).force(false);
    }

    @Test
    public void testFlush() throws IOException {
        byte[] testData = new byte[]{10, 20, 30};
        dummyWriteBuf.writeBytes(testData);
        ByteBuffer mockNioBuffer = dummyWriteBuf.internalNioBuffer(0, dummyWriteBuf.writerIndex());

        when(mockFileChannel.write(any(ByteBuffer.class))).thenAnswer(invocation -> {
            ByteBuffer buffer = invocation.getArgument(0);
            int remaining = buffer.remaining();
            buffer.position(buffer.limit()); // simulate all bytes written
            return remaining;
        });
        long oldPos = bufferedChannel.getFileChannelPosition();
        bufferedChannel.flush();

        assertEquals(0, bufferedChannel.getNumOfBytesInWriteBuffer());
        verify(mockFileChannel, atLeastOnce()).write(mockNioBuffer);
    }
}
