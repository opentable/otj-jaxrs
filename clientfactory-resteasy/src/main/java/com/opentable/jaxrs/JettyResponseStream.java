package com.opentable.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.ToIntFunction;

import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JettyResponseStream extends InputStream {
    private static final Chunk EOF = new Chunk(null, null);
    private static final Logger LOG = LoggerFactory.getLogger(JettyResponseStream.class);
    private final BlockingQueue<Chunk> chunks = new LinkedBlockingQueue<>();

    void offer(ByteBuffer content, Callback callback) {
        if (chunks.peek() == EOF) {
            LOG.debug("discarding offer {}", content.remaining());
            callback.succeeded();
            return;
        }
        LOG.debug("offer {}", content.remaining());
        chunks.add(new Chunk(content, callback));
    }

    @Override
    public int read() throws IOException {
        return read0(chunk -> chunk.buf.get());
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return read0(chunk -> {
            final int r = Math.min(chunk.buf.remaining(), len);
            chunk.buf.get(b, off, r);
            return r;
        });
    }

    private int read0(ToIntFunction<Chunk> reader) throws IOException {
        try {
            Chunk top = chunks.peek();
            if (top == EOF) {
                LOG.debug("early eof");
                return -1;
            }
            while (top == null || !top.buf.hasRemaining()) {
                chunks.take();
                top = chunks.peek();
                if (top == EOF) {
                    LOG.debug("eof");
                    return -1;
                }
            }
            final int result = reader.applyAsInt(top);
            LOG.debug("read0 {}", result);
            if (!top.buf.hasRemaining()) {
                LOG.debug("callback {} succeeded", top.callback);
                top.callback.succeeded();
                assert top == chunks.take();
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        chunks.add(EOF);
    }

    static class Chunk {
        final ByteBuffer buf;
        final Callback callback;

        Chunk(ByteBuffer buf, Callback callback) {
            this.buf = buf;
            this.callback = callback;
        }
    }
}
