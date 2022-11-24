package com.intracom.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.common.web.WebServer;
import com.intracom.model.Message;
import com.intracom.model.Message.MessageBuilder;

import io.reactivex.Completable;
import io.reactivex.functions.Predicate;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

/**
 * 
 */
public class ChatHandler
{
    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);
    private final WebServer server;
    private final Message message;

    public ChatHandler(WebServer server)
    {
        // create web server
        this.server = server;
        this.message = new MessageBuilder()//
                .withId(null)
                .withMessage("The data exist.")
                .withOwner("God")
                .withRecipient(false)
                .withUser("YOU")
                .build();
    }

    public Completable start()
    {

        return Completable.fromAction(() ->
        {
            
            // respond with dummy message
            this.server.configureRouter(router -> router.get() //
                                                        .handler(rc -> rc.response() //
                                                                         .end((Handler<AsyncResult<Void>>) this.message)));
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
