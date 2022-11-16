package com.intracom.common.web;

import java.util.Objects;
import java.util.function.Consumer;

import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.Vertx;

public class WebServerBuilder
{

    final HttpServerOptions options;
    String host;
    int port;
    boolean httpTracing = false;
    boolean listenAll = false;

    public WebServerBuilder()
    {
        this.options = new HttpServerOptions();
        this.host = options.getHost();
        this.port = options.getPort();
    }

    public WebServerBuilder(WebServerBuilder builder)
    {
        this.options = new HttpServerOptions(builder.options);
        this.host = builder.host;
        this.port = builder.port;
        this.httpTracing = builder.httpTracing;
        this.listenAll = builder.listenAll;
    }

    public WebServer build(Vertx vertx)
    {
        return new WebServer(vertx, this);
    }

    public WebServerBuilder withPort(final int port)
    {
        if (port < 0)
            throw new IllegalArgumentException("Invalid port " + port);

        this.port = port;
        this.options.setPort(port);
        return this;
    }

    public WebServerBuilder withHost(final String host)
    {
        Objects.requireNonNull(host);

        this.host = host;
        this.options.setHost(host);
        return this;
    }

    public WebServerBuilder withOptions(Consumer<HttpServerOptions> httpServerOptionSetter)
    {
        Objects.requireNonNull(httpServerOptionSetter);

        httpServerOptionSetter.accept(this.options);
        return this;
    }
}
