package org.boon.concurrent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Richard on 9/10/14.
 */
public class SimpleExecutors {

    public static ExecutorService threadPool(int size, final String poolName) {

        final int [] threadId = new int[1];
        threadId[0] = 0;
        return Executors.newFixedThreadPool(size,
                runnable -> {
                    threadId[0] = threadId[0]++;
                    Thread thread = new Thread(runnable);
                    thread.setName(poolName + " " + threadId[0]);
                    return thread;
                }
        );
    }


    public static ExecutorService threadPool(final String poolName) {

        return Executors.newCachedThreadPool(
                runnable -> {
                    Thread thread = new Thread(runnable);
                    thread.setName(poolName );
                    return thread;
                }
        );
    }
}