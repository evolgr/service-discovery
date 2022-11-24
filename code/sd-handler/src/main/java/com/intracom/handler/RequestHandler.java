package com.intracom.handler;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intracom.common.utilities.Jackson;
import com.intracom.common.web.WebClient;
import com.intracom.common.web.WebServer;
import com.intracom.model.Service;
import com.intracom.model.ServiceDiscovery;
import com.intracom.model.ServiceRegistry;
import com.intracom.model.ServiceRegistry.ServiceRegistryBuilder;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.client.HttpResponse;

/**
 * 
 */
public class RequestHandler
{
    private final WebServer server;
    private final WebClient client;
    private final HandlerParameters params;

    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);
    private static final ObjectMapper json = Jackson.om();
    private static final URI CHAT_MESSAGES_URI = URI.create("/chat/messages");
    private static final String REGISTRY_HOST = "sd-registry";
    private static final int REGISTRY_PORT = 8080;
    private static final URI REGISTRY_URI = URI.create("/registrations");

    public RequestHandler(WebServer server,
                          WebClient client,
                          HandlerParameters params)
    {
        this.server = server;
        this.params = params;
        this.client = client;
    }

    public void createRouters()
    {
        this.server.configureRouter(router -> router.route(CHAT_MESSAGES_URI.getPath()).handler(this::requestHandler));
//        router.route(DEFAULT_URI.getPath()).handler(this::authenticationHandler)
    }

