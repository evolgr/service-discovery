package com.intracom.chat;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.chat.ServerParameters.ServerParametersBuilder;
import com.intracom.common.web.TerminateHook;

import io.reactivex.Completable;
import io.reactivex.functions.Predicate;

public class Server
{
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private final RegistrationHandler registrationHandler;
    private final ChatHandler chatHandler;
    private final ServerParameters params;
    private final TerminateHook termination;

    public Server(TerminateHook termination,
                  ServerParameters params) throws UnknownHostException
    {
        this.termination = termination;
        this.params = params;
        log.info("Starting chat service with parameters: {}", this.params);

        this.registrationHandler = new RegistrationHandler(this.params);
        this.chatHandler = new ChatHandler(this.params);
    }

    private Completable run()
    {
        return Completable.complete() //
                          .andThen(this.chatHandler.start())
                          .andThen(this.registrationHandler.start())
                          .andThen(this.termination.get())
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
                          .andThen(this.registrationHandler.stop().onErrorComplete(logError))
                          .andThen(this.chatHandler.stop().onErrorComplete(logError))
                          .andThen(this.params.getVertx().rxClose().onErrorComplete(logError));
    }

    public static void main(String args[]) throws InterruptedException, UnknownHostException
    {
        var terminateCode = 0;
        log.info("Staring Chat server");

        try (var termination = new TerminateHook())
        {
            var params = new ServerParametersBuilder().build();
            var server = new Server(termination, params);
            server.run().blockingAwait();
        }
        catch (Exception e)
        {
            log.error("Chat server terminated abnormally", e);
            terminateCode = 1;
        }

        log.info("Chat server stopped.");
        System.exit(terminateCode);
    }
}
