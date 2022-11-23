package com.intracom.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.common.web.VertxBuilder;
import com.intracom.common.web.WebClientC;
import com.intracom.common.web.WebServer;

import io.reactivex.Completable;
import io.vertx.reactivex.core.Vertx;

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

public class Server
{
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private final RegistrationClient registrationClient;
    private final ChatHandler chatHandler;
    private final WebServer webServer;
    private final WebClientC webClient;
    private final Vertx vertx = new VertxBuilder().build();

    public Server()
    {
        // empty constructor
        this.webServer = WebServer.builder()//
                                  .withHost(InetAddress.getLocalHost().getHostAddress())
                                  .withPort(port)
                                  .build(this.vertx);

        this.webClient = WebClientC.builder().build(this.vertx);

        this.registrationClient = new RegistrationClient(this.webClient, null, null, null, 200);
        this.chatHandler = new ChatHandler(this.webServer);
    }
    
    public static void main(String args[]) throws InterruptedException
    {

        Server server = new Server();
        server.registrationClient.start(null);
        server.chatHandler.start();
        
        server.registrationClient.stop();
        server.chatHandler.stop();

//		while(true)
//		{
//			log.info("this is a simple example");
//			TimeUnit.SECONDS.sleep(10);
//		}
    }
}
