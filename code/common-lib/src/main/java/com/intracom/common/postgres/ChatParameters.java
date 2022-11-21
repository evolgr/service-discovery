package com.intracom.common.postgres;

import java.util.Base64;

import com.intracom.common.utilities.EnvironmentParameters;

import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.core.json.JsonObject;

/**
 * 
 */
public class ChatParameters
{
    private final String host;
    private final int port;
    private final String dbName;
    private final int connections;
    private final String user;
    private final String pass;
    private final PgPoolOptions pgPoolOptions;

    public ChatParameters(String host,
                          int port,
                          String dbName,
                          int connections,
                          String user,
                          String pass)
    {
        this.host = host;
        this.port = port;
        this.dbName = dbName;
        this.connections = connections;
        this.user = user;
        this.pass = pass;
        this.pgPoolOptions = new PgPoolOptions().setHost(this.host)
                                                .setPort(this.port)
                                                .setDatabase(this.dbName)
                                                .setUser(this.user)
                                                .setPassword(this.pass)
                                                .setMaxSize(this.connections);
    }

    public String getHost()
    {
        return this.host;
    }

    public int getPort()
    {
        return this.port;
    }

    public String getDbName()
    {
        return this.dbName;
    }

    public int getConnections()
    {
        return this.connections;
    }

    public String getUser()
    {
        return this.user;
    }

    public String getPass()
    {
        return this.pass;
    }

    public PgPoolOptions getPgPoolOptions()
    {
        return this.pgPoolOptions;
    }

    public static ChatParameters fromEnvironment()
    {
        return new ChatParameters(EnvironmentParameters.get("PG_HOST", "chat-db-postgresql"),
                                  Integer.parseInt(EnvironmentParameters.get("PG_PORT", 8432)),
                                  EnvironmentParameters.get("PG_DBNAME", "best"),
                                  Integer.parseInt(EnvironmentParameters.get("PG_MAX_CONNECTIONS", 10)),
                                  EnvironmentParameters.get("PG_USER", "best"),
                                  EnvironmentParameters.get("PG_PASS", "best"));
    }

    @Override
    public String toString()
    {
        final var parameters = new JsonObject();
        parameters.put("host", this.host);
        parameters.put("port", this.host);
        parameters.put("Database name", this.connections);
        parameters.put("Username", this.user);
        parameters.put("Password", Base64.getEncoder().encodeToString(this.pass.getBytes()));
        return parameters.encode();
    }
}
