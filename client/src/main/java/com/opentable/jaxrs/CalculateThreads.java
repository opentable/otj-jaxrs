package com.opentable.jaxrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// A bit of copy/paste between otj-jaxrs/otj-rest-template - not sure where we could put otherwise other than otj-spring
// which is a bit.... weird.

/**
 * Calculate optimal threads for Jetty, etc implementations. Avoids deadlocking
 * as cores grow.
 * https://docs.google.com/spreadsheets/d/179upsXNJv_xMWYHZLY2e0456bxoBYbOHW7ORV3m-CxE/edit#gid=288475975
 */
public final class CalculateThreads {
    private static final Logger LOG = LoggerFactory.getLogger(CalculateThreads.class);
    private static final int BASE_OPTIMAL_THREADS = 10;
    private static final int THREAD_OVERHEAD = 10;

    private CalculateThreads() {
        /* utility class */
    }

    /**
     * Calculate optimal threads. If they exceed the specified executorThreads, use optimal threads
     * instead. Emit appropriate logging
     * @param executorThreads executor threads - what the caller wishes to use for size
     * @param name client pool name. Used for logging.
     * @return int actual threads to use
     */
    public static int calculateThreads(final int executorThreads, final String name) {
        // For current standard 8 core machines this is 10 regardless.
        // On Java 10, you might get less than 8 core reported, but it will still size as if it's 8
        // Beyond 8 core you MIGHT undersize if running on Docker, but otherwise should be fine.
        final int optimalThreads= Math.max(BASE_OPTIMAL_THREADS, (Runtime.getRuntime().availableProcessors() + THREAD_OVERHEAD));
        int threadsChosen = optimalThreads;
        if (executorThreads > optimalThreads) {
            // They requested more, sure we can do that!
            threadsChosen = executorThreads;
            LOG.warn("Requested more than optimal threads. This is not necessarily an error, but you may be overallocating.");
        } else {
            // They weren't auto tuning (<0))
            if (executorThreads > 0) {
                LOG.warn("You requested less than optimal threads. We've ignored that.");
            }
        }

        LOG.debug("For factory {}, Optimal Threads {}, Configured Threads {}", name, optimalThreads, threadsChosen);
        return threadsChosen;
    }
}
