/**
 * COPYRIGHT ERICSSON GMBH 2022
 *
 * The copyright to the computer program(s) herein is the property
 * of Ericsson GmbH, Germany.
 *
 * The program(s) may be used and/or copied only with the written
 * permission of Ericsson GmbH in accordance with
 * the terms and conditions stipulated in the agreement/contract
 * under which the program(s) have been supplied.
 *
 * Created on: Nov 14, 2022
 *     Author: zpalele
 */

package com.intracom.common;

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

    WebServerBuilder()
    {
        this.options = new HttpServerOptions();
        this.host = options.getHost();
        this.port = options.getPort();
    }

    WebServerBuilder(WebServerBuilder builder)
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
