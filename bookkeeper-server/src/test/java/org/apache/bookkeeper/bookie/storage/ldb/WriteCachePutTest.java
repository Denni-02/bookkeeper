
package org.apache.bookkeeper.bookie.storage.ldb;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class WriteCachePutTest extends WriteCacheTestBase {

    private ByteBuf entry;

    @After
    public void cleanup() {
        releaseBuffer(entry); // libera la memoria buffer dopo ogni test
    }

    @Test
    public void testPutWithNullEntry() { // T1
        try {
            sut.put(0, 0, null);
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            // atteso
        }
    }

    @Test
    public void testPutWithEmptyBuffer() { // T2
        entry = Unpooled.buffer(0); // buffer vuoto
        boolean result = sut.put(0, 0, entry);
        assertTrue("Entry vuota dovrebbe essere accettata", result);
    }

    @Test
    public void testPutWithSize1() { // T3
        entry = bufferOfSize(1); // 1 byte
        boolean result = sut.put(0, 0, entry);
        assertTrue("Entry da 1 byte dovrebbe essere accettata", result);
    }

    @Test
    public void testPutWithSizeMaxMinus1() { // T4
        entry = bufferOfSize((int)(maxCacheSize - 1));
        boolean result = sut.put(0, 0, entry);
        assertTrue("Entry che occupa quasi tutta la cache deve essere accettata", result);
    }

    @Test
    public void testPutWithSizeMax() { // T5
        entry = bufferOfSize((int) maxCacheSize);
        boolean result = sut.put(0, 0, entry);
        assertTrue("Entry che occupa tutta la cache deve essere accettata", result);
    }

    @Test
    public void testPutWithSizeMaxPlus1() { // T6
        entry = bufferOfSize((int)(maxCacheSize + 1));
        boolean result = sut.put(0, 0, entry);
        assertFalse("Entry più grande della cache deve essere rifiutata", result);
    }

    /*@Test
    public void testPutWithNegativeLedgerId() { // T7
        entry = bufferOfSize(1);
        boolean result = sut.put(-1, 0, entry); // accettato se il metodo non lo vieta
        assertTrue("Entry con ledgerId negativo dovrebbe essere accettata (se permesso)", result);
    }*/


    @Test
    public void testPutWithNegativeLedgerIdThrows() { // T7 bis
        entry = bufferOfSize(1);
        try {
            sut.put(-1, 0, entry);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // atteso
        }
    }

    /*@Test
    public void testPutWithNegativeEntryId() { // T8
        entry = bufferOfSize(1);
        boolean result = sut.put(0, -1, entry); // accettato se il metodo non lo vieta
        assertTrue("Entry con entryId negativo dovrebbe essere accettata (se permesso)", result);
    }*/

    @Test
    public void testPutWithNegativeEntryIdThrows() { // T8 bis
        entry = bufferOfSize(1);
        try {
            sut.put(0, -1, entry);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // atteso
        }
    }

    @Test
    public void testPutStandard() { // T9
        entry = bufferOfSize(1);
        boolean result = sut.put(1, 2, entry);
        assertTrue("Entry standard dovrebbe essere accettata", result);
    }

    @Test
    public void testPutOverwrite() { // T10
        entry = bufferOfSize(1);
        boolean firstInsert = sut.put(0, 0, entry); // primo inserimento
        entry = bufferOfSize(1);
        boolean secondInsert = sut.put(0, 0, entry); // sovrascrittura tentata

        assertTrue("Il primo inserimento deve riuscire", firstInsert);
        assertFalse("Il secondo inserimento (sovrascrittura) deve fallire", secondInsert);
    }

}
