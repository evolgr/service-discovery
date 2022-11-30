package com.intracom.chat;

import static org.testng.Assert.assertTrue;

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
import com.intracom.chat.ServerParameters.ServerParametersBuilder;
import com.intracom.common.utilities.Jackson;
import com.intracom.common.web.WebServer;
import com.intracom.model.Request;
import com.intracom.model.Request.RequestBuilder;
import com.intracom.model.ServiceDiscovery.ServiceDiscoveryBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;

public class ChatHandlerTest
{

    private static final Logger log = LoggerFactory.getLogger(ChatHandlerTest.class);
    private static final URI CHAT_MESSAGES_URI = URI.create("/chat/messages");
    private static final ObjectMapper json = Jackson.om();
    private static final String CHAT_SERVER_LOCAL_HOST = "127.0.0.76";
    private static final Integer CHAT_SERVER_LOCAL_PORT = 8081;
    private static final String CHAT_FUNCTION = "chat";

    private final io.vertx.ext.web.client.WebClientOptions httpOptions = new io.vertx.ext.web.client.WebClientOptions().setProtocolVersion(HttpVersion.HTTP_2);

    private ChatHandler chatHandler;
    private ServerParameters params;
    private io.vertx.reactivex.ext.web.client.WebClient client;
    private WebServer chatServiceServer;

    @Test
    public void simpleRequest() throws JsonProcessingException
    {
        log.info(">>> Class: {}. Test: {}", ChatHandlerTest.class.getName(), "Test simple request to ChatHandler");

        // dummy serviceDiscovery request
        var recipients = new ArrayList<String>();
        var request = new RequestBuilder("user", "crazy message", new DateTime(), recipients).build();
        var requests = new ArrayList<Request>();
        requests.add(request);
        var serviceDiscovery = new ServiceDiscoveryBuilder().withFunction("chat") //
                                                            .withRequests(requests) //
                                                            .build();
        log.info("Service discovery: {}", serviceDiscovery);

        // get request
        var reqs = serviceDiscovery.getRequests();
        assertTrue(reqs.isPresent(), "No requests identified in service discovery data");
        var req = reqs.get().get(0);

        var testObserver = client.get(CHAT_SERVER_LOCAL_PORT, //
                                      CHAT_SERVER_LOCAL_HOST, //
                                      CHAT_MESSAGES_URI.getPath())
                                 .putHeader("Content-Type", "application/json")
                                 .rxSendJsonObject(new JsonObject(json.writeValueAsString(req)))
                                 .test();

        testObserver.awaitTerminalEvent();
        testObserver.assertComplete();
        testObserver.assertNoErrors();

        try
        {
            testObserver.assertValue(response ->
            {
                var resp = response.getDelegate();
                log.info("Response body: {}", resp.bodyAsString());
                return resp.bodyAsString() == null;
            });
        }
        catch (AssertionError expected)
        {
            assertTrue(expected.getMessage().startsWith("Value not present"), expected.getMessage());
        }

        try
        {
            testObserver.assertValue(response ->
            {
                var resp = response.getDelegate();
                log.info("Test observer response with code:{}, result message {} and body:{}", //
                         resp.statusCode(),
                         resp.statusMessage(),
                         resp.bodyAsString());
                return resp.statusCode() == HttpResponseStatus.ACCEPTED.code();
            });
//
//            testObserver.assertValue(response ->
//            {
//                var data = json.readValue(response.getDelegate().bodyAsJsonObject().toString(), Message.class);
//                return data.getMessage().equalsIgnoreCase(ChatHandler.dummyMessage.getMessage());
//            });
//
//            testObserver.assertValue(response ->
//            {
//                var data = json.readValue(response.getDelegate().bodyAsJsonObject().toString(), Message.class);
//                return data.getUser().equals(ChatHandler.dummyMessage.getUser());
//            });
        }
        finally
        {
            testObserver.dispose();
        }
    }

    @BeforeClass
    public void beforeClass() throws NumberFormatException, UnknownHostException
    {
        log.info(">>> Class: {}. Test: {}", ChatHandlerTest.class.getName(), "Before Class");

        // setup parameters for registration handler
        this.params = new ServerParametersBuilder().withServerPodname("my-pod-name") //
                                                   .withServerHost("my-chat-service") //
                                                   .withServiceAddress(CHAT_SERVER_LOCAL_HOST) //
                                                   .withServerPort(CHAT_SERVER_LOCAL_PORT) //
                                                   .withServerFunction(CHAT_FUNCTION) //
                                                   .withRegistryHost("2.2.2.2")
                                                   .withRegistryPort(999) //
                                                   .build();

        // server
        this.chatServiceServer = WebServer.builder() //
                                          .withHost(CHAT_SERVER_LOCAL_HOST) //
                                          .withPort(CHAT_SERVER_LOCAL_PORT) //
                                          .build(this.params.getVertx());

        // create registration handler
        this.chatHandler = new ChatHandler(params);
        log.info("Chat handler created.");

        // start chat handler
        this.chatHandler.start().blockingAwait();

        // create client
        this.client = io.vertx.reactivex.ext.web.client.WebClient.create(this.params.getVertx(), httpOptions);
    }

    @AfterClass
    public void afterClass()
    {
        log.info(">>> Class: {}. Test: {}", ChatHandlerTest.class.getName(), "After Class");

        // stop chat handler
        this.chatHandler.stop().blockingAwait();

        // stop client
        this.client.close();

        // stop server
        this.chatServiceServer.shutdown().blockingAwait();

        // close vertx
        this.params.getVertx().close();
    }
}
