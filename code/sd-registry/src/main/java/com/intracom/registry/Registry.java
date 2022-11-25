package com.intracom.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.common.web.TerminateHook;
import com.intracom.common.web.WebServer;

public class Registry
{
    private static final Logger log = LoggerFactory.getLogger(Registry.class);
    private final WebServer serverHandler;
    private final WebServer serverChat;
    private final RequestHandler requestHandler;
    private final Registrations registrations;
    

    public Registry()
    {
		// empty constructor
        this.serverHandler = WebServer.builder().withHost(null).withPort(0).build(null);
        this.serverChat = WebServer.builder().withHost(null).withPort(0).build(null);
        this.requestHandler = new RequestHandler(this.serverHandler, "function");
        this.registrations = new Registrations(this.serverChat);
	}

	public static void main(String args[]) throws InterruptedException {
	    
	    var terminateCode = 0;
        log.info("Starting Handler");

        var registry = new Registry();
        
        try (var termination = new TerminateHook())
        {
            registry.requestHandler.start();
            registry.registrations.start();
        }
        catch (Exception e)
        {
            log.error("Handler terminated abnormally", e);
            terminateCode = 1;
        }

        registry.requestHandler.stop();
        registry.registrations.stop();
        
        log.info("Stopped Handler.");

        System.exit(terminateCode);
	    
	}
}
