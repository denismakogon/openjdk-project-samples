package com.openjdk.samples.loom;

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


public class LoomServer {
    static Logger logger = Logger.getLogger("http");
    static final List<String> allHttpMethods = List.of(
            "GET", "HEAD", "POST", "PUT", "DELETE",
            "CONNECT", "OPTIONS", "TRACE", "PATCH"
    );

    private static final ExecutorService executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                    .name("virtual-", 0)
                    .allowSetThreadLocals(false)
                    .factory()
    );

    public static void main(String[] args) throws IOException {
        logger.info("Server is starting...");

        var tcpSocket = new InetSocketAddress(8080);
        var port = tcpSocket.getPort();
        var host = tcpSocket.getHostString();
        var server = HttpServer.create(tcpSocket, 0);
        server.createContext("/", router()).getFilters().add(requestLogger(logger));
        server.setExecutor(executor);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Server is shutting down...");
            try {
                executor.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            server.stop(0);
            logger.info("Server stopped");
        }));

        logger.info(String.format("Server is ready to handle requests at %s:%s", host, port));
        server.start();
    }

    private static void handle_not_found(HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
        try(var responseBody = httpExchange.getResponseBody()) {
            responseBody.write("Not Found!".getBytes(StandardCharsets.UTF_8));
            responseBody.flush();
        }
    }

    private static void handle_root(HttpExchange httpExchange) throws IOException {
        var data = "Hello, World!\n";
        logger.info(String.format("[thread: %s] is virtual? - %s",
                Thread.currentThread().getName(),
                Thread.currentThread().isVirtual()
        ));
        httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, data.length());
        try(var responseBody = httpExchange.getResponseBody()) {
            responseBody.write(data.getBytes(StandardCharsets.UTF_8));
            responseBody.flush();
        }
    }

    private static void handle_error(HttpExchange httpExchange, Exception ex, Integer statusCode) throws IOException {
        httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        httpExchange.sendResponseHeaders(statusCode, ex.getMessage().length());
        try(var responseBody = httpExchange.getResponseBody()) {
            responseBody.write(ex.getMessage().getBytes(StandardCharsets.UTF_8));
            responseBody.flush();
        }
    }
    private static void handle_error(HttpExchange httpExchange, Exception ex) throws IOException {
        handle_error(httpExchange, ex, HttpURLConnection.HTTP_BAD_GATEWAY);
    }

    private static void handle_get(HttpExchange httpExchange) throws IOException {
        var queryParams= queryToMap(httpExchange.getRequestURI());
        logger.info(queryParams.toString());
        var respData = String.format("I've got the following query: %s\n", queryParams);
        logger.info(respData);
        httpExchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        httpExchange.sendResponseHeaders(HttpURLConnection.HTTP_OK, respData.length());
        try(var responseBody = httpExchange.getResponseBody()) {
            responseBody.write(respData.getBytes(StandardCharsets.UTF_8));
            responseBody.flush();
        }
    }

    private static boolean validateHTTPMethod(
            HttpExchange httpExchange, List<String> allowedHttpMethods) throws IOException {
        var method = httpExchange.getRequestMethod();
        var exists = allowedHttpMethods.contains(method);
        if (!exists) {
            handle_error(httpExchange,
                    new Exception("method not allowed"),
                    HttpURLConnection.HTTP_BAD_METHOD);
        }
        return exists;
    }

    private static HttpHandler router() {
        return httpExchange -> {
            try {
                logger.info("in exchange switch");
                switch (httpExchange.getRequestURI().getPath()) {
                    case "/" -> {
                        if(validateHTTPMethod(httpExchange, allHttpMethods)) {
                            handle_root(httpExchange);
                        }
                    }
                    case "/get" -> {
                        if (validateHTTPMethod(httpExchange, List.of("GET", "POST"))) {
                            handle_get(httpExchange);
                        }
                    }
                    default -> handle_not_found(httpExchange);
                }
            } catch (Exception ex) {
                handle_error(httpExchange, ex);
            } finally {
                httpExchange.close();
            }
        };
    }

    private static Filter requestLogger(Logger logger) {
        return new Filter() {
            @Override
            public void doFilter(HttpExchange httpExchange, Chain chain) throws IOException {
                try {
                    chain.doFilter(httpExchange);
                } finally {
                    logger.info(String.format("""
                                    request details:
                                    \tmethod=%s
                                    \turi=%s
                                    \tclient=%s
                                    \tagent=%s""",
                            httpExchange.getRequestMethod(),
                            httpExchange.getRequestURI(),
                            httpExchange.getRemoteAddress(),
                            httpExchange.getRequestHeaders().getFirst("User-Agent")));
                    logger.info(String.format("""
                                    response details:
                                    \tstatus_code=%s
                                    \theader=%s""",
                            httpExchange.getResponseCode(),
                            httpExchange.getResponseHeaders())
                    );
                }
            }

            @Override
            public String description() {
                return "logging";
            }
        };
    }

    private static Map<String, List<String>> queryToMap(URI url) {
        logger.info("in query params method");
        return Arrays.stream(url.getQuery().split("&")).parallel()
                .map(keyValue -> {
                    var pair = keyValue.split("=");
                    String key = null;
                    String value = null;
                    if (pair.length >= 2) {
                        key = pair[0];
                        value = keyValue.substring(key.length() + 1);
                    }
                    if (pair.length < 2) {
                        key = pair[0];
                    }
                    return new AbstractMap.SimpleEntry<>(key, value);
                })
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(
                        Map.Entry::getValue, Collectors.toList()))
                );
    }

}
