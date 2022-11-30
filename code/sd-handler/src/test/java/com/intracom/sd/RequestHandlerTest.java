package com.intracom.sd;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.intracom.common.utilities.Jackson;
import com.intracom.common.web.VertxBuilder;
import com.intracom.common.web.WebClient;
import com.intracom.common.web.WebServer;
import com.intracom.model.Message;
import com.intracom.model.Message.MessageBuilder;
import com.intracom.model.Request;
import com.intracom.model.Request.RequestBuilder;
import com.intracom.model.Service;
import com.intracom.model.Service.ServiceBuilder;
import com.intracom.model.ServiceDiscovery.ServiceDiscoveryBuilder;
import com.intracom.model.ServiceRegistry.ServiceRegistryBuilder;
import com.intracom.sd.HandlerParameters.HandlerParametersBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;

public class RequestHandlerTest
{
    private static final Logger log = LoggerFactory.getLogger(RequestHandlerTest.class);
    private static final String BACKEND_SERVER_LOCAL_HOST = "127.0.0.69";
    private static final String REGISTRY_SERVER_LOCAL_HOST = "127.0.0.71";
    private static final String HANDLER_SERVER_LOCAL_HOST = "127.0.0.73";
    private static final URI CHAT_MESSAGES_URI = URI.create("/chat/messages");
    private static final URI REGISTRY_URI = URI.create("/registrations");
    private static final ObjectMapper json = Jackson.om();

    private final Vertx vertx = new VertxBuilder().build();
    private final WebServer backendServiceServer = WebServer.builder() //
                                                            .withHost(BACKEND_SERVER_LOCAL_HOST)
                                                            .withPort(this.getAvailablePort(BACKEND_SERVER_LOCAL_HOST))
                                                            .build(this.vertx);

    private final WebServer registryServiceServer = WebServer.builder() //
                                                             .withHost(REGISTRY_SERVER_LOCAL_HOST)
                                                             .withPort(this.getAvailablePort(REGISTRY_SERVER_LOCAL_HOST))
                                                             .build(this.vertx);

    private final WebClient internalClient = WebClient.builder().build(this.vertx);

    private final WebServer handlerServiceServer = WebServer.builder() //
                                                            .withHost(HANDLER_SERVER_LOCAL_HOST)
                                                            .withPort(this.getAvailablePort(HANDLER_SERVER_LOCAL_HOST))
                                                            .build(this.vertx);
    private final io.vertx.ext.web.client.WebClientOptions httpOptions = new io.vertx.ext.web.client.WebClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
    private final io.vertx.reactivex.ext.web.client.WebClient externalClient = io.vertx.reactivex.ext.web.client.WebClient.create(vertx, httpOptions);

    // 1. client (simulated) -> handler: initial request
    // 2. handler -> registry (simulated): check for registered service
    // 3. handler -> server (simulated): forward request
    // 4. server (simulated) -> handler: response to forwarded request
    // 5. handler -> client (simulated): response to initial request

    @BeforeClass
    private void beforeClass() throws JsonProcessingException
    {

        var message = json.writeValueAsString(new MessageBuilder().withId(666L) //
                                                                  .withUser("user") //
                                                                  .withMessage("response message")
                                                                  .withRecipient(false) //
                                                                  .build());

        this.backendServiceServer.configureRouter(router -> router.get(CHAT_MESSAGES_URI.getPath()) //
                                                                  .handler(rc ->
                                                                  {
                                                                      rc.request() //
                                                                        .bodyHandler(buffer ->
                                                                        {
                                                                            log.info("Simulated backend server received {}", buffer.toString());
                                                                            rc.response().end(message);
                                                                        });
                                                                  }));

        this.backendServiceServer.startListener().blockingAwait();
        log.info("Backend Service server started.");

        Service service = new ServiceBuilder().withHost(BACKEND_SERVER_LOCAL_HOST) //
                                              .withPort(Double.valueOf(this.backendServiceServer.actualPort()))
                                              .withName("internal-service")
                                              .withTimestamp(new DateTime())
                                              .build();
        var data = new ArrayList<Service>();
        data.add(service);
        var serviceRegistry = new ServiceRegistryBuilder().withFunction("chat") //
                                                          .withServices(data)
                                                          .build();
        var responseData = json.registerModule(new JodaModule()) //
                               .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //
                               .writeValueAsString(serviceRegistry);

        this.registryServiceServer.configureRouter(router -> router.get(REGISTRY_URI.getPath()).handler(rc ->
        {
            rc.request().bodyHandler(buffer ->
            {
                log.info("Simulated registry server received {}", buffer.toString());
                rc.response() //
                  .setStatusCode(HttpResponseStatus.FOUND.code()) //
                  .end(responseData);
            });
        }));

        this.registryServiceServer.startListener().blockingAwait();
        log.info("Registry Service server started.");

        this.handlerServiceServer.startListener().blockingAwait();
        log.info("Handler Service server started.");
    }

