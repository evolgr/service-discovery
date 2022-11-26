package com.intracom.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

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
    private String serverPodname;
    private String serverAddress;
    private int serverPort;
    private String function;
    private String registryHost;
    private int registryPort;

    private ServerParameters(String serverPodname,
                             String serverAddress,
                             int serverPort,
                             String function,
                             String registryHost,
                             int registryPort)
    {
        this.serverPodname = serverPodname;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.function = function;
        this.registryHost = registryHost;
        this.registryPort = registryPort;
    }

    private ServerParameters(ServerParameters oldInstance)
    {
        this.serverPodname = oldInstance.serverPodname;
        this.serverAddress = oldInstance.serverAddress;
        this.serverPort = oldInstance.serverPort;
        this.function = oldInstance.function;
        this.registryHost = oldInstance.registryHost;
        this.registryPort = oldInstance.registryPort;
    }

    public Vertx getVertx()
    {
        return this.vertx;
    }

    public String getServerPodname()
    {
        return this.serverPodname;
    }

    public String getServerAddress()
    {
        return this.serverAddress;
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
        parameters.put("Backend server service pod name", serverPodname);
        parameters.put("Backend server service address", serverAddress);
        parameters.put("Backend server service port", serverPort);
        parameters.put("Backend server service function", function);
        parameters.put("Service registry host", registryHost);
        parameters.put("Service registry port", registryPort);
        return parameters.encode();
    }

    public static ServerParameters fromEnvironment() throws NumberFormatException, UnknownHostException
    {
        return new ServerParameters(EnvParams.get("HOSTNAME", "unknown"),
                                    EnvParams.get("SERVER_ADDRESS", InetAddress.getLocalHost().getHostAddress()),
                                    Integer.parseInt(EnvParams.get("SERVER_PORT", 8080)),
                                    EnvParams.get("SERVER_FUNCTION", "unknown"),
                                    EnvParams.get("REGISTRY_HOST", "sd-registry"),
                                    Integer.parseInt(EnvParams.get("REGISTRY_PORT", 8080)));
    }

    public static class ServerParametersBuilder
    {
        protected ServerParameters instance;

        public ServerParametersBuilder() throws NumberFormatException, UnknownHostException
        {
            this.instance = ServerParameters.fromEnvironment();
        }

        public ServerParametersBuilder(ServerParameters oldInstance)
        {
            this.instance = new ServerParameters(oldInstance);
        }

        public ServerParametersBuilder(String serverPodname,
                                       String serverAddress,
                                       int serverPort,
                                       String function,
                                       String registryHost,
                                       int registryPort)
        {
            this.instance = new ServerParameters(serverPodname, serverAddress, serverPort, function, registryHost, registryPort);
        }

        public ServerParameters build()
        {
            ServerParameters result = this.instance;
            this.instance = null;
            return result;
        }

        public ServerParametersBuilder withServerPodname(String serverPodname)
        {
            this.instance.serverPodname = serverPodname;
            return this;
        }

        public ServerParametersBuilder withServerAddress(String serverAddress)
        {
            this.instance.serverAddress = serverAddress;
            return this;
        }

        public ServerParametersBuilder withServerPort(int serverPort)
        {
            this.instance.serverPort = serverPort;
            return this;
        }

        public ServerParametersBuilder withServerFunction(String function)
        {
            this.instance.function = function;
            return this;
        }

        public ServerParametersBuilder withRegistryHost(String registryHost)
        {
            this.instance.registryHost = registryHost;
            return this;
        }

        public ServerParametersBuilder withRegistryPort(int registryPort)
        {
            this.instance.registryPort = registryPort;
            return this;
        }
    }
}
