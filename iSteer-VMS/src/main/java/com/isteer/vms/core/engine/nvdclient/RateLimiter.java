package com.isteer.vms.core.engine.nvdclient;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class RateLimiter {

    private static final int MAX_REQUESTS = 50;
    private static final long TIME_WINDOW_MS = 30_000;

    private final Deque<Long> requestTimestamps = new LinkedList<>();
    private final ReentrantLock lock = new ReentrantLock();

    // Thread pool for executing tasks after acquiring permit
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * Acquire a permit (wait if needed) based on 50 requests per 30 seconds.
     */
    public CompletableFuture<Void> acquirePermit() {
        return CompletableFuture.runAsync(() -> {
            while (true) {
                long now = Instant.now().toEpochMilli();

                lock.lock();
                try {
                    // Remove old timestamps
                    while (!requestTimestamps.isEmpty() && now - requestTimestamps.peekFirst() > TIME_WINDOW_MS) {
                        requestTimestamps.pollFirst();
                    }

                    if (requestTimestamps.size() < MAX_REQUESTS) {
                        requestTimestamps.offerLast(now);
                        return;
                    }

                } finally {
                    lock.unlock();
                }

                try {
                    Thread.sleep(100); // Retry every 100ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Rate limiter interrupted", e);
                }
            }
        });
    }

    /**
     * Submits a task for rate-limited execution and returns a CompletableFuture.
     */
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        return acquirePermit().thenApplyAsync(ignored -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }
}
