/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.http.impl.conn;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpClientConnection;
import org.apache.http.conn.ConnectionPoolTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * A pooling connection manager that overrides
 * {@link PoolingHttpClientConnectionManager} with some simple monitoring that
 * connections aren't starved.
 */
public class MonitoredPoolingHttpClientConnectionManager extends PoolingHttpClientConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(MonitoredPoolingHttpClientConnectionManager.class);
    private static final long WARN_INTERVAL_SECONDS = 5;

    private String clientName;
    /** Time that we last noticed a blocked connection. */
    private volatile Instant lastBlockedAt = Instant.now();
    private volatile long warnTimeMs = -1;

    private final ScheduledThreadPoolExecutor scheduler;

    public MonitoredPoolingHttpClientConnectionManager(String clientName) {
        this.clientName = clientName;

        scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1,
                new ThreadFactoryBuilder()
                    .setDaemon(true)
                    .setNameFormat("jaxrs-client-" + clientName + "-monitor-%d")
                    .build());
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.scheduleAtFixedRate(this::warnIfStalling, WARN_INTERVAL_SECONDS, WARN_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    protected HttpClientConnection leaseConnection(Future<CPoolEntry> future, long timeout, TimeUnit tunit)
    throws InterruptedException, ExecutionException, ConnectionPoolTimeoutException {
        final Optional<ScheduledFuture<?>> warnFuture;

        if (warnTimeMs > 0 && warnTimeMs < timeout) {
            final Runnable warnCommand = () -> lastBlockedAt = Instant.now();
            warnFuture = Optional.of(scheduler.schedule(warnCommand, warnTimeMs, TimeUnit.MILLISECONDS));
        } else {
            warnFuture = Optional.empty();
        }

        final Instant start = Instant.now();
        final HttpClientConnection result = super.leaseConnection(future, timeout, tunit);
        final Duration time = Duration.between(start, Instant.now());

        warnFuture.ifPresent(f -> f.cancel(false));

        if (time.toMillis() > warnTimeMs) {
            // Log stack trace only if TRACE enabled
            Throwable t = null;
            if (LOG.isTraceEnabled()) {
                t = new Throwable();
                t.fillInStackTrace();
            }
            LOG.warn("Checkout from pool \"{}\" took {}", clientName, time, t);
        }

        return result;
    }

    private void warnIfStalling() {
        if (lastBlockedAt.isAfter(Instant.now().minusSeconds(WARN_INTERVAL_SECONDS))) {
            final int nBlockedThreads = scheduler.getQueue().size() - 1; // don't count the notification task itself
            LOG.warn("Pool \"{}\" is stalling!  {} threads currently awaiting checkout", clientName, nBlockedThreads);
        }
    }

    @Override
    public void shutdown() {
        scheduler.shutdown();
        super.shutdown();
    }

    public void setCheckoutWarnTime(Duration warnTime) {
        this.warnTimeMs = warnTime.toMillis();
    }
}