//  private void authenticationHandler(final RoutingContext rc)
//  {
//      final JsonObject credentials = new JsonObject();
//      credentials.put("username", this.params.getUsername());
//      credentials.put("password", this.params.getPassword());
//      BasicAuthHandler.create(new AuthenticationProvider(new UserAuthentication(credentials))).handle(rc);
//  }

    public void requestHandler(RoutingContext routingContext)
    {
        log.info("Request received by Handler service");
        routingContext.request().bodyHandler(buffer ->
        {
            log.info("Request body: {}", buffer.toString());
            try
            {
                ServiceDiscovery serviceDiscovery = json.readValue(buffer.toJsonObject().toString(), ServiceDiscovery.class);
                log.info("Received service discovery request: {}", serviceDiscovery);
                var function = serviceDiscovery.getFunction();
                if (function.isBlank())
                {
                    log.error("Failed to indentify function information");
                    routingContext.response() // create response object
                                  .setStatusCode(HttpResponseStatus.BAD_REQUEST.code()) // set response code 400
                                  .end(); // complete with response action
                }
                else
                {
                    log.info("Successfully identified function {}", function);
                    this.checkRegistrations(routingContext, serviceDiscovery) //
                        .subscribe(cr ->
                        {
                            log.info("Registry response with code:{}, result message:{} and body:{}", //
                                     cr.statusCode(), //
                                     cr.statusMessage(), //
                                     cr.bodyAsJsonObject());
                            if (cr.statusCode() == HttpResponseStatus.NO_CONTENT.code())
                            {
                                log.error("No registrered services found.");
                                routingContext.response() // create response object
                                              .setStatusCode(HttpResponseStatus.NOT_FOUND.code()) // set response code 400
                                              .end("Failed to identify service with requested function name");
                            }
                            else if (cr.statusCode() == HttpResponseStatus.FOUND.code())
                            {
                                var serviceRegistry = json.readValue(cr.bodyAsString(), ServiceRegistry.class);
                                log.error("Service registry response {}", serviceRegistry);

                                // find best service
                                var selectedService = this.findService(serviceRegistry.getServices());
                                log.error("Selected service: {}", selectedService);

                                if (selectedService == null)
                                {
                                    routingContext.response() // create response object
                                                  .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) // set response code 400
                                                  .end("Failed to identify service with requested function name");
                                }
                                else
                                {
                                    // forward request to identified service
                                    this.forwardRequest(routingContext, selectedService, serviceDiscovery) //
                                        .subscribe(fwdResp ->
                                        {
                                            log.info("Backend service response with code:{}, result message:{} and body:{}", //
                                                     fwdResp.statusCode(), //
                                                     fwdResp.statusMessage(), //
                                                     fwdResp.bodyAsJsonObject());
                                            routingContext.response() // create response object
                                                          .setStatusCode(fwdResp.statusCode()) // set response code from backend service
                                                          .setStatusMessage(fwdResp.statusMessage()) // set response code message
                                                          .end(fwdResp.bodyAsString());
                                        }, // complete with response action
                                                   t -> log.error("Error during the forward of request to backend service.", t));
                                }
                            }
                            else
                            {
                                log.error("Registry checks failed due to wrong request");
                                routingContext.response() // create response object
                                              .setStatusCode(HttpResponseStatus.BAD_REQUEST.code()) // set response code 400
                                              .end(); // complete with response action
                            }
                        }, t -> log.error("Error during checking for registered services.", t));
                }
            }
            catch (JsonMappingException e)
            {
                log.error("Request data contains invalid format");
                routingContext.response() // create response object
                              .setStatusCode(HttpResponseStatus.BAD_REQUEST.code()) // set response code 400
                              .end(); // complete with response action
            }
            catch (Exception e)
            {
                log.info("Request handling failed due to exception", e);
                routingContext.response() // create response object
                              .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code()) // set response code 501
                              .end(); // complete with response action
            }
        });
    }

    private Single<HttpResponse<Buffer>> checkRegistrations(RoutingContext routingContext,
                                                            ServiceDiscovery serviceDiscovery)
    {
        var data = new ServiceRegistryBuilder().withFunction(serviceDiscovery.getFunction()) //
                                               .withServices(new ArrayList<String>()) //
                                               .build();

        log.info("host {}, port {}, uri {}", //
                 this.params.getRegistryHost(),
                 this.params.getRegistryPort(),
                 REGISTRY_URI.getPath());

        return this.client.get() // get client
                          .flatMap(webClient -> webClient.get(this.params.getRegistryPort(), //
                                                              this.params.getRegistryHost(), //
                                                              REGISTRY_URI.getPath()) //
                                                         .rxSendJsonObject(new JsonObject(json.writeValueAsString(data)))
                                                         .doOnSubscribe(d -> log.info("Check registry for {} function services",
                                                                                      serviceDiscovery.getFunction()))
                                                         .doOnSuccess(resp -> log.info("Registry response with code:{}, result message {} and body:{}",
                                                                                       resp.statusCode(),
                                                                                       resp.statusMessage(),
                                                                                       resp.bodyAsString()))
                                                         .doOnError(ar -> log.error("Something went wrong during checking registry: {}", ar.getMessage())));
    }

    private Service findService(Optional<List<Service>> list) throws JsonMappingException, JsonProcessingException
    {
        Service service = null;
        if (list.isPresent())
        {
            var services = list.get();
            service = services.get(new Random().nextInt(services.size()));
        }
        return service;
    }

    // Send request to Server side - return Message
    private Single<HttpResponse<Buffer>> forwardRequest(RoutingContext routingContext,
                                                        Service service,
                                                        ServiceDiscovery serviceDiscovery) throws JsonProcessingException
    {
        var servicePort = service.getPort().intValue();
        var serviceHost = service.getHost();
        var serviceUri = CHAT_MESSAGES_URI.getPath();
        log.info("host {}, port {}, uri {}", //
                 serviceHost,
                 servicePort,
                 serviceUri);

        var requests = serviceDiscovery.getRequests();
        if (requests.isEmpty())
        {
            return this.client.get()
                              .flatMap(webClient -> webClient.get(servicePort, serviceHost, serviceUri)
                                                             .rxSend()
                                                             .doOnSubscribe(d -> log.info("Forwarding empty request to {} function services",
                                                                                          serviceDiscovery.getFunction()))
                                                             .doOnSuccess(resp -> log.info("Forward response with code:{}, result message {} and body:{}",
                                                                                           resp.statusCode(),
                                                                                           resp.statusMessage(),
                                                                                           resp.bodyAsString()))
                                                             .doOnError(ar -> log.error("Something went wrong during checking registry: {}", ar.getMessage())));
        }
        else
        {
            var request = requests.get().get(0);
            log.info("Request to forward {}", request);
            var data = new JsonObject(json.writeValueAsString(request));
            log.info("Forwarding to backend service {}", data);

            return this.client.get()
                              .flatMap(webClient -> webClient.get(servicePort, serviceHost, serviceUri)
                                                             .rxSendJsonObject(data)
                                                             .doOnSubscribe(d -> log.info("Forwarding request to {} function services",
                                                                                          serviceDiscovery.getFunction()))
                                                             .doOnSuccess(resp -> log.info("Forward response with code:{}, result message {} and body:{}",
                                                                                           resp.statusCode(),
                                                                                           resp.statusMessage(),
                                                                                           resp.bodyAsString()))
                                                             .doOnError(ar -> log.error("Something went wrong during checking registry: {}", ar.getMessage())));
        }
    }
}
