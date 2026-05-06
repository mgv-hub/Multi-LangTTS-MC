package com.mgv.client.tts.util;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Rate limiter for TTS requests - enforces minimum interval between calls.
 * Uses lock/condition for precise blocking without busy-waiting.
 */

public class TtsRateLimiter {
    private final long minIntervalMs;
    private final TtsLogger logger;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean permitted = new AtomicBoolean(true);
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private volatile long lastRequestTime = 0;
    
    // considered using ScheduledFuture for cancellation but condition.await() is cleaner
    // private ScheduledFuture<?> pendingTask;
    
    public TtsRateLimiter(long minIntervalMs, TtsLogger logger) {
        this.minIntervalMs = minIntervalMs;
        this.logger = logger;
        // single-thread scheduler is enough - all tasks are short-lived signals
        this.scheduler = new ScheduledThreadPoolExecutor(1);
    }
    
    /**
     * Blocks until the rate limit allows the next request.
     * Thread-safe - multiple callers will queue via the condition variable.
     */
    public void enforceLimit() throws InterruptedException {
        lock.lock();
        try {
            long elapsed = System.currentTimeMillis() - lastRequestTime;
            long remaining = minIntervalMs - elapsed;
            
            if (remaining > 0) {
                // schedule unlock signal after remaining time
                scheduler.schedule(() -> {
                    lock.lock();
                    try {
                        permitted.set(true);
                        condition.signalAll();
                    } finally {
                        lock.unlock();
                    }
                }, remaining, TimeUnit.MILLISECONDS);
                
                // wait for signal or spurious wakeup (hence the while loop)
                while (!permitted.get()) {
                    condition.await();
                }
                permitted.set(false);
            }
            lastRequestTime = System.currentTimeMillis();
        } finally {
            lock.unlock();
        }
    }
    
    /**
     * Resets the limiter - next call to enforceLimit() will proceed immediately.
     */
    public void reset() {
        lastRequestTime = 0;
        permitted.set(true);
    }
    
    public void shutdown() {
        scheduler.shutdownNow();
    }
    
    // debug helper - exposed only in dev builds via conditional compilation
    // public long getLastRequestTime() { return lastRequestTime; }
}