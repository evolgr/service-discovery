package com.intracom.sd;

import java.io.IOException;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.common.web.TerminateHook;
import com.intracom.sd.RegistryParameters.RegistryParametersBuilder;

import io.kubernetes.client.openapi.ApiException;
import io.reactivex.Completable;
import io.reactivex.functions.Predicate;

public class Registry
{
    private static final Logger log = LoggerFactory.getLogger(Registry.class);
    private final RegistrationHandler handler;
    private final Registrations registrations;
    private final RegistryParameters params;
//    private final RegistrationExpirationHandler expirationHandler;
    private final TerminateHook termination;

    public Registry(TerminateHook termination,
                    RegistryParameters params) throws URISyntaxException, IOException, ApiException
    {
        this.termination = termination;
        this.params = params;
        this.registrations = new Registrations();
// TODO: use expiration handler for expiration/clearing of invalid services
//        this.expirationHandler = new RegistrationExpirationHandler(this.params, this.registrations);
        this.handler = new RegistrationHandler(this.params, this.registrations);
    }

    private Completable run()
    {
        return Completable.complete() //
                          .andThen(this.handler.start())
//                          .andThen(this.expirationHandler.start())
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

        return Completable.complete() //
                          .doOnSubscribe(disposable -> log.info("Initiated gracefull shutdown"))
//                          .andThen(Completable.fromAction(() ->
//                          {
//                              if (this.expirationHandler.getDisposable() != null && !this.expirationHandler.getDisposable().isDisposed())
//                                  this.expirationHandler.stop();
//                              else
//                                  Completable.complete();
//                          }))
                          .andThen(this.handler.stop().onErrorComplete(logError))
                          .andThen(this.params.getVertx().rxClose().onErrorComplete(logError));
    }

    public static void main(String args[]) throws InterruptedException
    {

        var terminateCode = 0;
        log.info("Starting Registry");

        try (var termination = new TerminateHook())
        {
            var params = new RegistryParametersBuilder().build();
            log.info("Starting service discovery registry service with parameters: {}", params);

            var registry = new Registry(termination, params);
            registry.run().blockingAwait();
        }
        catch (Exception e)
        {
            log.error("Registry terminated abnormally", e);
            terminateCode = 1;
        }

        log.info("Registry stopped.");
        System.exit(terminateCode);

    }
}
