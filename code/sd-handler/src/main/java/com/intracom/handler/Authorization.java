package com.intracom.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;

public class Authorization
{
    private static final Logger log = LoggerFactory.getLogger(Authorization.class);
    private final JsonObject userCredentials;

    protected Authorization(JsonObject userCredentials)
    {
        this.userCredentials = userCredentials;
        log.info("Monitor username: {}", userCredentials.getString("username"));
        log.info("Monitor password: {}", userCredentials.getString("password"));
    }

    public void authenticate(Handler<AsyncResult<User>> resultHandler)
    {
        authenticate(this.userCredentials, resultHandler);
    }

    public void authenticate(JsonObject authInfo,
                             Handler<AsyncResult<User>> resultHandler)
    {
        log.info("Checking username: {}", authInfo.getString("username"));
        log.info("Checking password: {}", authInfo.getString("password"));

        if (authInfo.getString("username").equals(this.userCredentials.getValue("username"))
            && authInfo.getString("password").equals(this.userCredentials.getValue("password")))
        {
            log.info("Username {} authenticated successfully.", authInfo.getString("username"));
            resultHandler.handle(Future.succeededFuture(User.create(authInfo)));
        }
        else
        {
            log.debug("Username {} not authorized to access webserver.", authInfo.getString("username"));
            log.error("User authentication failed.");
            resultHandler.handle(Future.failedFuture("Unauthorized(401) User"));
        }

        log.info("Authentication procedure complete.");
    }
}
