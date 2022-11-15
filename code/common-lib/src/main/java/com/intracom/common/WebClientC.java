package com.intracom.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;

public class WebClientC
{
    private static final Logger log = LoggerFactory.getLogger(WebClientC.class);
    private final Single<WebClient> webClient;

    public WebClientC(Vertx vertx,
                      WebClientCBuilder builder)
    {
        final var init = Completable.complete();
        this.webClient = init.andThen(Single.fromCallable(() -> WebClient.create(vertx, builder.options))).cache();
    }

    public Single<WebClient> get()
    {
        return this.webClient;
    }
    public Completable close()
    {
        return Completable.complete() //
                          .andThen(this.webClient.flatMapCompletable(WebClientC::closeWebClient)) //
                          .cache();
    }

    private static Completable closeWebClient(WebClient wc)
    {
        return Completable.fromAction(wc::close) //
                          .subscribeOn(Schedulers.io()) //
                          .doOnError(err -> log.warn("Failed to close WebClient", err)) //
                          .onErrorComplete();
    }
}
