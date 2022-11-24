package com.intracom.server;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.common.web.VertxBuilder;
import com.intracom.common.web.WebClient;
import com.intracom.common.web.WebServer;

import io.vertx.reactivex.core.Vertx;

public class Server
{
    private static final Logger log = LoggerFactory.getLogger(Server.class);
    private final RegistrationClient registrationClient;
    private final ChatHandler chatHandler;
    private final WebServer webServer;
    private final WebClient webClient;
    private final Vertx vertx = new VertxBuilder().build();

    public Server() throws UnknownHostException
    {
        // empty constructor
        this.webClient = WebClient.builder().build(this.vertx);
        this.registrationClient = new RegistrationClient(this.webClient, "function", null, "ChatServerHost", null, "RegistrationHost", null);
        this.webServer = WebServer.builder()//
                                  .withHost(InetAddress.getLocalHost().getHostAddress())
                                  .withPort((Integer) null)
                                  .build(this.vertx);
        this.chatHandler = new ChatHandler(this.webServer);
    }

    public static void main(String args[]) throws InterruptedException, UnknownHostException
    {
        Server server = new Server();
        server.registrationClient.start();
        server.chatHandler.start();

        server.registrationClient.stop();
        server.chatHandler.stop();
    }
}
