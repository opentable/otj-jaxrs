package com.opentable.jaxrs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.eclipse.jetty.client.util.DeferredContentProvider;
import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JettyContentStream extends OutputStream {
    private static final Logger LOG = LoggerFactory.getLogger(JettyContentStream.class);
    private static final int BUF_SIZE = 16 * 1024;

    private final ByteBufferPool bufs;
    private final DeferredContentProvider out;

    private ByteBuffer buf;

    JettyContentStream(ByteBufferPool bufs, DeferredContentProvider out) {
        this.bufs = bufs;
        this.out = out;
        buf = acquire();
    }

    @Override
    public void write(int b) throws IOException {
        if (!buf.hasRemaining()) {
            flush();
        }
        LOG.debug("write {}, {} remain", b, buf.remaining());
        buf.put((byte)b);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if (!buf.hasRemaining()) {
            flush();
        }
        while (off < len) {
            final int r = Math.min(buf.remaining(), len);
            buf.put(b, off, r);
            off += r;
            len -= r;
            offer(buf);
        }
    }

    @Override
    public void flush() {
        offer(buf);
        buf = acquire();
    }

    @Override
    public void close() throws IOException {
        if (buf != null) {
            flush();
            buf = null;
        }
    }

    private void offer(ByteBuffer buf) {
        buf.flip();
        LOG.debug("offer {}", buf.remaining());
        out.offer(buf, new ReleaseCallback(bufs, buf));
    }

    private ByteBuffer acquire() {
        final ByteBuffer b = bufs.acquire(BUF_SIZE, false);
        b.limit(b.capacity());
        return b;
    }
}

class ReleaseCallback implements Callback {

    private final ByteBufferPool bufs;
    private final ByteBuffer buf;

    ReleaseCallback(ByteBufferPool bufs, ByteBuffer buf) {
        this.bufs = bufs;
        this.buf = buf;
    }

    @Override
    public void succeeded() {
        bufs.release(buf);
    }

    @Override
    public void failed(Throwable x) {
        bufs.release(buf);
    }
}
