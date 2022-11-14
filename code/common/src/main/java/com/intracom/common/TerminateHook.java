package com.intracom.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Completable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class TerminateHook
{
    private static final Logger log = LoggerFactory.getLogger(TerminateHook.class);
    private final Completable vmShutdown;
    private final CompletableFuture<Object> shutdownComplete = new CompletableFuture<>();

    public TerminateHook()
    {
        this.vmShutdown = Completable.create(emitter ->
        {
            final var hook = new Thread(() ->
            {
                log.info("JVM is shutting down");
                emitter.onComplete();

                try
                {
                    shutdownComplete.get();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    // Ignore exception, continue JVM shutdown
                }
            });
            hook.setName(TerminateHook.class.getName());
            Runtime.getRuntime().addShutdownHook(hook);
        }).cache();
	}
    
    public Completable get()
    {
        return this.vmShutdown;
    }
}
