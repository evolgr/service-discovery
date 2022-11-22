package com.intracom.handler;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intracom.common.utilities.Jackson;
import com.intracom.common.web.VertxBuilder;
import com.intracom.common.web.WebClientC;
import com.intracom.common.web.WebServer;
import com.intracom.model.Entry;
import com.intracom.model.Service;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.BehaviorSubject;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;

public class Handler
{
    private static final Logger log = LoggerFactory.getLogger(Handler.class);
    private static final HandlerParameters PARAMS = HandlerParameters.instance;
    private final Vertx vertx = new VertxBuilder().build();
    private final WebServer server;
    private final WebClientC client;
    private static final ObjectMapper json = Jackson.om();
    private JsonObject jo;
    private Disposable updater = null;
    private URI baseUri = null;
    private final BehaviorSubject<String> subject = BehaviorSubject.create();
    private static final URI REGISTRY_URI = URI.create("/registrations");
    private static final URI CHAT_MESSAGES_URI = URI.create("/chat/messages");

    public Handler() throws UnknownHostException
    {
        // empty constructor
        this.client = WebClientC.builder().build(this.vertx);

        this.server = WebServer.builder() //
                               .withHost(InetAddress.getLocalHost().getHostAddress())
                               .withPort(PARAMS.handlerServerPort)
                               .build(this.vertx);

    }

//    private void handleAuth(final RoutingContext routingContext)
//    {
//        final JsonObject credentials = new JsonObject();
//        credentials.put("username", PARAMS.getUsername());
//        credentials.put("password", PARAMS.getPassword());
//        final Authorization simpleAuthProvider = new Authorization(credentials);
//        final AuthenticationProvider authProvider = new AuthenticationProvider(simpleAuthProvider);
//        final AuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);
//        basicAuthHandler.handle(routingContext);
//    }

    private void checkRequests(final RoutingContext routingContext)
    {
        routingContext.request().bodyHandler(buffer ->
        {
            String jsonBody = null;
            try
            {
                jsonBody = json.writeValueAsString(buffer);
                log.debug("Indicate request: {}", jsonBody);
                jo = new JsonObject(jsonBody);
                jo.encodePrettily();

                log.debug("Pretty print request: {}", jsonBody);

                // step 1 - extract function and data
                // object mapper, failures?
                // step 2 - check with registry and get services
                // getRegistry
                // step 3 - select single service
                // check pod cpu usage (if pod exists)
                // or random select service
                // step 4 - forward request to service
                // return Message object
            }
            catch (JsonProcessingException e)
            {
                e.printStackTrace();
                log.debug("Inavlid json format");
            }

        });
    }

    // Get request to registry -> return Set of Services
    public Single<String> getRegistry(String functionName)
    {
        return this.client.get()
                          .flatMap(webClient -> webClient.get(PARAMS.getRegistryCPort(), PARAMS.getRegistryCHost(), REGISTRY_URI.getPath())
                                                         .rxSendJsonObject(new JsonObject().put("function", functionName))
                                                         .doOnError(throwable -> log.warn("GET request failed"))
                                                         .flatMap(resp ->
                                                         {
                                                             if (resp.statusCode() == HttpResponseStatus.OK.code())
                                                             {
                                                                 // object mapper and return Set of Services
                                                                 return Single.just(resp.bodyAsString());
                                                             }
                                                             else
                                                             {
                                                                 // return empty Set of Services
                                                                 return Single.error(new RuntimeException("GET request failed. statusCode: " + resp.statusCode()
                                                                                                          + ", body: " + resp.bodyAsString()));
                                                             }
                                                         }));
    }

    // Send request to Server side - return Message
    public Single<String> forwardRequest(Service service,
                                         Set<Entry> data)
    {
        var array = new JsonArray();
        for (Entry datum : data)
        {
            array.add(new JsonObject().put("host", datum.getHost())
                                      .put("port", datum.getPort())
                                      .put("user", datum.getUser())
                                      .put("message", datum.getMessage())
                                      .put("timestamp", datum.getTimestamp()));
        }

        return this.client.get()
                          .flatMap(webClient -> webClient.get(Integer.valueOf(service.getPort().toString()), service.getHost(), CHAT_MESSAGES_URI.getPath())
                                                         .rxSendJsonObject(new JsonObject().put("data", array))
                                                         .doOnError(throwable -> log.warn("GET request failed"))
                                                         .flatMap(resp ->
                                                         {
                                                             if (resp.statusCode() == HttpResponseStatus.OK.code())
                                                             {
                                                                 // object mapper and return Message
                                                                 return Single.just(resp.bodyAsString());
                                                             }
                                                             else
                                                             {
                                                                 // return empty Message
                                                                 return Single.error(new RuntimeException("GET request failed. statusCode: " + resp.statusCode()
                                                                                                          + ", body: " + resp.bodyAsString()));
                                                             }
                                                         }));
    }

    public Completable start(WebServer routerHandler)
    {
        return Completable.fromAction(() ->
        {
            final String Url = "url";

            if (this.updater == null)
            {

//                this.updater = Handler.this.getRegistry()//
//                                           .doOnSuccess(req ->
//                                           {
//                                               log.info("Requests fetched for the first time: {}", req);
                                               this.baseUri = routerHandler.baseUri().resolve(Url);
                                               log.info("Registering URL for receiving requests: {}", this.baseUri);
//                                                 routerHandler.configureRouter(router -> router.route("/model").handler(this::handleAuth));
                                               routerHandler.configureRouter(router -> router.route("/model").handler(this::checkRequests));
//                                               this.subject.onNext(req);
//                                           })
//                                           .retryWhen(errors -> errors.flatMap(e ->
//                                           {
//                                               log.warn("Failed to fetch requests for the first time, retrying.", e);
//                                               return Flowable.timer(10, TimeUnit.SECONDS);
//                                           }))
//                                           .ignoreElement()
//                                           .subscribe();

            }
        });
    }

    public void run()
    {
        log.info("Running...");

        try
        {
            Completable.complete()//
                       .andThen(this.server.startListener())
                       .andThen(this.start(this.server))
                       .andThen(Completable.create(emitter ->
                       {
                           log.info("Registering shutdown hook.");
                           Runtime.getRuntime().addShutdownHook(new Thread(() ->
                           {
                               log.info("Shutdown hook called.");
                               this.server.stopListener().blockingAwait();
                               emitter.onComplete();
                           }));
                       }))
                       .blockingAwait();
        }
        catch (Exception e)
        {
            log.error("Exception caught, stopping monitor.", e);
        }

        log.info("Stopped.");
    }

    public static void main(String args[]) throws InterruptedException, UnknownHostException
    {
        int exitStatus = 0;

        log.info("Starting Handler");

        Handler app = new Handler();
        app.run();

        log.info("Stopped Handler.");

        System.exit(exitStatus);
    }
}