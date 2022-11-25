package com.intracom.server;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.intracom.common.utilities.Jackson;
import com.intracom.common.web.WebServer;
import com.intracom.model.Message;
import com.intracom.model.Message.MessageBuilder;
import com.intracom.model.Request;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.functions.Predicate;
import io.vertx.reactivex.ext.web.RoutingContext;

/**
 * 
 */
public class ChatHandler
{
    private static final Logger log = LoggerFactory.getLogger(ChatHandler.class);
    private static final URI CHAT_MESSAGES_URI = URI.create("/chat/messages");
    private static final String DUMMY_USER = "JohnDoe";
    private static final String DUMMY_MSG_0 = "Quisque faucibus lectus id turpis aliquet venenatis.";
    private static final ObjectMapper json = Jackson.om();
    private static final String DUMMY_MESSAGE_1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit.";
    private static final String DUMMY_MESSAGE_2 = "Duis fermentum lacus vitae egestas molestie.";
    private static final String DUMMY_MESSAGE_3 = "Nullam sed tortor id mauris suscipit interdum.";
    private static final String DUMMY_MESSAGE_4 = "Quisque vestibulum ante vel lorem commodo porttitor.";
    private static final String DUMMY_MESSAGE_5 = "Maecenas et neque ac quam dapibus fringilla.";
    private static final String DUMMY_MESSAGE_6 = "Vivamus eget mauris pellentesque, molestie sapien sit amet, auctor erat.";
    private static final String DUMMY_MESSAGE_7 = "Quisque bibendum odio quis libero lacinia, quis luctus justo euismod.";
    private static final String DUMMY_MESSAGE_8 = "Donec non turpis interdum, rhoncus lorem in, aliquam ipsum.";
    private static final String DUMMY_MESSAGE_9 = "Nulla nec odio rhoncus, facilisis ligula quis, vehicula augue.";
    private static final String DUMMY_MESSAGE_10 = "Sed in libero ut nisi dictum facilisis.";
    private static final String DUMMY_MESSAGE_11 = "Donec id dui ut tortor laoreet dapibus in vitae lectus.";
    private static final String DUMMY_MESSAGE_12 = "Donec quis tortor sed eros vulputate sodales.";
    private static final List<String> DUMMY_MESSAGES = List.of(DUMMY_MESSAGE_1, //
                                                               DUMMY_MESSAGE_2, //
                                                               DUMMY_MESSAGE_3, //
                                                               DUMMY_MESSAGE_4, //
                                                               DUMMY_MESSAGE_5, //
                                                               DUMMY_MESSAGE_6, //
                                                               DUMMY_MESSAGE_7, //
                                                               DUMMY_MESSAGE_8, //
                                                               DUMMY_MESSAGE_9, //
                                                               DUMMY_MESSAGE_10, //
                                                               DUMMY_MESSAGE_11, //
                                                               DUMMY_MESSAGE_12);

    private final WebServer server;
    private final Message dummyMessage;

    public ChatHandler(ServerParameters params) throws UnknownHostException
    {
        this.server = WebServer.builder() // create new webserver
                               .withHost(InetAddress.getLocalHost().getHostAddress()) // set current ip address
                               .withPort(params.getServerPort()) // use port from input parameters
                               .build(params.getVertx()); // build new webserver using common Vert.x Core API entry point

        this.dummyMessage = new MessageBuilder().withId(0L) //
                                                .withMessage(DUMMY_MSG_0) //
                                                .withOwner(DUMMY_USER) //
                                                .withRecipient(false) //
                                                .withUser(DUMMY_USER) //
                                                .build();

        // configure web server routers
        this.server.configureRouter(router -> router.get(CHAT_MESSAGES_URI.getPath()) //
                                                    .handler(this::fetchMessage));
    }

    public Completable start()
    {
        return Completable.complete() //
                          .andThen(this.server.startListener()) //
                          .onErrorResumeNext(t -> this.stop().andThen(Completable.error(t)));
    }

    public Completable stop()
    {
        final Predicate<? super Throwable> logError = t ->
        {
            log.warn("Ignored Exception during shutdown", t);
            return true;
        };

        return Completable.complete() //
                          .andThen(this.server.stopListener().onErrorComplete(logError));
    }

    public void fetchMessage(RoutingContext routingContext)
    {
        routingContext.request().bodyHandler(buffer ->
        {
            log.info("Handle fetch message request");
            try
            {
                Request request = json.readValue(buffer.toJsonObject().toString(), Request.class);
                log.info("Request data: {}", request);

                Message reply = new MessageBuilder(this.dummyMessage).withId(this.getRandomId())
                                                                     .withMessage(this.getRandomMessage())
                                                                     .withOwner(request.getUser())
                                                                     .withOwner(request.getUser())
                                                                     .withRecipient(false)
                                                                     .build();

                routingContext.response() //
                              .setStatusCode(HttpResponseStatus.ACCEPTED.code())
                              .end(json.registerModule(new JodaModule()) //
                                       .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS) //
                                       .writeValueAsString(reply));
            }
            catch (JsonProcessingException e)
            {
                log.error("Fetch message request data with invalid format");
                routingContext.response() // create response object
                              .setStatusCode(HttpResponseStatus.BAD_REQUEST.code()) // set response code 400
                              .end(); // complete with response action
            }
        });
    }

    public List<String> getDummyMessages()
    {
        return DUMMY_MESSAGES;
    }

    private String getRandomMessage()
    {
        return DUMMY_MESSAGES.get(new Random().nextInt(DUMMY_MESSAGES.size()));
    }

    private Long getRandomId()
    {
        return Long.parseLong(String.valueOf(new Random().ints().findFirst().getAsInt()));
    }

}
