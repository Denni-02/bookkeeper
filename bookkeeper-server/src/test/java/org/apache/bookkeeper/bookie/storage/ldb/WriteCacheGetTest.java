package org.apache.bookkeeper.bookie.storage.ldb;

import io.netty.buffer.ByteBuf;
import org.junit.Test;

import static org.junit.Assert.*;

public class WriteCacheGetTest extends WriteCache_TestBase {

    @Test
    public void testGetFromNonExistentLedger() { // T11
        ByteBuf result = sut.get(999, 0); // ledger inesistente
        assertNull("Deve restituire null se il ledger non esiste", result);
    }

    @Test
    public void testGetExistingEntry0() { // T12
        ByteBuf entry = bufferOfSize(1);
        sut.put(1, 0, entry.copy()); // copia per evitare problemi di refCnt

        ByteBuf result = sut.get(1, 0);
        assertNotNull("L'entry (1,0) dovrebbe esistere", result);
        assertEquals("Contenuto dell'entry deve combaciare", entry.getByte(0), result.getByte(0));

        releaseBuffer(entry); // rilascio solo la copia locale
    }

    @Test
    public void testGetExistingEntry1() { // T13
        ByteBuf entry = bufferOfSize(1);
        sut.put(1, 1, entry.copy()); // stessa logica del test precedente

        ByteBuf result = sut.get(1, 1);
        assertNotNull("L'entry (1,1) dovrebbe esistere", result);
        assertEquals("Contenuto dell'entry deve combaciare", entry.getByte(0), result.getByte(0));

        releaseBuffer(entry);
    }

    @Test
    public void testGetNonExistentEntryInExistingLedger() { // T14
        // inseriamo almeno un ledger per testare il caso entryId mancante
        sut.put(1, 0, bufferOfSize(1));

        ByteBuf result = sut.get(1, 999); // entryId non presente
        assertNull("Deve restituire null se l'entry non esiste anche se il ledger esiste", result);
    }
}
