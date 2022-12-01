package com.intracom.sd;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
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
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * 
 */
public class RegistrationExpirationHandler
{
    private static final Logger log = LoggerFactory.getLogger(RegistrationExpirationHandler.class);
//    private final Long timeout;
//    private final Flowable<Long> timer;
    private Disposable disposable;
    private CoreV1Api coreV1Api;
    private RegistryParameters params;
    private Registrations registrations;

    public RegistrationExpirationHandler(RegistryParameters params,
                                         Registrations registrations) throws URISyntaxException, IOException, ApiException
    {
        this.params = params;
        this.registrations = registrations;

//        this.timeout = params.getCheckPeriod();
//        this.timer = Flowable.interval(this.timeout - 10L, TimeUnit.SECONDS, Schedulers.io());

        // kubernetes
        ApiClient apiClient = Config.fromCluster();
        apiClient.setReadTimeout(0); // infinite timeout
        apiClient.setBasePath(this.normalize(apiClient.getBasePath()));

        // set default configuration for the api client
        Configuration.setDefaultApiClient(apiClient);
        this.coreV1Api = new CoreV1Api(apiClient);

//        var pods = this.coreV1Api.listNamespacedPod(this.params.getNamespace(), null, false, null, null, null, null, null, null, null, false)
//                                 .getItems()
//                                 .stream()
//                                 .map(pod -> pod.getMetadata().getName())
//                                 .collect(Collectors.toList());
        var pods = this.getPods();
        log.info("Initial pods identified in namespace {} -> {}", pods.size(), pods);

        this.disposable = null;
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
        List<String> pods = new ArrayList<>();
        try
        {
            pods = this.coreV1Api.listNamespacedPod(this.params.getNamespace(), // namespace
                                                    null, // pretty
                                                    false, // allow watch bookmarks
                                                    null, // _continue
                                                    null, // field selector
                                                    null, // label selector
                                                    null, // limit integer value
                                                    null, // resource version
                                                    null, // resource version watch
                                                    null, // timeout seconds
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

        log.info("{} pods identified in namespace: {}", pods.size(), pods);
        return pods;
    }

    public Completable start()
    {
        return Completable.fromAction(() ->
        {
            Single.fromCallable(this::getPods)
                  .subscribeOn(Schedulers.io())
                  .retryWhen(handler -> handler.delay(this.params.getCheckPeriod(), TimeUnit.SECONDS))
                  .doOnSubscribe(d -> log.info("Start monitoring namespaced pods"))
                  .flatMapCompletable(pods -> this.registrations.applyExpiration(pods))
                  .doOnSubscribe(s -> log.debug("Starting check of registred services"))
                  .doOnError(e -> log.error("Stopped check of registred services due to unexpected error", e))
                  .subscribe(() -> log.info("Started check of registred services"), t -> log.error("Error starting registration timeout engine", t));

//                                        .flatMap(pods -> {
//                                            var result = false;
//                                            if (pods.size() > 0)
//                                            {
//                                                this.registrations.applyExpiration(pods);
//                                                result = true;
//                                            }
//                                            return result;
//                                        }).;

//                                        .flatMapCompletable(pods -> this.registrations.applyExpiration(pods))
//                                        
//                                        
//                                        .doOnSuccess(r -> log.info("Success"))
//                                        
//                                        .repeatWhen(handler -> handler.delay(this.params.getCheckPeriod(), TimeUnit.SECONDS))
//                                        .concatMapCompletable(pods -> this.registrations.applyExpiration(pods))
//                                        .doOnSubscribe(s -> log.debug("Starting check of registred services"))
//                                        .doOnError(e -> log.error("Stopped check of registred services due to unexpected error", e))
//                                        .subscribeOn(Schedulers.io())
//                                        .retry(3)
//                                        .subscribe(() -> log.info("Started check of registred services"),
//                                                   t -> log.error("Error starting registration timeout engine", t));
//            }
        });
//        this.registrations.getFunctions() //
//        .entrySet() //
//        .stream() //
//        .map(Entry::getValue) //
//        .flatMap(services -> services.g)

//        return Completable.fromAction(() ->
//        {
//            if (this.disposable == null || this.disposable.isDisposed())
//            {
//                this.disposable = this.timer.doOnNext(t -> log.info("Registrations check triggered")) //
//                                            .filter(t ->
//                                            {
//                                                var functions = this.registrations.getFunctions();
//                                                log.info("Functions and registered services {}", functions);
//                                                return !functions.isEmpty();
//                                            })
//                                            .concatMapCompletable(t -> this.registrations.applyExpiration(this.getPods()))
//                                            .doOnSubscribe(s -> log.debug("Starting check of registred services"))
//                                            .doOnTerminate(() -> log.info("Terminating check of registred services"))
//                                            .doOnError(e -> log.error("Stopped check of registred services due to unexpected error", e))
//                                            .subscribe(() -> log.info("Started check of registred services"),
//                                                       t -> log.error("Error starting registration timeout engine", t));
//            }
//        });
    }

    public Completable stop()
    {
        return Completable.fromAction(() -> this.disposable.dispose());
    }

//    public Completable restart()
//    {
//        return Completable.defer(() ->
//        {
//            if (this.disposable != null)
//                return this.stop().andThen(this.start());
//            else
//                return this.start();
//        });
//    }
}
