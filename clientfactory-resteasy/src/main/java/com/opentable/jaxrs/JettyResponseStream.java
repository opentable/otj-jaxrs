package com.opentable.jaxrs;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.nio.ByteBuffer;
import java.util.function.ToIntFunction;

import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.paralleluniverse.fibers.SuspendExecution;
import co.paralleluniverse.fibers.Suspendable;
import co.paralleluniverse.strands.channels.Channel;
import co.paralleluniverse.strands.channels.Channels;

class JettyResponseStream extends InputStream {
    private static final int CHUNK_QUEUE = 32;
    private static final Logger LOG = LoggerFactory.getLogger(JettyResponseStream.class);
    private final Channel<Chunk> channel = Channels.newChannel(CHUNK_QUEUE);
    private Chunk readTop;

    void offer(ByteBuffer content, Callback callback) throws SuspendExecution {
        if (channel.isClosed()) {
            LOG.warn("Chunk {} offered to closed channel of {}", content, this);
            callback.succeeded();
            return;
        }
        LOG.debug("send chunk {} to channel of {}", content, this);
        try {
            channel.send(new Chunk(content, callback));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    @Suspendable
    @Override
    public int read() throws IOException {
        return read0(chunk -> chunk.buf.get());
    }

    @Suspendable
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return read0(chunk -> {
            final int r = Math.min(chunk.buf.remaining(), len);
            chunk.buf.get(b, off, r);
            return r;
        });
    }

    @Suspendable
    private int read0(ToIntFunction<Chunk> reader) throws IOException {
        try {
            if (readTop == null) {
                LOG.debug("do receive {}", this);
                readTop = channel.receive();
            }
            if (readTop == null) {
                LOG.debug("eof {}", this);
                return -1;
            }

            final int result = reader.applyAsInt(readTop);
            LOG.debug("read0 {} = {}", this, result);
            if (!readTop.buf.hasRemaining()) {
                LOG.debug("callback {} run", readTop.callback);
                readTop.callback.succeeded();
                readTop = null;
            }
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException(e.getMessage());
        } catch (SuspendExecution e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
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
