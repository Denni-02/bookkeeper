
package org.apache.bookkeeper.bookie.storage.ldb;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Before;

public class WriteCache_TestBase {

    protected WriteCache sut; // System Under Test
    protected ByteBufAllocator allocator; // allocatore buffer
    protected long maxCacheSize; // dimensione max cache

    @Before
    public void setup() {
        allocator = ByteBufAllocator.DEFAULT;
        maxCacheSize = 10L; // default: 10 byte di cache 
        sut = new WriteCache(allocator, maxCacheSize);
    }

    // Crea un ByteBuf con una certa dimensione
    protected ByteBuf bufferOfSize(int size) {
        ByteBuf buf = Unpooled.buffer(size);
        for (int i = 0; i < size; i++) {
            buf.writeByte((byte) i); // contenuto dummy
        }
        return buf;
    }

    // Rilascia memoria occupata dal buffer
    protected void releaseBuffer(ByteBuf buf) {
        if (buf != null && buf.refCnt() > 0) {
            buf.release();
        }
    }
}
