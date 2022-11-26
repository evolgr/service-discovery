package com.intracom.registry;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.model.Service;
import com.intracom.model.ServiceRegistry;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.util.Config;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * Registration data used to store all services for all available functions
 */
public class Registrations
{
    private static final Logger log = LoggerFactory.getLogger(Registrations.class);
    private ConcurrentHashMap<String, List<Service>> functions = new ConcurrentHashMap<>();
    private RegistryParameters params;
    private CoreV1Api coreV1Api;
    private final RegistrationExpirationHandler expirationHandler;

    public Registrations(RegistryParameters params) throws URISyntaxException, IOException
    {
        this.params = params;

        // kubernetes
        ApiClient apiClient = Config.fromCluster();
        apiClient.setReadTimeout(0); // infinite timeout
        apiClient.setBasePath(this.normalize(apiClient.getBasePath()));

        // set default configuration for the api client
        Configuration.setDefaultApiClient(apiClient);
        this.coreV1Api = new CoreV1Api(apiClient);

        // registration expiration handler
        this.expirationHandler = new RegistrationExpirationHandler(this.params.getExpirationPeriod());
    }

    public Completable run()
    {
        return Completable.complete() //
                          .andThen(this.expirationHandler.start());
    }

    public Completable stop()
    {
        if (this.expirationHandler.disposable != null && !this.expirationHandler.disposable.isDisposed())
            return this.expirationHandler.stop();
        return Completable.complete();
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

        // get input service data
        Service inService = this.getInService(serviceRegistry);
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
            if (inServicesData.isEmpty() || inServicesData.size() > 1)
                log.error("Registration contains none or more than 1 services.");
            else
                inServicesData = inServices.get();
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
                                                      .filter(service -> service.equals(inService)) // keep only services that match
                                                      .findAny() // find any service that match
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
            log.info("Exiting registration does not containe function {}", inFunction);
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

    private List<String> getPods()
    {
        try
        {
            return this.coreV1Api.listNamespacedPod(this.params.getNamespace(), // namespace
                                                    null, // pretty
                                                    false, // allow watch bookmarks
                                                    null, // _continue
                                                    null, // field selector
                                                    null, // label selector
                                                    0, // limit integer value
                                                    null, // resource version
                                                    null, // resource version watch
                                                    10, // timeout seconds
                                                    false) // watch
                                 .getItems()
                                 .stream()
                                 .map(pod -> pod.getMetadata().getName())
                                 .collect(Collectors.toList());
        }
        catch (ApiException e)
        {
            throw new RuntimeException("Fetching of the list of namespaced pods returned error", e);
        }
    }

    private Completable applyExpiration(List<String> pods)
    {
        return Completable.fromAction(() ->
        {
            functions.entrySet() //
                     .stream() //
                     .forEach(func ->
                     {
                         var functionName = func.getKey();
                         var serviceList = func.getValue();

                         var newServiceList = serviceList.stream() //
                                                         .filter(service -> pods.contains(service.getName()))
                                                         .collect(Collectors.toList());
                         functions.put(functionName, newServiceList);
                     });
        });
    }

    private String normalize(String basePath) throws URISyntaxException, UnknownHostException
    {
        var oldUri = new URI(basePath);

        var normalizedAddress = InetAddress.getByName(oldUri.getHost()).getHostAddress();

        var normalizedUri = new URI(oldUri.getScheme(),
                                    oldUri.getUserInfo(),
                                    normalizedAddress,
                                    oldUri.getPort(),
                                    oldUri.getPath(),
                                    oldUri.getQuery(),
                                    oldUri.getFragment());

        log.info("Normalized URI: {}", normalizedUri);

        return normalizedUri.toString();
    }

    private class RegistrationExpirationHandler
    {
        private final Long timeout;
        private final Flowable<Long> timer;
        private Disposable disposable;

        public RegistrationExpirationHandler(Long timeout)
        {
            this.timeout = timeout;
            this.timer = Flowable.interval(timeout - 10L, TimeUnit.SECONDS, Schedulers.io());
        }

        private Completable start()
        {
            return Completable.fromAction(() -> this.disposable = this.timer.doOnNext(timeout -> log.info("Registrations timeout triggered"))
                                                                            .filter(timeout -> !functions.isEmpty())
                                                                            .concatMap(timeout -> Flowable.fromCallable(() ->
                                                                            {
                                                                                return getPods();
                                                                            }))
                                                                            .concatMapCompletable(pods -> applyExpiration(pods))
                                                                            .onErrorComplete(InterruptedException.class::isInstance)
                                                                            .retry(3)
                                                                            .doOnError(e -> log.error("Error occured during timeout of registrations", e))
                                                                            .doOnSubscribe(s -> log.debug("Started registration timeout engine"))
                                                                            .doOnTerminate(() -> log.debug("Stopped registration timeout engine"))
                                                                            .subscribe());
        }

        private Completable stop()
        {
            return Completable.fromAction(() -> this.disposable.dispose());
        }

        private Completable restart()
        {
            return Completable.defer(() ->
            {
                if (this.disposable != null)
                    return this.stop().andThen(this.start());
                else
                    return this.start();
            });
        }
    }
}
