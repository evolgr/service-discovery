package com.intracom.server;

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
public class RegistrationClient
{
    private static final Logger log = LoggerFactory.getLogger(RegistrationClient.class);
    private final WebClient client;
    private final String function;
    private Disposable updater = null;
    private final ServiceRegistry registration;
    private final Service service;
    private final String hostRegistry;
    private final Double portRegistry;

    public RegistrationClient(WebClient client,
                              String function,
                              String ip,
                              String host,
                              Double port,
                              String hostRegistry,
                              Double portRegistry)
    {
        // create webclient
        this.client = client;
        this.function = function;
        this.service = new ServiceBuilder().withHost(host)//
                                           .withName(ip)
                                           .withPort(port)
                                           .withTimestamp(new DateTime())
                                           .build();
        this.registration = new ServiceRegistryBuilder()//
                                                        .withFunction(this.function)
                                                        .withServices((List) this.service)
                                                        .build();
        this.hostRegistry = hostRegistry;
        this.portRegistry = portRegistry;

    }

    public Single<Object> put()
    {
        return this.client.get()
                          .map(wc -> wc.put(this.portRegistry.intValue(), this.hostRegistry, "/registrations")
                                       .ssl(false)
                                       .rxSendJson(this.registration)
                                       .doOnError(throwable -> log.warn("Request {} update failed", throwable))
                                       .doOnSuccess(resp -> log.debug("PUT registration {}, statusCode: {}, statudMessage: {}, body: {}",
                                                                      registration,
                                                                      resp.statusCode(),
                                                                      resp.statusMessage(),
                                                                      resp.bodyAsString()))
                                       .map(resp -> resp.statusCode() == HttpResponseStatus.OK.code() ? Completable.complete()
                                                                                                      : Completable.error(new RuntimeException("PUT request failed. statusCode: "
                                                                                                                                               + resp.statusCode()
                                                                                                                                               + ", body: "
                                                                                                                                               + resp.bodyAsString()))));
    }

    public Completable start()
    {
        // continously send registrations every 1 min
        // to sd-registry service
        // with data according to ServiceRegistry.java
        // autogenerate timestamp
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
        // close web client
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

}