    @AfterClass
    private void afterClass()
    {
        this.externalClient.close();
        this.backendServiceServer.shutdown().blockingAwait();
//        this.backendServiceServer.stopListener().blockingAwait();
        this.backendServiceServer.shutdown().blockingAwait();
//        this.backendServiceServer.stopListener().blockingAwait();
        this.registryServiceServer.shutdown().blockingAwait();
//        this.registryServiceServer.stopListener().blockingAwait();
        this.vertx.close();
    }

    @Test
    public void simpleRequestTest() throws InterruptedException, JsonProcessingException, NumberFormatException, UnknownHostException
    {
        var parameters = new HandlerParametersBuilder().withHandlerPort(this.handlerServiceServer.actualPort()) //
                                                       .withRegistryHost(REGISTRY_SERVER_LOCAL_HOST) //
                                                       .withRegistryPort(this.registryServiceServer.actualPort()) //
                                                       .withServiceAddress("1.1.1.1")
                                                       .build();
        var requestHandler = new RequestHandler(this.handlerServiceServer, this.internalClient, parameters);
        requestHandler.createRouters();

        var recipients = new ArrayList<String>();
        var request = new RequestBuilder("user", "crazy message", new DateTime(), recipients).build();
        var requests = new ArrayList<Request>();
        requests.add(request);
        var serviceDiscovery = new ServiceDiscoveryBuilder().withFunction("chat") //
                                                            .withRequests(requests) //
                                                            .build();
        log.warn("Initial service discovery from external client {}", serviceDiscovery);

        var testObserver = externalClient.get(handlerServiceServer.actualPort(), HANDLER_SERVER_LOCAL_HOST, CHAT_MESSAGES_URI.getPath())
                                         .putHeader("Content-Type", "application/json")
                                         .rxSendJsonObject(new JsonObject(json.writeValueAsString(serviceDiscovery)))
                                         .test();

        try
        {
            testObserver.awaitTerminalEvent();
            testObserver.assertComplete();
            testObserver.assertNoErrors();
            var message = new MessageBuilder().withId(666L) //
                                              .withUser("user") //
                                              .withMessage("response message")
                                              .withRecipient(false) //
                                              .build();

            testObserver.assertValue(response ->
            {
                var resp = response.getDelegate();
                log.info("Test observer response with code:{}, result message {} and body:{}", //
                         resp.statusCode(),
                         resp.statusMessage(),
                         resp.bodyAsString());
                var data = json.readValue(response.bodyAsJsonObject().toString(), Message.class);
                return data.getId().equals(message.getId());
            });

            testObserver.assertValue(response ->
            {
                var resp = response.getDelegate();
                log.info("Test observer response with code:{}, result message {} and body:{}", //
                         resp.statusCode(),
                         resp.statusMessage(),
                         resp.bodyAsString());
                var data = json.readValue(response.bodyAsJsonObject().toString(), Message.class);
                return data.getMessage().equalsIgnoreCase(message.getMessage());
            });

            testObserver.assertValue(response ->
            {
                var resp = response.getDelegate();
                log.info("Test observer response with code:{}, result message {} and body:{}", //
                         resp.statusCode(),
                         resp.statusMessage(),
                         resp.bodyAsString());
                var data = json.readValue(response.bodyAsJsonObject().toString(), Message.class);
                return data.getUser().equals(message.getUser());
            });
        }
        finally
        {
            testObserver.dispose();
        }
    }

    public Integer getAvailablePort(String host)
    {
        var port = 0;
        try
        {
            var address = InetAddress.getByName(host);
            try (var socket = new ServerSocket(0, 100, address))
            {
                port = socket.getLocalPort();
            }
        }
        catch (Exception e)
        {
            log.error("Failed to get available port for {}", host);
        }
        return port;
    }
}
