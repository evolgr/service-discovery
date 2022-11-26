package com.intracom.registry;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.intracom.model.Service;
import com.intracom.model.Service.ServiceBuilder;
import com.intracom.model.ServiceRegistry;
import com.intracom.model.ServiceRegistry.ServiceRegistryBuilder;

public class RegistrationsTest
{
    private static final Logger log = LoggerFactory.getLogger(RegistrationsTest.class);
    private Registrations registrations;

    @BeforeClass
    public void beforeClass() throws URISyntaxException, IOException
    {
        this.registrations = new Registrations();
    }

    @AfterMethod
    public void afterMethod()
    {
        var functions = this.registrations.getFunctions();
        if (!functions.isEmpty())
            this.registrations.clearFunctions();
    }

    @Test
    public void addRegistrationTest()
    {
        var functions = this.registrations.getFunctions();
        assertTrue(functions.isEmpty(), "Functions unexpectedly not empty");

        var serviceRegistry = this.getServiceRegistry(null);
        assertTrue(this.registrations.addRegistration(serviceRegistry), "Failed to add registration");

        functions = this.registrations.getFunctions();
        assertFalse(functions.isEmpty(), "Functions unexpectedly empty");

        List<Service> extractedServices = this.registrations.getRegistrations("test-function");
        assertTrue(serviceRegistry.getServices() // get optional services
                                  .get() // get services
                                  .equals(extractedServices), // check with extracted services
                   "Added services are not the same with services exist in functions");
    }

    @Test
    public void getRegistrationsTest()
    {
        var functions = this.registrations.getFunctions();
        assertTrue(functions.isEmpty(), "Functions unexpectedly not empty");

        var serviceRegistry = this.getServiceRegistry(null);
        this.registrations.addRegistration(serviceRegistry);

        functions = this.registrations.getFunctions();
        assertFalse(functions.isEmpty(), "Functions unexpectedly empty");

        // simulate change of service
        var newService = new ServiceBuilder().withHost("test-host") //
                                             .withName("test-pod-name-2") //
                                             .withPort(Double.valueOf("666"))
                                             .withTimestamp(new DateTime())
                                             .build();
        var newServiceRegistry = this.getServiceRegistry(newService);
        this.registrations.addRegistration(newServiceRegistry);

        // without timeout, expect to have both services
        List<Service> extractedServices = this.registrations.getRegistrations("test-function");
        assertTrue(extractedServices.size() == 2, "Wrong number of services in registered test-function");
        assertFalse(serviceRegistry.getServices() // get optional services
                                   .get() // get services
                                   .equals(extractedServices), // check with extracted services
                    "Added services are not the same with services exist in functions");

        assertFalse(serviceRegistry.getServices() // get optional services
                                   .get() // get services
                                   .equals(extractedServices), // check with extracted services
                    "Added services are not the same with services exist in functions");

    }

    private ServiceRegistry getServiceRegistry(Service service)
    {
        if (service == null)
        {
            service = new ServiceBuilder().withHost("test-host") //
                                          .withName("test-pod-name") //
                                          .withPort(Double.valueOf("666"))
                                          .withTimestamp(new DateTime())
                                          .build();
        }
        List<Service> services = new ArrayList<>();
        services.add(service);
        ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().withFunction("test-function") //
                                                                      .withServices(services)
                                                                      .build();
        log.info("Dummy service regisrty: {}", serviceRegistry);
        return serviceRegistry;
    }
}
