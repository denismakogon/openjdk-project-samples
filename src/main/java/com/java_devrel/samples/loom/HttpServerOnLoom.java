package com.java_devrel.samples.loom;

import com.java_devrel.samples.http.Server;

import java.io.IOException;
import java.util.concurrent.*;


public class HttpServerOnLoom {

    private static final ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                    .name("virtual-", 0)
                    .allowSetThreadLocals(false)
                    .factory()
    );

    public static void main(String[] args) throws IOException {
        try(executor) {
            var server = Server.serverWithExecutor(executor, Server.defaultRouter());
            server.start();
        }
    }

}
