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
import com.intracom.common.web.VertxBuilder;
import com.intracom.common.web.WebServer;
import com.intracom.model.Service;
import com.intracom.model.Service.ServiceBuilder;
import com.intracom.model.ServiceRegistry;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.reactivex.core.Vertx;

public class RegistrationHandlerTest
{
    private static final Logger log = LoggerFactory.getLogger(RegistrationHandlerTest.class);
    private static final URI REGISTRY_URI = URI.create("/registrations");
    private static final String REGISTRY_SERVER_LOCAL_HOST = "127.0.0.76";
    private static final Integer REGISTRY_SERVER_LOCAL_PORT = 8082;
    private static final ObjectMapper json = Jackson.om();
    private static final String CHAT_FUNCTION = "chat";

    private final Vertx vertx = new VertxBuilder().build();
    private final WebServer registryServiceServer = WebServer.builder() //
                                                             .withHost(REGISTRY_SERVER_LOCAL_HOST)
                                                             .withPort(REGISTRY_SERVER_LOCAL_PORT)
                                                             .build(this.vertx);
    private ServerParameters params;
    private RegistrationHandler registrationHandler;

    @BeforeClass
    public void beforeClass()
    {
        log.info(">>> Class: {}. Test: {}", RegistrationHandlerTest.class.getName(), "Before Class");

        // create expected service data
        Service expectedService = new ServiceBuilder().withHost("1.1.1.1") //
                                                      .withPort(Double.valueOf(666))
                                                      .withName("my-pod-name")
                                                      .withTimestamp(new DateTime())
                                                      .build();
        var expectedServices = new ArrayList<Service>();
        expectedServices.add(expectedService);

        // start handling requests from simulated registry service
        this.registryServiceServer.configureRouter(router -> router.put(REGISTRY_URI.getPath()).handler(routingContext ->
        {
            routingContext.request().bodyHandler(buffer ->
            {
                log.info("Registration request received {}", buffer.toString());
                try
                {
                    var registrationRequest = json.readValue(buffer.toJsonObject().toString(), ServiceRegistry.class);
                    assertTrue(registrationRequest.getFunction().equals(CHAT_FUNCTION), "Failed to identify expected chat function");

                    var services = registrationRequest.getServices();
                    assertTrue(services.isPresent(), "Failed to identify service data in registration request");
                    if (services.isEmpty())
                    {
                        routingContext.response() // create response object
                                      .setStatusCode(HttpResponseStatus.BAD_REQUEST.code()) // set response code 400
                                      .end(); // complete with response action
                    }
                    else
                    {
                        var candidateService = services.get().get(0);
                        log.info("Service: {}", candidateService);
                        log.info("Expected service: {}", expectedService);
                        assertTrue(candidateService.getHost().equals(expectedService.getHost()), "Failed to identify expected chat service hostname");
                        assertTrue(candidateService.getName().equals(expectedService.getName()), "Failed to identify expected chat service pod name");
                        assertTrue(candidateService.getPort().equals(expectedService.getPort()), "Failed to identify expected chat service port");
                        routingContext.response() //
                                      .setStatusCode(HttpResponseStatus.CREATED.code()) // set response to 201
                                      .end();
                    }
                }
                catch (JsonProcessingException e)
                {
                    log.error("Registration request data contains invalid format", e);
                    routingContext.response() // create response object
                                  .setStatusCode(HttpResponseStatus.BAD_REQUEST.code()) // set response code 400
                                  .end(); // complete with response action
                }
            });
        }));

        // starting web server
        this.registryServiceServer.startListener().blockingAwait();
        log.info("Registry Service server started.");
    }

    @AfterClass
    public void afterClass()
    {
        log.info(">>> Class: {}. Test: {}", ChatHandlerTest.class.getName(), "After Class");

        // stop registration handler
        this.registrationHandler.stop().blockingAwait();

        // stop and terminate web server
        this.registryServiceServer.shutdown().blockingAwait();

        // close vertx
        this.params.getVertx().close();
    }

    @Test
    public void checkRegistration() throws JsonProcessingException, InterruptedException, NumberFormatException, UnknownHostException
    {
        log.info(">>> Class: {}. Test: {}", RegistrationHandlerTest.class.getName(), "Test simple request to Registry");

        // setup parameters for registration handler
        this.params = new ServerParametersBuilder().withServerPodname("my-pod-name") //
                                                   .withServerHost("my-chat-service") //
                                                   .withServiceAddress("1.1.1.1") //
                                                   .withServerPort(666) //
                                                   .withServerFunction(CHAT_FUNCTION) //
                                                   .withRegistryHost(REGISTRY_SERVER_LOCAL_HOST)
                                                   .withRegistryPort(REGISTRY_SERVER_LOCAL_PORT) //
                                                   .build();

        // create registration handler
        this.registrationHandler = new RegistrationHandler(this.params);
        log.info("Registration handler created.");

        // trigger single registration
        var testObserver = this.registrationHandler.put().test();

        try
        {
            testObserver.awaitTerminalEvent();
            testObserver.assertComplete();
            testObserver.assertNoErrors();
            testObserver.assertValue(response ->
            {
                log.info("Test observer response with code:{}, result message {} and body:{}", //
                         response.statusCode(),
                         response.statusMessage(),
                         response.bodyAsString());
                return response.statusCode() == HttpResponseStatus.CREATED.code();
            });
            testObserver.assertValue(response -> response.body() == null);
        }
        finally
        {
            testObserver.dispose();
        }
    }
}
