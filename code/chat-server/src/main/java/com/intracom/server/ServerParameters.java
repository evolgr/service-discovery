package com.intracom.server;

import com.intracom.common.utilities.EnvParams;
import com.intracom.common.web.VertxBuilder;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

/**
 * 
 */
public class ServerParameters
{

    private final Vertx vertx = new VertxBuilder().build();
    public String serverHostname;
    public int serverPort;
    public String function;
    public String registryHost;
    public int registryPort;

    private ServerParameters(String serverHostname,
                             int serverPort,
                             String function,
                             String registryHost,
                             int registryPort)
    {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
        this.function = function;
        this.registryHost = registryHost;
        this.registryPort = registryPort;
    }

    public Vertx getVertx()
    {
        return this.vertx;
    }

    public String getHostname()
    {
        return this.serverHostname;
    }

    public int getServerPort()
    {
        return this.serverPort;
    }

    public String getFunction()
    {
        return this.function;
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
        parameters.put("Backend server service hostname", serverHostname);
        parameters.put("Backend server service port", serverPort);
        parameters.put("Backend server service function", function);
        parameters.put("Service registry host", registryHost);
        parameters.put("Service registry port", registryPort);
        return parameters.encode();
    }

    public static ServerParameters fromEnvironment()
    {
        return new ServerParameters(EnvParams.get("HOSTNAME", "unknown"),
                                    Integer.parseInt(EnvParams.get("SERVER_PORT", 8080)),
                                    EnvParams.get("SERVER_FUNCTION", "unknown"),
                                    EnvParams.get("REGISTRY_HOST", "sd-registry"),
                                    Integer.parseInt(EnvParams.get("REGISTRY_PORT", 8080)));
    }

    public static class ServerParametersBuilder
    {
        protected ServerParameters instance;

        public ServerParametersBuilder()
        {
            this.instance = ServerParameters.fromEnvironment();
        }

        public ServerParametersBuilder(String serverHostname,
                                       int serverPort,
                                       String function,
                                       String registryHost,
                                       int registryPort)
        {
            this.instance = new ServerParameters(serverHostname, serverPort, function, registryHost, registryPort);
        }

        public ServerParameters build()
        {
            ServerParameters result = this.instance;
            this.instance = null;
            return result;
        }

        public ServerParametersBuilder withServerHostname(String serverHostname)
        {
            this.instance.serverHostname = serverHostname;
            return this;
        }

        public ServerParametersBuilder withServerPort(int port)
        {
            this.instance.serverPort = port;
            return this;
        }

        public ServerParametersBuilder withServerFunction(String function)
        {
            this.instance.function = function;
            return this;
        }

        public ServerParametersBuilder withRegistryHost(String host)
        {
            this.instance.registryHost = host;
            return this;
        }

        public ServerParametersBuilder withRegistryPort(int port)
        {
            this.instance.registryPort = port;
            return this;
        }
    }
}
