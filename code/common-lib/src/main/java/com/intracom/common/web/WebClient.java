package com.intracom.common.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.core.Vertx;

public class WebClient
{
    private static final Logger log = LoggerFactory.getLogger(WebClient.class);
    private final Single<io.vertx.reactivex.ext.web.client.WebClient> webClient;

    public WebClient(Vertx vertx,
                     WebClientBuilder builder)
    {
        final var init = Completable.complete();
        this.webClient = init.andThen(Single.fromCallable(() -> io.vertx.reactivex.ext.web.client.WebClient.create(vertx, builder.options))).cache();
    }

    public static WebClientBuilder builder()
    {
        return new WebClientBuilder();
    }

    public Single<io.vertx.reactivex.ext.web.client.WebClient> get()
    {
        return this.webClient;
    }

    public Completable close()
    {
        return Completable.complete() //
                          .andThen(this.webClient.flatMapCompletable(WebClient::closeWebClient)) //
                          .cache();
    }

    private static Completable closeWebClient(io.vertx.reactivex.ext.web.client.WebClient wc)
    {
        return Completable.fromAction(wc::close) //
                          .subscribeOn(Schedulers.io()) //
                          .doOnError(err -> log.warn("Failed to close WebClient", err)) //
                          .onErrorComplete();
    }
}
