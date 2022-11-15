package com.intracom.common;

import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.vertx.core.http.HttpVersion;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.HttpResponse;

public class WebServerTest
{

    private static final Logger log = LoggerFactory.getLogger(WebServerTest.class);
    private final Vertx vertx = new VertxBuilder().build();

    @Test
    public void connection() throws URISyntaxException
    {
        var server = new WebServerBuilder().withHost("127.0.0.63") //
                                           .withPort(8080) //
                                           .build(vertx);
        server.configureRouter(router -> router.get() //
                                               .handler(rc -> rc.response() //
                                                                .end("web-server works")));
        server.startListener().blockingAwait();

        var client = new WebClientCBuilder().withOptions(options -> options.setProtocolVersion(HttpVersion.HTTP_2) //
                                                                           .setHttp2ClearTextUpgrade(false) //
                                                                           .setHttp2MaxPoolSize(4) //
                                                                           .setDefaultHost("127.0.0.63") //
                                                                           .setDefaultPort(server.actualPort()))
                                            .build(this.vertx);

        var serverURI = new URI("/best/test");
        var response = client.get()
                             .flatMap(c -> c.get(8080, "127.0.0.63", serverURI.getPath())
                                            .rxSend()
                                            .map(HttpResponse::bodyAsString)
                                            .doOnSuccess(log::info)
                                            .doOnError(e -> log.error("Failed to send request to server.", e)))
                             .retry(5)
                             .blockingGet();

        assertTrue(response.equals("web-server works"), "Server response do not match client received response");
        client.close();
        server.stopListener().blockingAwait();
    }

    @Test
    public void Termination() throws URISyntaxException
    {

        var server1 = new WebServerBuilder().withHost("127.0.0.66") //
                                            .withPort(8080) //
                                            .build(vertx);
        server1.configureRouter(router -> router.get() //
                                                .handler(rc -> rc.response() //
                                                                 .end("web-server works")));
        server1.startListener().blockingAwait();

        var server2 = new WebServerBuilder().withHost("127.0.0.66") //
                                            .withPort(8080) //
                                            .build(vertx);
        server2.configureRouter(router -> router.get() //
                                                .handler(rc -> rc.response() //
                                                                 .end("web-server works")));
        server2.startListener().blockingAwait();

        var client = new WebClientCBuilder().withOptions(options -> options.setProtocolVersion(HttpVersion.HTTP_2) //
                                                                           .setHttp2ClearTextUpgrade(false) //
                                                                           .setHttp2MaxPoolSize(4) //
                                                                           .setDefaultHost("127.0.0.66") //
                                                                           .setDefaultPort(server2.actualPort()))
                                            .build(this.vertx);

        var ans = new AtomicInteger(0);
        var cnt = 1000;
        var responses = Flowable.range(0, cnt) //
                                .flatMapSingle(tick -> client.get() //
                                                             .flatMap(c ->
                                                             {
                                                                 var uri = new URI("/best/test" + tick);
                                                                 log.info("Sending request to {}", uri.getPath());
                                                                 return c.get(8080, "127.0.0.66", uri.getPath())
                                                                         .rxSend()
                                                                         .map(HttpResponse::bodyAsString)
                                                                         .doAfterSuccess(resp ->
                                                                         {
                                                                             var count = ans.incrementAndGet();
                                                                             if (count == 10)
                                                                                 Completable.complete().andThen(server1.shutdown()).subscribe(() ->
                                                                                 {
                                                                                 }, err -> log.error("Unexpected error during server shutdown", err));
                                                                         })
                                                                         .doOnSuccess(log::info)
                                                                         .doOnError(e -> log.error("Failed to send request to server.", e))
                                                                         .retry(20);
                                                             }))
                                .toList()
                                .blockingGet();

        log.info("Stopping server 1");
        server1.stopListener().blockingAwait();
        log.info("Stopping server 2");
        server2.stopListener().blockingAwait();

        var allResponses = responses.size();
        log.info("Client responses: {}", allResponses);
        client.close();
        var successfulResponses = responses.stream() //
                                           .filter(data -> data.equalsIgnoreCase("web-server works")) //
                                           .collect(Collectors.toList()) //
                                           .size();
        log.info("Client successful responses: {}", successfulResponses);
        assertTrue(allResponses == successfulResponses, "Client unexpected receieved empty response.");

    }
}
