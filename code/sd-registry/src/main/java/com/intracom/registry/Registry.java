package com.intracom.registry;

import java.io.IOException;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.common.web.TerminateHook;
import com.intracom.registry.RegistryParameters.RegistryParametersBuilder;

import io.reactivex.Completable;
import io.reactivex.functions.Predicate;

public class Registry
{
    private static final Logger log = LoggerFactory.getLogger(Registry.class);
    private final RegistrationHandler handler;
    private final Registrations registrations;
    private final RegistryParameters params;
    private final RegistrationExpirationHandler expirationHandler;

    public Registry(RegistryParameters params) throws URISyntaxException, IOException
    {
        this.params = params;
        this.registrations = new Registrations();
        this.expirationHandler = new RegistrationExpirationHandler(this.params, this.registrations);
        this.handler = new RegistrationHandler(this.params, this.registrations);
    }

    private Completable run()
    {
        return Completable.complete() //
                          .andThen(this.handler.start())
                          .andThen(this.expirationHandler.restart())
                          .onErrorResumeNext(t -> this.stop().andThen(Completable.error(t)))
                          .andThen(this.stop());
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
                          .andThen(this.handler.stop().onErrorComplete(logError))
                          .andThen(Completable.fromAction(() ->
                          {
                              if (this.expirationHandler.getDisposable() != null && !this.expirationHandler.getDisposable().isDisposed())
                                  this.expirationHandler.stop();
                              else
                                  Completable.complete();
                          }))
                          .andThen(this.params.getVertx().rxClose().onErrorComplete(logError));
    }

    public static void main(String args[]) throws InterruptedException
    {

        var terminateCode = 0;
        log.info("Starting Registry");

        try (var termination = new TerminateHook())
        {
            var params = new RegistryParametersBuilder().build();
            var registry = new Registry(params);
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
