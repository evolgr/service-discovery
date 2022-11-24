package com.intracom.common.web;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Completable;

public class TerminateHook implements AutoCloseable
{
    private static final Logger log = LoggerFactory.getLogger(TerminateHook.class);
    private final Completable terminate;
    private final CompletableFuture<Object> termination = new CompletableFuture<>();

    public TerminateHook()
    {
        this.terminate = Completable.create(emitter ->
        {
            final var hook = new Thread(() ->
            {
                log.info("JVM is shutting down");
                emitter.onComplete();

                try
                {
                    termination.get();
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                }
                catch (ExecutionException e)
                {
                    // Continue JVM shutdown and log exception
                    log.error("Error during shutdown", e);
                }
            });
            hook.setName(TerminateHook.class.getName());
            Runtime.getRuntime().addShutdownHook(hook);
        }).cache();
	}
    
    public Completable get()
    {
        return this.terminate;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.AutoCloseable#close()
     */
    @Override
    public void close()
    {
        this.termination.complete(new Object());
    }
}
