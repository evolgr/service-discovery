package com.intracom.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.common.web.WebServer;

import io.reactivex.Completable;
import io.reactivex.functions.Predicate;

/**
 * 
 */
public class ChatHandler
{
    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);
    private final WebServer server;

    public ChatHandler(WebServer server)
    {
        // create web server
        this.server = server;
    }

    public Completable start()
    {

        return Completable.fromAction(() ->
        {
            // respond with dummy message
            this.server.configureRouter(router -> router.get() //
                                                        .handler(rc -> rc.response() //
                                                                 .end("Dummy message")));
            this.server.startListener().blockingAwait();
        });
    }

    public Completable stop()
    {
        // close webserver
        final Predicate<? super Throwable> logErr = t ->
        {
            log.warn("Ignored Exception during shutdown", t);
            return true;
        };
        
        return Completable.complete()//
                .andThen(this.server.startListener().onErrorComplete(logErr));
    }
}
