package org.apache.bookkeeper.bookie;

import io.netty.buffer.ByteBuf;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class BufferedChannel_ReadTest extends BufferedChannel_TestBase {

    private final ByteBuf dest;
    private final int pos;
    private final int len;
    private final Class<? extends Throwable> expected;
    private final String written;

    public BufferedChannel_ReadTest(ByteBuf dest, int pos, int len, Class<? extends Throwable> expected, String written) throws IOException {
        super();
        this.dest = dest; //  buffer destinazione in cui BufferedChannel scriverà i byte letti.
        this.pos = pos; // posizione da cui cominciare a leggere nel file simulato
        this.len = len; // byte da legegre
        this.expected = expected; // ececzione attesa
        this.written = written; // simula dati presenti nel file prima della lettura

        sut.write(wrap(written));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> inputs() {
        String sample = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234"; // 30 chars
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(sample);
        }
        String bigSample = sb.toString();

        return Arrays.asList(new Object[][]{
                {null, 0, 0, NullPointerException.class, sample}, //T4
                //{emptyBuf(0), -1, -1, IllegalArgumentException.class, sample}, //T5
                //{emptyBuf(0), -1, 0, IllegalArgumentException.class, sample}, //T6
                //{emptyBuf(0), 0, -1, IllegalArgumentException.class, sample} ,//T7
                {emptyBuf(0), 0, 0, null, sample}, //T8
                {emptyBuf(1), 0, 1, null, sample}, //T9
                {emptyBuf(8), 0, 8, null, sample}, //T10
                {emptyBuf(8), 1, 8, null, sample}, //T11
                {emptyBuf(3), 5, 3, null, sample}, //T12
                {emptyBuf(31), 0, 31, IOException.class, sample}, //T13
                {emptyBuf(3), 28, 3, IOException.class, sample}, //T14
                {emptyBuf(60), 0, 60, IOException.class, sample}, //T21
                {emptyBuf(100), 10, 200, IllegalArgumentException.class, bigSample}, //T22
                {emptyBuf(256), 41, 256, null, bigSample} //T23
        });
    }

    @Test
    public void shouldBehaveAsExpected() throws Exception {
        try {
            int read = sut.read(dest, pos, len);

            // se ti aspetti eccezione ma non è stata lanciata
            if (expected != null) {
                fail("Ci aspettavamo exception: " + expected.getSimpleName());
            }

            // se ti aspetti successo, ma è una short read
            if (read < len) {
                throw new IOException("Short read");
            }

            // controlla che contenuto letto = contenuto scritto
            if (expected == null) {
                String exp = written.substring(pos, pos + len);
                String act = extractString(len, dest);
                assertEquals("Contenuto letto differente", exp, act);
            }

        } catch (Throwable t) {
            if (expected == null) {
                throw t;
            }
            if (!expected.isInstance(t)) {
                throw new AssertionError("Ci aspettavamo" + expected.getSimpleName() + " ma ottenuto: " + t.getClass().getSimpleName(), t);
            }
        }
    }



}
