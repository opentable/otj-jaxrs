package com.opentable.jaxrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CalculateThreads {
    private static final Logger LOG = LoggerFactory.getLogger(CalculateThreads.class);

    private CalculateThreads() {
        /* utility class */
    }
    public static int calculateThreads(final int executorThreads) {
        // For current standard 8 core machines this is 10 regardless.
        // On Java 10, you might get less than 8 core reported, but it will still size as if it's 8
        // Beyond 8 core you MIGHT undersize if running on Docker, but otherwise should be fine.
        final int optimalThreads= Math.max(10, (Runtime.getRuntime().availableProcessors() + 2 ));
        int threadsChosen = optimalThreads;
        if (executorThreads > optimalThreads) {
            threadsChosen = executorThreads;
        }

        LOG.debug("Optimal Threads {}, Configured Threads {}", optimalThreads, threadsChosen);
        return threadsChosen;
    }
}
