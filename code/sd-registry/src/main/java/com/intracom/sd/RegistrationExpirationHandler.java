package com.intracom.sd;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * 
 */
public class RegistrationExpirationHandler
{
    private static final Logger log = LoggerFactory.getLogger(RegistrationExpirationHandler.class);
    private final Long timeout;
    private final Flowable<Long> timer;
    private Disposable disposable;
    private CoreV1Api coreV1Api;
    private RegistryParameters params;
    private Registrations registrations;

    public RegistrationExpirationHandler(RegistryParameters params,
                                         Registrations registrations) throws URISyntaxException, IOException
    {
        this.params = params;
        this.registrations = registrations;

        this.timeout = params.getExpirationPeriod();
        this.timer = Flowable.interval(this.timeout - 10L, TimeUnit.SECONDS, Schedulers.io());

        // kubernetes
        ApiClient apiClient = Config.fromCluster();
        apiClient.setReadTimeout(0); // infinite timeout
        apiClient.setBasePath(this.normalize(apiClient.getBasePath()));

        // set default configuration for the api client
        Configuration.setDefaultApiClient(apiClient);
        this.coreV1Api = new CoreV1Api(apiClient);
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

    public Disposable getDisposable()
    {
        return this.disposable;
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

    public Completable start()
    {
        log.info("Starting registration expiration handler");
        return Completable.fromAction(() -> this.disposable = this.timer.doOnNext(t -> log.info("Registrations timeout triggered")) //
                                                                        .filter(t ->
                                                                        {
                                                                            var functions = this.registrations.getFunctions();
                                                                            log.info("Functions and registered services {}", functions);
                                                                            return !functions.isEmpty();
                                                                        })
                                                                        .concatMapCompletable(t -> this.registrations.applyExpiration(this.getPods()))
                                                                        .doOnSubscribe(s -> log.debug("Starting removal of absent services from registrations"))
                                                                        .doOnTerminate(() -> log.info("Terminating removal of absent services from registrations"))
                                                                        .subscribe(() -> log.info("Started  registration timeout engine"),
                                                                                   t -> log.error("Error starting registration timeout engine", t)));
    }

    public Completable stop()
    {
        return Completable.fromAction(() -> this.disposable.dispose());
    }

    public Completable restart()
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
