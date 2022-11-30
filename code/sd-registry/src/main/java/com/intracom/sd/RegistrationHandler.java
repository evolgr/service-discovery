package com.intracom.sd;

import java.net.URI;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.intracom.common.utilities.Jackson;
import com.intracom.common.web.WebServer;
import com.intracom.model.Service;
import com.intracom.model.ServiceRegistry;
import com.intracom.model.ServiceRegistry.ServiceRegistryBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.functions.Predicate;
import io.vertx.reactivex.ext.web.RoutingContext;

/**
 * 
 */
public class RegistrationHandler
{
    private static final Logger log = LoggerFactory.getLogger(RegistrationHandler.class);
    private static final ObjectMapper json = Jackson.om();
    private static final URI REGISTRY_URI = URI.create("/registrations");

    private final WebServer server;
    private final Registrations registrations;

    public RegistrationHandler(RegistryParameters params,
                               Registrations registrations)
    {
        this.registrations = registrations;
        this.server = WebServer.builder() // create new webserver
                               .withHost(params.getServiceAddress()) // set registry address
                               .withPort(params.getPort()) // set registry port
                               .build(params.getVertx());

        // configure web server routers
        this.server.configureRouter(router -> router.get(REGISTRY_URI.getPath()) //
                                                    .handler(this::getRegisteredServices));
        this.server.configureRouter(router -> router.put(REGISTRY_URI.getPath()) //
                                                    .handler(this::registerServices));
    }

    public Completable start()
    {
        return Completable.complete() //
                          .andThen(this.server.startListener()) //
                          .onErrorResumeNext(t -> this.stop().andThen(Completable.error(t)));
    }

    public Completable stop()
    {
        final Predicate<? super Throwable> logError = t ->
        {
            log.warn("Ignored Exception during shutdown", t);
            return true;
        };

        return Completable.complete() //
                          .andThen(this.server.stopListener().onErrorComplete(logError));
    }

    public void getRegisteredServices(RoutingContext routingContext)
    {
        routingContext.request().bodyHandler(buffer ->
        {
            log.info("Handling request for extraction of registered services");
            try
            {
                ServiceRegistry serviceRegistry = json.readValue(buffer.toJsonObject().toString(), ServiceRegistry.class);
                var function = serviceRegistry.getFunction();
                log.info("Searching services belonging to function {}", function);

                List<Service> registeredServices = registrations.getRegistrations(function);
                if (registeredServices.isEmpty())
                {
                    log.warn("Failed to identify any registered services");
                    routingContext.response() // create response object
                                  .setStatusCode(HttpResponseStatus.NO_CONTENT.code()) // set response code 204
                                  .end(); // complete with response action
                }
                else
                {
                    ServiceRegistry newServiceRegistry = new ServiceRegistryBuilder(serviceRegistry).withServices(registeredServices) //
                                                                                                    .build();

                    routingContext.response() // create response object
                                  .setStatusCode(HttpResponseStatus.OK.code()) // set response code 200
                                  .end(json.registerModule(new JodaModule()) //
                                           .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //
                                           .writeValueAsString(newServiceRegistry)); // complete with response action
                }
            }
            catch (JsonProcessingException e)
            {
                log.error("Fetch registration request data with invalid format");
                routingContext.response() // create response object
                              .setStatusCode(HttpResponseStatus.BAD_REQUEST.code()) // set response code 400
                              .end(); // complete with response action
            }
        });
    }

    public void registerServices(RoutingContext routingContext)
    {
        routingContext.request().bodyHandler(buffer ->
        {
            log.info("Handling registration request");
            try
            {
                ServiceRegistry serviceRegistry = json.readValue(buffer.toJsonObject().toString(), ServiceRegistry.class);
                log.info("Registration request with data: {}", serviceRegistry);

                boolean result = registrations.addRegistration(serviceRegistry);
                if (result)
                {
                    log.error("Successfully registered service");
                    routingContext.response() // create response object
                                  .setStatusCode(HttpResponseStatus.CREATED.code()) // set response code 201
                                  .end(); // complete with response action
                }
                else
                {
                    log.error("Failed to registered service");
                    routingContext.response() // create response object
                                  .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) // set response code 500
                                  .end(); // complete with response action
                }
            }
            catch (JsonProcessingException e)
            {
                log.error("Registration request data with invalid format");
                routingContext.response() // create response object
                              .setStatusCode(HttpResponseStatus.BAD_REQUEST.code()) // set response code 400
                              .end(); // complete with response action
            }
        });
    }
}
