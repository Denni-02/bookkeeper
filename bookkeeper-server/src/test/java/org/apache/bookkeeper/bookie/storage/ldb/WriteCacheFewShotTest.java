package org.apache.bookkeeper.bookie.storage.ldb;

import static org.junit.Assert.*;  

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import org.apache.bookkeeper.bookie.storage.ldb.WriteCache;
import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.atomic.AtomicLong;
import java.io.IOException;



public class WriteCacheFewShotTest {
    private WriteCache cache;

    @Before
    public void setup() {
        this.cache = new WriteCache(PooledByteBufAllocator.DEFAULT, 1024 * 1024);
    }

    @Test
    public void testSizeAndCount() {
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertEquals(0, cache.count());
    }

    @Test
    public void testPut() {
        long ledgerId = 1;
        long entryId = 2;
        ByteBuf buf = Unpooled.wrappedBuffer("data".getBytes());

        assertTrue(cache.put(ledgerId, entryId, buf));
        assertEquals(buf.readableBytes(), cache.size());
        assertEquals(1, cache.count());

        assertTrue(cache.put(ledgerId, entryId, buf));
        assertEquals(buf.readableBytes() * 2, cache.size());
        assertEquals(2, cache.count());
    }

    @Test
    public void testGet() {
        long ledgerId = 1;
        long entryId = 2;
        ByteBuf buf = Unpooled.wrappedBuffer("ciao".getBytes());

        cache.put(ledgerId, entryId, buf);

        ByteBuf entry = cache.get(ledgerId, entryId);
        assertNotNull(entry);
        assertTrue(entry.isReadable());
        assertEquals("ciao".getBytes().length, entry.readableBytes());
    }

    @Test
    public void testHasEntry() {
        long ledgerId = 1;
        long entryId = 2;
        ByteBuf buf = Unpooled.buffer(1024);

        assertTrue(cache.put(ledgerId, entryId, buf));
        assertTrue(cache.hasEntry(ledgerId, entryId));
    }

    @Test
    public void testDeleteLedger() {
        long ledgerId = 1;
        long entryId = 2;
        ByteBuf buf = Unpooled.wrappedBuffer("abc".getBytes());

        cache.put(ledgerId, entryId, buf);
        cache.deleteLedger(ledgerId);

        AtomicLong counter = new AtomicLong();
        try {
            cache.forEach((lid, eid, entry) -> counter.incrementAndGet());
        } catch (IOException e) {
            fail("IOException not expected");
        }
        assertEquals(0, counter.get());
    }


    @Test
    public void testForEach() throws Exception {
        long ledgerId = 1;
        long entryId = 2;
        ByteBuf buf = Unpooled.wrappedBuffer("entry".getBytes());

        cache.put(ledgerId, entryId, buf.retainedDuplicate());

        AtomicLong counter = new AtomicLong();

        cache.forEach((lid, eid, entry) -> {
            assertEquals(ledgerId, lid);
            assertEquals(entryId, eid);
            assertNotNull(entry);
            assertTrue(entry.isReadable());
            counter.incrementAndGet();
        });

        assertEquals(1, counter.get());
    }

    // ###TEST CHAT GPT###

    @Test
    public void testGetLastEntry() {
        long ledgerId = 42;
        ByteBuf buf1 = Unpooled.wrappedBuffer("entry1".getBytes());
        ByteBuf buf2 = Unpooled.wrappedBuffer("entry2".getBytes());

        cache.put(ledgerId, 1L, buf1);
        cache.put(ledgerId, 2L, buf2); // last entry

        ByteBuf lastEntry = cache.getLastEntry(ledgerId);
        assertNotNull(lastEntry);
        assertEquals("entry2".getBytes().length, lastEntry.readableBytes());
    }

    @Test
    public void testClearCache() {
        cache.put(1L, 1L, Unpooled.wrappedBuffer("test".getBytes()));
        assertFalse(cache.isEmpty());

        cache.clear();
        assertTrue(cache.isEmpty());
        assertEquals(0, cache.size());
        assertEquals(0, cache.count());
        assertNull(cache.get(1L, 1L));
    }

    @Test
    public void testCloseCache() {
        // Simply call close() and ensure no exceptions are thrown.
        try {
            cache.close();
        } catch (Exception e) {
            fail("close() should not throw exceptions");
        }
    }

    @Test
    public void testGetLastEntryOnEmptyLedger() {
        ByteBuf result = cache.getLastEntry(99L);
        assertNull(result);
    }

    @Test
    public void testOverwriteSameEntryId() {
        long ledgerId = 10;
        long entryId = 5;
        ByteBuf buf1 = Unpooled.wrappedBuffer("aaa".getBytes());
        ByteBuf buf2 = Unpooled.wrappedBuffer("bbb".getBytes());

        assertTrue(cache.put(ledgerId, entryId, buf1));
        assertTrue(cache.put(ledgerId, entryId, buf2)); // overwrite

        assertEquals(2, cache.count()); // both entries are counted
        assertEquals(buf1.readableBytes() + buf2.readableBytes(), cache.size());
    }

    @Test
    public void testCacheLimitExceeded() {
        WriteCache smallCache = new WriteCache(PooledByteBufAllocator.DEFAULT, 64); // Very small size
        ByteBuf buf = Unpooled.wrappedBuffer(new byte[128]); // Too large

        boolean success = smallCache.put(1, 1, buf);
        assertFalse(success);
    }

    @Test
    public void testAlign64() {
        int size = 70;
        int aligned = WriteCache.align64(size);
        assertEquals(128, aligned); // 64-byte alignment
    }

    // ###END TEST CHAT GPT###

}
