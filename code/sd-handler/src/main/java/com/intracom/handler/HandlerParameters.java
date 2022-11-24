package com.intracom.handler;

import com.intracom.common.utilities.EnvParams;
import com.intracom.common.web.VertxBuilder;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

public class HandlerParameters
{
    public int handlerPort;
    public String registryHost;
    public int registryPort;
    private final Vertx vertx = new VertxBuilder().build();

    private HandlerParameters(int handlerPort,
                              String registryHost,
                              int registryPort)
    {
        this.handlerPort = handlerPort;
        this.registryHost = registryHost;
        this.registryPort = registryPort;
    }

    public Vertx getVertx()
    {
        return this.vertx;
    }

    public int getHandlerPort()
    {
        return this.handlerPort;
    }

    public String getRegistryHost()
    {
        return this.registryHost;
    }

    public int getRegistryPort()
    {
        return this.registryPort;
    }

    @Override
    public String toString()
    {
        var parameters = new JsonObject();
        parameters.put("Service discovery handler port", handlerPort);
        parameters.put("Service registry host", registryHost);
        parameters.put("Service registry port", registryPort);
        return parameters.encode();
    }

    public static HandlerParameters fromEnvironment()
    {
        return new HandlerParameters(Integer.parseInt(EnvParams.get("HANDLER_PORT", 8080)),
                                     EnvParams.get("REGISTRY_HOST", "sd-registry"),
                                     Integer.parseInt(EnvParams.get("REGISTRY_PORT", 8080)));
    }

    public static class HandlerParametersBuilder
    {
        protected HandlerParameters instance;

        public HandlerParametersBuilder()
        {
            this.instance = HandlerParameters.fromEnvironment();
        }

        public HandlerParametersBuilder(int handlerPort,
                                        String registryHost,
                                        int registryPort)
        {
            this.instance = new HandlerParameters(handlerPort, registryHost, registryPort);
        }

        public HandlerParameters build()
        {
            HandlerParameters result = this.instance;
            this.instance = null;
            return result;
        }

        public HandlerParametersBuilder withHandlerPort(int port)
        {
            this.instance.handlerPort = port;
            return this;
        }

        public HandlerParametersBuilder withRegistryHost(String host)
        {
            this.instance.registryHost = host;
            return this;
        }

        public HandlerParametersBuilder withRegistryPort(int port)
        {
            this.instance.registryPort = port;
            return this;
        }
    }
}