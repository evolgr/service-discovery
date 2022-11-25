package com.intracom.server;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.common.web.WebClient;
import com.intracom.model.Service;
import com.intracom.model.Service.ServiceBuilder;
import com.intracom.model.ServiceRegistry;
import com.intracom.model.ServiceRegistry.ServiceRegistryBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;

/**
 * 
 */
public class RegistrationHandler
{
    private static final Logger log = LoggerFactory.getLogger(RegistrationHandler.class);
    private static final URI REGISTRY_URI = URI.create("/registrations");

    private final WebClient client;
    private final ServerParameters params;
    private Disposable updater = null;

    public RegistrationHandler(ServerParameters params)
    {
        this.client = WebClient.builder().build(params.getVertx());
        this.params = params;
    }

    public Single<Object> put()
    {
        return this.client.get()
                          .map(wc -> wc.put(this.params.getRegistryPort(), //
                                            this.params.getRegistryHost(), //
                                            REGISTRY_URI.getPath())
                                       .ssl(false)
                                       .rxSendJson(this.getRegistrationData())
                                       .doOnError(t -> log.error("Something went wrong during registration of service: {}", t.getMessage()))
                                       .doOnSuccess(resp -> log.debug("Registration response with statusCode: {}, statudMessage: {}, body: {}",
                                                                      resp.statusCode(),
                                                                      resp.statusMessage(),
                                                                      resp.bodyAsString()))
                                       .map(resp ->
                                       {
                                           if (resp.statusCode() == HttpResponseStatus.OK.code())
                                           {
                                               log.info("Service successfully registered");
                                               return Completable.complete();
                                           }
                                           else
                                           {
                                               log.error("Failed to register service");
                                               return Completable.error(new RuntimeException("PUT request failed. statusCode: " + resp.statusCode() + ", body: "
                                                                                             + resp.bodyAsString()));
                                           }
                                       }));
    }

    public Completable start()
    {
        return Completable.fromAction(() ->
        {
            try
            {
                while (true)
                {
                    if (this.updater == null)
                    {
                        this.updater = this.update()
                                           .timeout(1500, TimeUnit.MILLISECONDS)
                                           .doOnSubscribe(e -> log.debug("Updating registration."))
                                           .doOnError(e -> log.debug("Updating registration failed. Cause: {}", e.toString()))
                                           .onErrorReturn(t -> HttpResponseStatus.OK.code())
                                           .repeatWhen(handler -> handler.delay(10, TimeUnit.SECONDS))
                                           .ignoreElements()
                                           .doOnSubscribe(d -> log.info("Started updating registration."))
                                           .subscribe(() -> log.info("Stopped updating registration."),
                                                      t -> log.error("Stopped updating registration. Cause: {}", t.toString()));

                        Thread.sleep(60 * 1000);
                    }
                }
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        });
    }

    public Completable stop()
    {
        final Predicate<? super Throwable> logErr = t ->
        {
            log.warn("Ignored Exception during shutdown", t);
            return true;
        };

        return Completable.complete()//
                          .andThen(this.client.close().onErrorComplete(logErr))
                          .andThen(Completable.fromAction(() ->
                          {
                              if (this.updater != null)
                              {
                                  this.updater.dispose();
                                  this.updater = null;
                              }
                          }));

    }

    private Single<Object> update()
    {
        return this.put();
    }

    private ServiceRegistry getRegistrationData() throws UnknownHostException
    {
        Service currentService = new ServiceBuilder().withHost(InetAddress.getLocalHost().getHostAddress()) // service ip address
                                                     .withName(this.params.getHostname()) // pod/service name
                                                     .withPort(Double.valueOf(this.params.getServerPort())) // server port
                                                     .withTimestamp(new DateTime()) // current date/time
                                                     .build();
        List<Service> services = new ArrayList<Service>();
        services.add(currentService);
        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().withFunction(this.params.function) //
                                                                      .withServices(services) //
                                                                      .build();
        log.info("Registration data {}", serviceRegistry);
        return serviceRegistry;
    }

}
