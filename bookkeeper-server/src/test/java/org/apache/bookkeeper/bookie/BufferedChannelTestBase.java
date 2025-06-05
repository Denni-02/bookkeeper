package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBufAllocator;
import org.apache.bookkeeper.bookie.BufferedChannel;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.channels.FileChannel;

public class BufferedChannelTestBase {

    protected final FileChannel fileChannelMock;
    protected final BufferedChannel sut; // System Under Test

    public BufferedChannelTestBase() throws IOException {
        fileChannelMock = Mockito.mock(FileChannel.class);
        Mockito.when(fileChannelMock.position()).thenReturn(0L);

        sut = new BufferedChannel(ByteBufAllocator.DEFAULT, fileChannelMock, 1024);
    }
}
