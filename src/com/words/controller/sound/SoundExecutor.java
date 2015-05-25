package com.words.controller.sound;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sound executor service. Runs tasks in a single-threaded pool.
 * Rejects new tasks if current tasks is running.
 * Uses shutdown hook to terminate executor on program finish.
 * @author vlad
 */
class SoundExecutor implements Executor {
    
    private static final ThreadFactory THREAD_FACTORY = r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setPriority(Thread.MAX_PRIORITY);
        return t;
    };
    
    private final ExecutorService exec;
    
    SoundExecutor() {
        exec = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.MINUTES,
            new SynchronousQueue<>(), THREAD_FACTORY,
            new ThreadPoolExecutor.DiscardPolicy());
        
        Runtime.getRuntime().addShutdownHook(new Thread(
            () -> exec.shutdownNow()));
    }
    
    @Override
    public void execute(Runnable r) {
        exec.execute(r);
    }
}
