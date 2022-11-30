package com.intracom.chat;

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
    private String serverHost;
    private int serverPort;
    private String function;
    private String registryHost;
    private int registryPort;
    private String serviceAddress;

    private ServerParameters(String serverPodname,
                             String serverHost,
                             int serverPort,
                             String function,
                             String registryHost,
                             int registryPort,
                             String serviceAddress)
    {
        this.serverPodname = serverPodname;
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.function = function;
        this.registryHost = registryHost;
        this.registryPort = registryPort;
        this.serviceAddress = serviceAddress;
    }

    private ServerParameters(ServerParameters oldInstance)
    {
        this.serverPodname = oldInstance.serverPodname;
        this.serverHost = oldInstance.serverHost;
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

    public String getServerHost()
    {
        return this.serverHost;
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

    public String getServiceAddress()
    {
        return this.serviceAddress;
    }

    @Override
    public String toString()
    {
        var parameters = new JsonObject();
        parameters.put("Backend server service pod name", serverPodname);
        parameters.put("Backend server service host", serverHost);
        parameters.put("Backend server service port", serverPort);
        parameters.put("Backend server service function", function);
        parameters.put("Service registry host", registryHost);
        parameters.put("Service registry port", registryPort);
        parameters.put("Service address", serviceAddress);
        return parameters.encode();
    }

    public static ServerParameters fromEnvironment() throws NumberFormatException, UnknownHostException
    {
        return new ServerParameters(EnvParams.get("HOSTNAME", "unknown"),
                                    EnvParams.get("SERVER_HOST", "chat-server"),
                                    Integer.parseInt(EnvParams.get("SERVER_PORT", 8080)),
                                    EnvParams.get("SERVER_FUNCTION", "unknown"),
                                    EnvParams.get("REGISTRY_HOST", "sd-registry"),
                                    Integer.parseInt(EnvParams.get("REGISTRY_PORT", 8080)),
                                    EnvParams.get("SERVICE_ADDRESS", InetAddress.getLocalHost().getHostAddress()));
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
                                       String serverHost,
                                       int serverPort,
                                       String function,
                                       String registryHost,
                                       int registryPort,
                                       String serviceAddress)
        {
            this.instance = new ServerParameters(serverPodname, //
                                                 serverHost, //
                                                 serverPort, //
                                                 function, //
                                                 registryHost, //
                                                 registryPort, //
                                                 serviceAddress);
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

        public ServerParametersBuilder withServerHost(String serverHost)
        {
            this.instance.serverHost = serverHost;
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

        public ServerParametersBuilder withServiceAddress(String serviceAddress)
        {
            this.instance.serviceAddress = serviceAddress;
            return this;
        }
    }
}
