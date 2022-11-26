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
 * Created on: Nov 26, 2022
 *     Author: ekoteva
 */

package com.intracom.registry;

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
    private long expirationPeriod;

    private RegistryParameters(String host,
                               int port,
                               String namespace,
                               long expirationPeriod)
    {
        this.host = host;
        this.port = port;
        this.namespace = namespace;
        this.expirationPeriod = expirationPeriod;
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

    public long getExpirationPeriod()
    {
        return this.expirationPeriod;
    }

    @Override
    public String toString()
    {
        var parameters = new JsonObject();
        parameters.put("Registy host", this.host);
        parameters.put("Registry port", this.port);
        parameters.put("Namespace", this.namespace);
        parameters.put("Registrations expiration period", this.expirationPeriod);
        return parameters.encode();
    }

    public static RegistryParameters fromEnvironment()
    {
        return new RegistryParameters(EnvParams.get("REGISTRY_HOSTNAME", "sd-registry"),
                                      Integer.parseInt(EnvParams.get("REGISTRY_PORT", "8080")),
                                      EnvParams.get("NAMESPACE", "best"),
                                      Long.parseLong(EnvParams.get("EXPIRATION_PERIOD", "60")));
    }

    public static class RegistryParametersBuilder
    {
        protected RegistryParameters instance;

        public RegistryParametersBuilder()
        {
            this.instance = RegistryParameters.fromEnvironment();
        }

        public RegistryParametersBuilder(String host,
                                         int port,
                                         String namespace,
                                         long expirationPeriod)
        {
            this.instance = new RegistryParameters(host, port, namespace, expirationPeriod);
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

        public RegistryParametersBuilder withExpiration(int expirationPeriod)
        {
            this.instance.expirationPeriod = expirationPeriod;
            return this;
        }
    }
}
