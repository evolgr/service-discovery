package com.intracom.sd;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intracom.common.utilities.Jackson;
import com.intracom.model.Service;
import com.intracom.model.Service.ServiceBuilder;
import com.intracom.model.ServiceRegistry;
import com.intracom.model.ServiceRegistry.ServiceRegistryBuilder;
import com.intracom.sd.RegistryParameters.RegistryParametersBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.json.JsonObject;;

public class RegistrationHandlerTest
{
    private static final Logger log = LoggerFactory.getLogger(RegistrationHandlerTest.class);
    private static final URI REGISTRY_URI = URI.create("/registrations");
    private static final String REGISTRY_SERVER_LOCAL_HOST = "127.0.0.76";
    private static final Integer REGISTRY_SERVER_LOCAL_PORT = 8083;
    private static final ObjectMapper json = Jackson.om();

    private Registrations registrations;
    private RegistrationHandler handler;
    private final io.vertx.ext.web.client.WebClientOptions httpOptions = new io.vertx.ext.web.client.WebClientOptions().setProtocolVersion(HttpVersion.HTTP_2);
    private io.vertx.reactivex.ext.web.client.WebClient client;
    private RegistryParameters params;

    @BeforeMethod
    public void beforeMethod()
    {
    }

    @AfterMethod
    public void afterMethod()
    {
        var functions = this.registrations.getFunctions();
        if (!functions.isEmpty())
            this.registrations.clearFunctions();
    }

    @BeforeClass
    public void beforeClass() throws NumberFormatException, UnknownHostException
    {
        this.params = new RegistryParametersBuilder().withHost("my-registry-service") //
                                                     .withPort(REGISTRY_SERVER_LOCAL_PORT) //
                                                     .withNamespace("best") //
                                                     .withExpiration(60) //
                                                     .withServiceAddress(REGISTRY_SERVER_LOCAL_HOST) //
                                                     .build();
        this.registrations = new Registrations();
        this.handler = new RegistrationHandler(params, this.registrations);

        // start handler
        this.handler.start().blockingAwait();

        // create client
        this.client = io.vertx.reactivex.ext.web.client.WebClient.create(params.getVertx(), httpOptions);
    }

    @AfterClass
    public void afterClass()
    {
        // stop client
        this.client.close();

        // stop handler
        this.handler.stop().blockingAwait();

        // close vertx
        this.params.getVertx().close();
    }

    @Test
    public void registrationHandlerTest() throws JsonProcessingException
    {
        // create dummy service registry data
        DateTime dt = new DateTime();
        Service service = new ServiceBuilder().withHost("test-host") //
                                              .withName("test-pod-name") //
                                              .withPort(Double.valueOf("666"))
                                              .withTimestamp(dt)
                                              .build();
        List<Service> services = new ArrayList<>();
        services.add(service);
        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().withFunction("test-function") //
                                                                      .withServices(services)
                                                                      .build();

        assertTrue(this.registrations.getFunctions().isEmpty(), "Error registered functions are not empty");

        var testObserver = this.client.put(REGISTRY_SERVER_LOCAL_PORT, //
                                           REGISTRY_SERVER_LOCAL_HOST, //
                                           REGISTRY_URI.getPath())
                                      .putHeader("Content-Type", "application/json")
                                      .rxSendJsonObject(new JsonObject(json.writeValueAsString(serviceRegistry)))
                                      .test();

        try
        {
            testObserver.awaitTerminalEvent();
            testObserver.assertComplete();
            testObserver.assertNoErrors();

            testObserver.assertValue(response ->
            {
                var resp = response.getDelegate();
                log.info("Test observer response with code:{}, result message {} and body:{}", //
                         resp.statusCode(),
                         resp.statusMessage(),
                         resp.bodyAsString());
                return resp.statusCode() == HttpResponseStatus.CREATED.code();
            });
        }
        finally
        {
            testObserver.dispose();
        }

        var registeredFunctions = this.registrations.getFunctions();
        log.info("Registered functions {}", registeredFunctions);
        assertFalse(registeredFunctions.isEmpty(), "Error registered functions are empty");
        assertTrue(registeredFunctions.size() == 1, "Error registered functions are not as many as expected");

        var registeredServices = this.registrations.getRegistrations("test-function");
        log.info("Registered services {}", registeredServices);
        assertFalse(registeredServices.isEmpty(), "Error registered services for test-function are missing");
        assertTrue(registeredServices.size() == 1, "Error registered services for test-function are not as many as expected");

        var registeredService = registeredServices.get(0);
        log.info("Registered service {}", registeredService);
        assertTrue(registeredService.getHost().equals(service.getHost()), "Error registered service host is wrong");
        assertTrue(registeredService.getName().equals(service.getName()), "Error registered service pod name is wrong");
        assertTrue(registeredService.getPort().equals(service.getPort()), "Error registered service port is wrong");
        var registeredServiceDt = registeredService.getTimestamp();
        var expectedServiceDt = service.getTimestamp();
        assertTrue(registeredServiceDt.isPresent(), "Error date time is missing from registred service");
        if (registeredServiceDt.isPresent() && expectedServiceDt.isPresent())
        {
            log.info("Expected date: {}", expectedServiceDt.get());
            log.info("registeered date: {}", registeredServiceDt.get());
            assertTrue(expectedServiceDt.get().compareTo(registeredServiceDt.get()) == 0, "Error registred service date time is wrong");
        }
    }
}
