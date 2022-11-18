package com.intracom.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.intracom.common.utilities.Jackson;
import com.intracom.common.web.VertxBuilder;
import com.intracom.common.web.WebClientC;
import com.intracom.common.web.WebServer;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.authentication.AuthenticationProvider;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.AuthenticationHandler;
import io.vertx.reactivex.ext.web.handler.BasicAuthHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class Handler
{
    private static final Logger log = LoggerFactory.getLogger(Handler.class);
    private static final HandlerParameters PARAMS = HandlerParameters.instance;
    private final Vertx vertx = new VertxBuilder().build();
    private final WebServer handlerWebServer;
    private final WebClientC handlerClient;
    private final WebClientC registryClient;
    private final Router router;
    private static final ObjectMapper json = Jackson.om();

    public Handler() throws UnknownHostException
    {
        // empty constructor

        this.handlerClient = WebClientC.builder().withOptions(null).build(this.vertx);
        this.registryClient = WebClientC.builder().withOptions(null).build(this.vertx);

        this.handlerWebServer = WebServer.builder() //
                                         .withHost(InetAddress.getLocalHost().getHostAddress())
                                         .withPort(PARAMS.handlerServerPort)
                                         .build(this.vertx);

        this.router = Router.router(vertx);
        router.route("/model").handler(this::handleAuth);
        router.route("/model").handler(this::checkRequests);

    }

    private void handleAuth(final RoutingContext routingContext)
    {
        final JsonObject credentials = new JsonObject();
        credentials.put("username", PARAMS.getUsername());
        credentials.put("password", PARAMS.getPassword());
        final Authorization simpleAuthProvider = new Authorization(credentials);
        final AuthenticationProvider authProvider = new AuthenticationProvider(simpleAuthProvider);
        final AuthenticationHandler basicAuthHandler = BasicAuthHandler.create(authProvider);
        basicAuthHandler.handle(routingContext);
    }

    private void checkRequests(final RoutingContext routingContext)
    {
        routingContext.request().bodyHandler(buffer ->
        {
            String jsonBody = null;
            try
            {
                jsonBody = json.writeValueAsString(buffer);
                log.debug("Indicate request: {}", jsonBody);
                JsonObject jo = new JsonObject(jsonBody);
                jo.encodePrettily();

                log.debug("Pretty print request: {}", jsonBody);
            }
            catch (JsonProcessingException e)
            {
                e.printStackTrace();
                log.debug("Inavlid json format");
            }

        });
    }

    // Get request to registry
    public Single<String> get()
    {
        return this.handlerClient.get()
                                 .flatMap(webClient -> webClient.get(0, null, null)
                                                                .rxSend()
                                                                .doOnError(throwable -> log.warn("GET request failed"))
                                                                .flatMap(resp -> resp.statusCode() == HttpResponseStatus.OK.code() ? Single.just(resp.bodyAsString())
                                                                                                                                   : Single.error(new RuntimeException("GET request failed. statusCode: "
                                                                                                                                                                       + resp.statusCode()
                                                                                                                                                                       + ", body: "
                                                                                                                                                                       + resp.bodyAsString()))));
    }
    
    public Single<String> post()
    {
        return this.handlerClient.get()
                                 .flatMap(webClient -> webClient.post(0, null, null)
                                                                .rxSend()
                                                                .doOnError(throwable -> log.warn("POST request failed"))
                                                                .flatMap(resp -> resp.statusCode() == HttpResponseStatus.OK.code() ? Single.just(resp.bodyAsString())
                                                                                                                                   : Single.error(new RuntimeException("POST request failed. statusCode: "
                                                                                                                                                                       + resp.statusCode()
                                                                                                                                                                       + ", body: "
                                                                                                                                                                       + resp.bodyAsString()))));
    }

    public static void main(String args[]) throws InterruptedException
    {
        while (true)
        {
            log.info("this is a simple example");
            TimeUnit.SECONDS.sleep(10);
        }
    }
}