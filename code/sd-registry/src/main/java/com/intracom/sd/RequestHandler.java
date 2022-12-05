package com.intracom.sd;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intracom.common.utilities.Jackson;
import com.intracom.common.web.WebServer;
import com.intracom.model.Service;
import com.intracom.model.ServiceRegistry;
import com.intracom.model.ServiceRegistry.ServiceRegistryBuilder;

import io.reactivex.Completable;
import io.reactivex.functions.Predicate;

/**
 * 
 */
public class RequestHandler
{
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private static final URI REGISTRY_URI = URI.create("/registrations");
    private final WebServer serverHandler;
    private final String function;
    private ServiceRegistry serviceRegistry;
    private static final ObjectMapper json = Jackson.om();

    public RequestHandler(WebServer serverHandler,
                          String function)
    {
        // create webserver
        this.serverHandler = serverHandler;
        this.function = function;
    }

    public Optional<List<Service>> getService(String function) throws JsonProcessingException
    {
        // return services that belong to specific function
        this.serviceRegistry = new ServiceRegistryBuilder().withFunction(function).build();
        this.serverHandler.configureRouter(router -> router.get(REGISTRY_URI.getPath()) //
                                                           .handler(rc ->
                                                           {
                                                               rc.request().bodyHandler(buffer ->
                                                               {
                                                                   try
                                                                   {
                                                                       this.serviceRegistry = json.readValue(buffer.toJsonObject().toString(),
                                                                                                             ServiceRegistry.class);
                                                                       log.info("Received service registry request: {}", this.serviceRegistry);
                                                                       rc.response() //
                                                                         .end();
                                                                   }
                                                                   catch (JsonProcessingException e)
                                                                   {
                                                                       e.printStackTrace();
                                                                   }
                                                               });
                                                           }));
        return this.serviceRegistry.getServices();
    }

    public Completable start()
    {
        // start webserver and listen on path /registrations
        // return registrations set of services

        return Completable.fromAction(() ->
        {
            this.getService(this.function);
            this.serverHandler.startListener().blockingAwait();

        });
    }

    public Completable stop()
    {
        // stop webserver
        final Predicate<? super Throwable> logErr = t ->
        {
            log.warn("Ignored Exception during shutdown", t);
            return true;
        };

        return Completable.complete()//
                          .andThen(this.serverHandler.startListener().onErrorComplete(logErr));
    }
}
