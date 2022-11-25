package com.intracom.registry;

import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.intracom.common.web.WebServer;

import io.reactivex.Completable;
import io.reactivex.functions.Predicate;

/**
 * 
 */
public class Registrations
{
    private static final Logger log = LoggerFactory.getLogger(Registrations.class);
    private static final URI REGISTRY_URI = URI.create("/registrations");
    private final WebServer serverChat;
    

    public Registrations(WebServer serverChat)
    {
        // create webserver
        this.serverChat = serverChat;
    }

    public void putService() throws JsonProcessingException
    {
        // update services that belong to specific function
        this.serverChat.configureRouter(router -> router.put(REGISTRY_URI.getPath()) //
                                                    .handler(rc ->
                                                    {
                                                        rc.request() //
                                                          .bodyHandler(buffer ->
                                                          {
                                                              log.info("Updated service received {}", buffer.toString());
                                                              rc.response().end();
                                                          });
                                                    }));
    }

    public Completable start()
    {
        // start webserver and update services
        return Completable.fromAction(() ->
        {
            this.putService();
            this.serverChat.startListener().blockingAwait();
        });
    }

    public Completable stop()
    {
        // stop webserver
        final Predicate<? super Throwable> logError = t ->
        {
            log.warn("Ignored Exception during shutdown", t);
            return true;
        };

        return Completable.complete()//
                          .doOnSubscribe(disp -> log.info("Initiated gracefull shutdown"))
                          .andThen(this.serverChat.stopListener().onErrorComplete(logError));
    }
}
