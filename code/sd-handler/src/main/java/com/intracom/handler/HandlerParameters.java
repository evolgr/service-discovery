package com.intracom.handler;

import io.vertx.core.json.JsonObject;

import com.intracom.common.utilities.EnvParams;

public class HandlerParameters
{
    public static final HandlerParameters instance = new HandlerParameters();

    public final String hostname;
    public final String serviceHostname;
    public final int handlerServerPort;
    private final String username;
    private final String password;

    private HandlerParameters()
    {
        // pod name
        this.hostname = EnvParams.get("HOSTNAME");

        // service name
        this.serviceHostname = EnvParams.get("SERVICE_HOST", "eric-bsf-manager");

        // bsf manager web server port to be used for incoming requests by mediator,
        // yang-provider and pm-server
        this.handlerServerPort = Integer.parseInt(EnvParams.get("HANDLER_TARGET_PORT", 8082));
        
        this.username = EnvParams.get("USERNAME");
        this.password = EnvParams.get("PASSWORD");
    }
    
    public String getHostname()
    {
        return this.hostname;
    }
    
    public String getServiceHostname()
    {
        return this.serviceHostname;
    }
    
    public int getHandlerServerPort()
    {
        return this.handlerServerPort;
    }
    
    public String getUsername()
    {
        return this.username;
    }
    
    public String getPassword()
    {
        return this.password;
    }

    @Override
    public String toString()
    {
        var parameters = new JsonObject();
        parameters.put("hostname", hostname);
        parameters.put("serviceHostname", serviceHostname);
        parameters.put("handlerServerPort", handlerServerPort);
        return parameters.encode();
    }
}