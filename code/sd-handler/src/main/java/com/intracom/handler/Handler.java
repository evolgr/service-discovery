package com.intracom.handler;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.common.web.TerminateHook;
import com.intracom.common.web.WebClient;
import com.intracom.common.web.WebServer;

import io.reactivex.Completable;
import io.reactivex.functions.Predicate;
import io.vertx.reactivex.core.Vertx;

public class Handler
{
    private static final Logger log = LoggerFactory.getLogger(Handler.class);
    private final WebServer server;
    private final WebClient client;
    private final TerminateHook termination;
    private final Vertx vertx;

    public Handler(TerminateHook termination,
                   HandlerParameters params) throws UnknownHostException
    {
        this.termination = termination;
        this.vertx = params.getVertx();
        this.server = WebServer.builder() //
                               .withHost(InetAddress.getLocalHost().getHostAddress())
                               .withPort(params.getHandlerPort())
                               .build(this.vertx);
        this.client = WebClient.builder().build(params.getVertx());
        var requestHandler = new RequestHandler(this.server, this.client, params);
        requestHandler.createRouters();
    }

    private Completable run()
    {
        return Completable.complete()//
                          .andThen(this.server.startListener())
                          .andThen(this.termination.get())
                          .andThen(this.stop())
                          .onErrorResumeNext(t -> this.stop().andThen(Completable.error(t)));
    }

    private Completable stop()
    {
        final Predicate<? super Throwable> logError = t ->
        {
            log.warn("Ignored Exception during shutdown", t);
            return true;
        };

        return Completable.complete()//
                          .doOnSubscribe(disp -> log.info("Initiated gracefull shutdown"))
                          .andThen(this.server.stopListener().onErrorComplete(logError))
                          .andThen(this.client.close().onErrorComplete(logError))
                          .andThen(this.vertx.rxClose().onErrorComplete(logError));

    }

    public static void main(String args[])
    {
        var terminateCode = 0;
        log.info("Starting Handler");

        try (var termination = new TerminateHook())
        {
            var params = HandlerParameters.fromEnvironment();
            var handler = new Handler(termination, params);
            handler.run().blockingAwait();
        }
        catch (Exception e)
        {
            log.error("Handler terminated abnormally", e);
            terminateCode = 1;
        }

        log.info("Stopped Handler.");

        System.exit(terminateCode);
    }
}