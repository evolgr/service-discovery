package com.intracom.sd;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.intracom.common.utilities.EnvParams;
import com.intracom.common.web.VertxBuilder;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

/**
 * 
 */
public class RegistryParameters
{
    private final Vertx vertx = new VertxBuilder().build();
    private String host;
    private int port;
    private String namespace;
    private long checkPeriod;
    private String serviceAddress;

    private RegistryParameters(String host,
                               int port,
                               String namespace,
                               long checkPeriod,
                               String serviceAddress)
    {
        this.host = host;
        this.port = port;
        this.namespace = namespace;
        this.checkPeriod = checkPeriod;
        this.serviceAddress = serviceAddress;
    }

    public Vertx getVertx()
    {
        return this.vertx;
    }

    public String getHost()
    {
        return this.host;
    }

    public int getPort()
    {
        return this.port;
    }

    public String getNamespace()
    {
        return this.namespace;
    }

    public long getCheckPeriod()
    {
        return this.checkPeriod;
    }

    public String getServiceAddress()
    {
        return this.serviceAddress;
    }

    @Override
    public String toString()
    {
        var parameters = new JsonObject();
        parameters.put("Registy host", this.host);
        parameters.put("Registry port", this.port);
        parameters.put("Namespace", this.namespace);
        parameters.put("Registrations check period", this.checkPeriod);
        parameters.put("Service address", serviceAddress);
        return parameters.encode();
    }

    public static RegistryParameters fromEnvironment() throws NumberFormatException, UnknownHostException
    {
        return new RegistryParameters(EnvParams.get("REGISTRY_HOSTNAME", "sd-registry"),
                                      Integer.parseInt(EnvParams.get("REGISTRY_PORT", "8080")),
                                      EnvParams.get("NAMESPACE", "best"),
                                      Long.parseLong(EnvParams.get("CHECK_PERIOD", "60")),
                                      EnvParams.get("SERVICE_ADDRESS", InetAddress.getLocalHost().getHostAddress()));
    }

    public static class RegistryParametersBuilder
    {
        protected RegistryParameters instance;

        public RegistryParametersBuilder() throws NumberFormatException, UnknownHostException
        {
            this.instance = RegistryParameters.fromEnvironment();
        }

        public RegistryParametersBuilder(String host,
                                         int port,
                                         String namespace,
                                         long checkPeriod,
                                         String serviceAddress)
        {
            this.instance = new RegistryParameters(host, //
                                                   port, //
                                                   namespace, //
                                                   checkPeriod, //
                                                   serviceAddress);
        }

        public RegistryParameters build()
        {
            RegistryParameters result = this.instance;
            this.instance = null;
            return result;
        }

        public RegistryParametersBuilder withHost(String host)
        {
            this.instance.host = host;
            return this;
        }

        public RegistryParametersBuilder withPort(int port)
        {
            this.instance.port = port;
            return this;
        }

        public RegistryParametersBuilder withNamespace(String namespace)
        {
            this.instance.namespace = namespace;
            return this;
        }

        public RegistryParametersBuilder withCheckPeriod(long checkPeriod)
        {
            this.instance.checkPeriod = checkPeriod;
            return this;
        }

        public RegistryParametersBuilder withServiceAddress(String serviceAddress)
        {
            this.instance.serviceAddress = serviceAddress;
            return this;
        }
    }
}
