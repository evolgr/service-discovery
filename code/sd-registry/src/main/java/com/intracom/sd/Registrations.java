package com.intracom.sd;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.model.Service;
import com.intracom.model.ServiceRegistry;

import io.reactivex.Completable;

/**
 * Registration data used to store all services for all available functions
 */
public class Registrations
{
    private static final Logger log = LoggerFactory.getLogger(Registrations.class);
    private ConcurrentHashMap<String, List<Service>> functions = new ConcurrentHashMap<>();

    public Registrations()
    {
        // empty constructor
    }

    public ConcurrentHashMap<String, List<Service>> getFunctions()
    {
        return this.functions;
    }

    public void clearFunctions()
    {
        this.functions.clear();
    }

    /**
     * Get list of registred services
     * 
     * @param function The function name to be used for the retrieval of registred
     *                 services
     * @return list of services or null if there are no registrations for specific
     *         function
     */
    public List<Service> getRegistrations(String function)
    {
        List<Service> services = new ArrayList<Service>();
        if (this.functions.containsKey(function))
            services = this.functions.get(function);
        else
            log.warn("No registrations for function {}", function);
        return services;
    }

    public boolean addRegistration(ServiceRegistry serviceRegistry)
    {
        var result = false;

        // get input function name
        String inFunction = serviceRegistry.getFunction();
        log.info("New registration for function {}", inFunction);

        // get input service data
        Service inService = this.getInService(serviceRegistry);
        log.info("New registration for service {}", inService);
        if (inService == null)
            return result;

        // update functions and list of services
        this.updateFunctions(inFunction, inService);

        // confirm changes
        return this.confirm(inFunction, inService);
    }

    private Service getInService(ServiceRegistry serviceRegistry)
    {
        Optional<List<Service>> inServices = serviceRegistry.getServices();
        List<Service> inServicesData = new ArrayList<>();
        Service inServiceData = null;

        // check if multiple services added at once
        if (inServices.isPresent())
        {
            inServicesData = inServices.get();
            if (inServicesData.isEmpty() || inServicesData.size() > 1)
                log.error("Registration contains none {} or more than 1 services {}.", //
                          inServicesData.isEmpty(), //
                          inServicesData.size());
            else
                inServiceData = inServicesData.get(0);
        }
        else
            log.info("Registration does not contain any services");
        return inServiceData;
    }

    private void updateFunctions(String inFunction,
                                 Service inService)
    {
        // check if specific function exists
        if (this.functions.containsKey(inFunction))
        {
            // get existing services for specific function
            List<Service> existingServices = this.functions.get(inFunction);
            log.info("Function {} has {} services registred.", inFunction, existingServices.size());

            // get existing service that matches input service data
            Service existingService = existingServices.stream() // stream existing services
                                                      .filter(service -> service.getName().equals(inService.getName())) // keep only services that match
                                                      .findFirst() // find any service that match
                                                      .orElse(null); // if none services match return null

            // check if input service match
            if (existingService == null)
            {
                log.info("Adding service {} that does not exist in registred services for function {}", inService.getName(), inFunction);
                existingServices.add(inService);
                this.functions.put(inFunction, existingServices);
            }
            else
            {
                // remove old service from list
                existingServices.remove(existingService);
                existingServices.add(inService);
                this.functions.put(inFunction, existingServices);
            }
        }
        else
        {
            log.info("Existing registration does not contain function {}", inFunction);
            List<Service> newServices = new ArrayList<Service>();
            newServices.add(inService);
            this.functions.put(inFunction, newServices);
        }
    }

    private boolean confirm(String inFunction,
                            Service inService)
    {
        boolean result = false;

        // check if function added successfully
        if (!this.functions.containsKey(inFunction))
        {
            log.error("Failed to add function {}", inFunction);
            return result;
        }
        else
        {
            // check if services exist
            List<Service> services = this.functions.get(inFunction);
            if (services.isEmpty())
                log.error("No Services for function {}", inFunction);
            else
            {
                int count = services.stream() // stream services
                                    .filter(service -> service.equals(inService)) // keep only services that match
                                    .collect(Collectors.toList()) // collect match services in list
                                    .size(); // get size of list
                if (count == 0 || count > 1)
                    log.error("Invalid number of services indentified in function {}", inFunction);
                else
                    result = true;
            }
        }
        return result;
    }

    public Completable applyExpiration(List<String> pods)
    {
        return Completable.fromAction(() ->
        {
            var registrations = functions.entrySet();
            if (!registrations.isEmpty())
            {
                log.info("Registrations identified {}", registrations.size());

                registrations.stream() //
                             .forEach(func ->
                             {
                                 var functionName = func.getKey();
                                 var services = func.getValue();
                                 log.info("Current function {} services {}", functionName, services);

                                 var newServiceList = services.stream() //
                                                              .filter(service -> pods.contains(service.getName()))
                                                              .collect(Collectors.toList());
                                 log.info("New function {} services {}", functionName, newServiceList);

                                 functions.put(functionName, newServiceList);
                             });
            }
        });
    }
}
