package com.intracom.common;

import io.vertx.core.VertxOptions;
import io.vertx.reactivex.core.Vertx;

/**
 * 
 */
public class VertxBuilder
{
    private VertxOptions options;

    public VertxBuilder()
    {
        this.options = new VertxOptions();
    }

    public VertxBuilder(VertxOptions options)
    {
        this.options = options;
    }

    public Vertx build()
    {
        return Vertx.vertx(this.options);
    }
    
    public VertxBuilder withPreferNativeTransport()
    {
        this.options = this.options.setPreferNativeTransport(true);
        return this;
    }
}
