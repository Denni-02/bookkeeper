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
public class BufferedChannelReadTest extends BufferedChannelTestBase{

    private final ByteBuf dest;
    private final int pos;
    private final int len;
    private final Class<? extends Throwable> expected;
    private final String written;

    public BufferedChannelReadTest(ByteBuf dest, int pos, int len, Class<? extends Throwable> expected, String written) throws IOException {
        super();
        this.dest = dest;
        this.pos = pos;
        this.len = len;
        this.expected = expected;
        this.written = written;

        sut.write(wrap(written));
    }

    @Parameterized.Parameters
    public static Collection<Object[]> inputs() {
        String sample = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456"; // 32 chars

        return Arrays.asList(new Object[][]{
                {null, 0, 0, NullPointerException.class, sample},
                {emptyBuf(0), 0, 0, null, sample},
                {emptyBuf(1), 0, 1, null, sample},
                {emptyBuf(8), 0, 8, null, sample},
                {emptyBuf(8), 1, 8, null, sample},
                {emptyBuf(3), 5, 3, null, sample},
                {emptyBuf(31), 0, 31, null, sample},
                {emptyBuf(3), 28, 3, null, sample}
                //{emptyBuf(31), 0, 31, IOException.class, sample},
                //{emptyBuf(3), 28, 3, IOException.class, sample}
        });
    }

    @Test
    public void shouldBehaveAsExpected() throws Exception {
        try {
            int read = sut.read(dest, pos, len);

            // se ti aspetti eccezione ma non è stata lanciata
            if (expected != null) {
                fail("Expected exception: " + expected.getSimpleName());
            }

            // se ti aspetti successo, ma è una short read
            if (read < len) {
                throw new IOException("Short read");
            }

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
                throw new AssertionError("Expected " + expected.getSimpleName() + " but got: " + t.getClass().getSimpleName(), t);
            }
        }
    }



}
