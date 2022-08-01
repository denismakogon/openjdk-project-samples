package com.java_devrel.samples.jfr;

import jdk.jfr.*;

import java.util.List;

@Name(ThreadPoolStatusEvent.NAME)
@Label("ThreadPool status")
@Category("ThreadPool")
@Description("ThreadPool status event, represents two main attributes: " +
        "how many threads are busy and how many more can be started")
@StackTrace(false)
public class ThreadPoolStatusEvent extends Event {
    static final String NAME = "executors.threadpool.ThreadPoolStatusEvent";
    static final List<String> attributeNames = List.of("busy", "vacant", "total");

    @Label("Number of threads running")
    public int busy;

    @Label("Number of vacant threads slots")
    public int vacant;

    @Label("Total number of threads in a pool")
    public int total;

    @Label("ratio between vacant and total number of threads")
    public float ratio;
}
