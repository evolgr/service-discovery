package com.intracom.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;


public class WebClientC
{
    private static final Logger log = LoggerFactory.getLogger(WebClientC.class);
    private final Single<WebClient> webClient;
    private final String clienthostname;
    
    public static Builder builder()
    {
        return new Builder();
    }
    
    public WebClientC(Vertx vertx, Builder builder)
    {
		// empty constructor
        final var init = Completable.complete();
        this.webClient = init.andThen(Single.fromCallable(() -> WebClient.create(vertx, builder.options))).cache();   
        this.clienthostname = builder.clienthostname;
	}
    
    public Completable close()
    {
        final var finalize = Completable.complete();
        return finalize.andThen(this.webClient.flatMapCompletable(WebClientC::closeWebClient)).cache();
    }    
    
    private static Completable closeWebClient(WebClient wc)
    {
        return Completable.fromAction(wc::close) //
                          .subscribeOn(Schedulers.io())
                          .doOnError(err -> log.warn("Failed to close WebClient", err))
                          .onErrorComplete();
    }
    
    public static class Builder
    {
        private final WebClientOptions options = new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
        private String clienthostname; 

        public Builder withOptions(Consumer<WebClientOptions> options)
        {
            try
            {
                options.accept(this.options);
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("Failed to set WebClient otpions", e);
            }
            return this;
        }

        public Builder withHostName(String hostname)
        {
            this.clienthostname = hostname;
            return this;
        }

        public WebClientC build(Vertx vertx)
        {
            return new WebClientC(vertx, this);
        }

    }
    
    public String getHostName()
    {
        return this.clienthostname;
    }

	public static void main(String args[]) throws InterruptedException {
		while(true)
		{
			log.info("this is a simple example");
			TimeUnit.SECONDS.sleep(10);
		}
	}
}
