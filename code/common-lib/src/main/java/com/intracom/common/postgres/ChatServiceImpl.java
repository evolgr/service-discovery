package com.intracom.common.postgres;

import java.util.stream.Collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intracom.model.Message.MessageBuilder;

import io.reactiverse.pgclient.Row;
import io.reactiverse.reactivex.pgclient.PgPool;
import io.reactiverse.reactivex.pgclient.PgRowSet;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

/**
 * 
 */
public class ChatServiceImpl implements ChatService
{
    private static Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final String SQL_GET_ALL_MESSAGES = "SELECT * FROM chat";
    private final PgPool pgPool;

    public final static Collector<Row, ?, JsonArray> MESSAGES_COLLECTOR = MessageCollector.jsonArrayCollector(row -> new MessageBuilder().withMessageId(row.getLong(0)) //
                                                                                                                                         .withMessageUser(row.getString(1)) //
                                                                                                                                         .withMessageMessage(row.getString(2)) //
                                                                                                                                         .withMessageRecipient(row.getBoolean(3)) //
                                                                                                                                         .withMessageOwner(row.getString(4)) //
                                                                                                                                         .build());
    public ChatServiceImpl(io.reactiverse.pgclient.PgPool pool,
                           Handler<AsyncResult<ChatService>> resultHandler)
    {
        this.pgPool = new PgPool(pool);
        pgPool.rxGetConnection() //
              .flatMap(connection -> connection.rxQuery(SQL_GET_ALL_MESSAGES) //
//                                               .doAfterSuccess(result -> log.info("Connection to database confirmed, currently {} rows", //
//                                                                                  result.rowCount()))
                                               .doAfterTerminate(connection::close))
//                                               .subscribeOn(Schedulers.io()))
//              .subscribeOn(Schedulers.newThread())
//              .doOnSubscribe(s -> log.info("Subscription started for initial database connection"))
//              .doOnError(s -> log.error("Error occured, ", s))
              .subscribe(result -> resultHandler.handle(Future.succeededFuture(this)), t ->
              {
                  log.error("Failed to start new database connection", t);
                  resultHandler.handle(Future.failedFuture(t));
              });
    }

    @Override
    public ChatService getMessages(Handler<AsyncResult<PgRowSet>> resultHandler)
    {
        this.pgPool.rxGetConnection()
                   .flatMap(connection -> connection.rxQuery(SQL_GET_ALL_MESSAGES) //
                                                    .doAfterTerminate(connection::close))
//                   .doOnSubscribe(s -> log.info("Subscription started for initial database connection"))
//                   .doOnError(s -> log.error("Error occured, ", s))
                   .subscribe(result ->
                   {
                       var messages = result.value();
                       log.info("messages: {}", messages);
                       resultHandler.handle(Future.succeededFuture(messages));
                   }, t ->
                   {
                       log.error("Failed to start new database connection", t);
                       resultHandler.handle(Future.failedFuture(t));
                   });
        
        //.rxQuery(SQL_GET_ALL_MESSAGES, MESSAGES_COLLECTOR)
        
//        , asyncHandler -> {
//            if (asyncHandler.succeeded())
//            {
//                var messages = asyncHandler.result().value();
//                log.debug("Messages {}", messages.encodePrettily());
//                resultHandler.handle(Future.succeededFuture(messages));
//            }
//            else
//            {
//                log.error("Failed to get all messages", asyncHandler.cause());
//                resultHandler.handle(Future.failedFuture(asyncHandler.cause()));
//            }
//        });
        return this;
    }
}
