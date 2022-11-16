package com.intracom.common.web;

import io.reactivex.functions.Consumer;
import io.vertx.core.http.HttpVersion;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;

public class WebClientCBuilder
{
    final WebClientOptions options = new WebClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
    String hostname;

    public WebClientCBuilder()
    {
        // empty constructor
    }

    public WebClientCBuilder withOptions(Consumer<WebClientOptions> options)
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

    public WebClientC build(Vertx vertx)
    {
        return new WebClientC(vertx, this);
    }
}
