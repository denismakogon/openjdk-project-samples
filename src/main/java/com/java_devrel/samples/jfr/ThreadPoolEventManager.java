package com.java_devrel.samples.jfr;

public class ThreadPoolEventManager {

    ThreadPoolStatusEvent event;

    public ThreadPoolEventManager(int threadPoolSize) {
        event = new ThreadPoolStatusEvent();
        event.total = threadPoolSize > 0 ? threadPoolSize : -1;
    }

    public void record(int busyThreads) {
        if (event == null || !event.isEnabled()) {
            return;
        }

        event.begin();
        event.end();

        if (event.shouldCommit()) {
            event.busy = busyThreads;
            event.vacant = event.total > 0 ? event.total - busyThreads : -1;
            event.ratio = event.total > 0 ? (float) event.vacant / event.total : -1;
            event.commit();
        }
    }

}
