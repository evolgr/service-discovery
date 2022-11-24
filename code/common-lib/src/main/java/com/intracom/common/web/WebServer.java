package com.intracom.common.web;

import java.net.URI;
import java.net.URL;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Completable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpConnection;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.LoggerHandler;

public class WebServer
{
    private static final Logger log = LoggerFactory.getLogger(WebServer.class);
    
    private final Router router;
    private final HttpServer httpServer;
    private final HttpServerOptions options;
    private final boolean listenAll;
    private final Vertx vertx;
    private final URI baseUri;
    
    private AtomicBoolean terminating = new AtomicBoolean(false);
    
    private final Set<HttpConnection> connections = new HashSet<>();
    
    private final Subject<Set<HttpConnection>> connectionSubject = BehaviorSubject.createDefault(Set.<HttpConnection>of()).toSerialized();
    
    WebServer(Vertx vertx,
              WebServerBuilder builder)
    {
        this(vertx, builder, null);
    }
    
    public WebServer(Vertx vertx,
                     WebServerBuilder builder,
                     Router router)
    {
        log.info("Creating new server: {}", this);
        Objects.requireNonNull(vertx);
        Objects.requireNonNull(builder);

        this.vertx = vertx;
        this.options = new HttpServerOptions(builder.options);
        this.listenAll = builder.listenAll;
        this.baseUri = createBaseUri(options);
        this.router = router != null ? router : Router.router(vertx);
        this.httpServer = vertx.createHttpServer(this.options);

        this.httpServer.connectionHandler(connection ->
        {
            if (!this.terminating.get())
            {
                addConnection(connection);
                connection.closeHandler(event -> removeConnection(connection));
            }
            else
            {
                log.warn("Server is terminating, closing down newly established connection {}", connection.remoteAddress());

                drainConnection(5 * 1000L, connection);
            }
        });

        // Enable HTTP tracing, if configured in builder
        if (builder.httpTracing)

        {
            final var loggerHandler = LoggerHandler.create(LoggerFormat.SHORT);
            this.configureRouter(r -> r.route().handler(loggerHandler));
        }
	}
    
    private static URI createBaseUri(HttpServerOptions options)
    {
        try
        {
            return new URL("http", options.getHost(), options.getPort(), "").toURI();
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException(e);
        }
    }
    
    private void addConnection(HttpConnection conn)
    {

        synchronized (this)
        {
            final var added = this.connections.add(conn);
            if (!added)
            {
                log.error("Connection already exists {}", conn);
            }
            this.connectionSubject.onNext(Set.copyOf(this.connections));
        }
    }
    
    private void removeConnection(HttpConnection conn)
    {
        synchronized (this)
        {
            final var removed = this.connections.remove(conn);
            if (!removed)
            {
                log.error("Cannot remove non existent connection {}", conn);
            }
            this.connectionSubject.onNext(Set.copyOf(this.connections));
        }
    }
    
    private void drainConnection(long timeoutMillis,
                                 HttpConnection conn)
    {
        log.info("Draining connection {}->{}", conn.remoteAddress(), conn.localAddress());
        var success = false;
        try
        {
            conn.shutdown(timeoutMillis);
            success = true;
        }
        catch (Exception e)
        {
            log.warn("Failed to drain connection {}->{}: {} , will close instead", conn.remoteAddress(), conn.localAddress(), e.getMessage());
        }
        if (!success)
        {
            try
            {
                conn.close();
            }
            catch (Exception ex)
            {
                log.warn("Failed to close connection {}->{}: {}", conn.remoteAddress(), conn.localAddress(), ex.getMessage());
            }
        }
    }
    
    public void configureRouter(Consumer<Router> consumer)
    {
        consumer.accept(this.router);
    }
    
    public static WebServerBuilder builder()
    {
        return new WebServerBuilder();
    }

    public Completable shutdown()
    {
        return shutdown(30 * 1000L);
    }

    public Completable shutdown(long timeoutMillis)
    {
        return this.shutdownAllConnections(timeoutMillis) //
                   .timeout(timeoutMillis * 2, TimeUnit.MILLISECONDS)
                   .doOnError(err -> log.warn("Error while shutting down HTTP server", err))
                   .onErrorComplete()
                   .andThen(this.stopListener())
                   .doOnComplete(() -> this.terminating.set(false));
    }

    public int actualPort()
    {
        return this.httpServer.actualPort();
    }

    public URI baseUri()
    {
        return this.baseUri;
    }

    public Completable startListener()
    {
        Completable init = Completable.complete();
        return init.andThen(prepareListener()) //
                   .andThen((listenAll ? //
                                       httpServer.rxListen(options.getPort()) : httpServer.rxListen()))
                   .doOnError(e -> log.error("error starting listener", e)) //
                   .ignoreElement()
                   .doOnComplete(() -> log.info("Server started {}", this));
    }

    public Completable stopListener()
    {
        return this.httpServer.rxClose().doOnComplete(() -> log.info("Server {} terminated", this));
    }

    private Completable prepareListener()
    {
        return Completable.fromAction(() ->
        {
            log.info("Starting HTTP server{}, host: {} port: {} , configuration: {}",
                     this.listenAll ? " on 0.0.0.0" : "",
                     this.options.getHost(),
                     this.options.getPort(),
                     this.options.toJson().encode());

            if (log.isDebugEnabled())
                router.getRoutes().forEach(route -> log.debug(" path: {} route: {}", route.getPath(), route));

            this.httpServer.requestHandler(router);
        });
    }

    private Completable shutdownAllConnections(long timeoutMillis)
    {
        return Completable.defer(() ->
        {
            this.terminating.set(true);
            getConnections().forEach(conn -> drainConnection(timeoutMillis, conn));
            return this.connectionSubject //
                                         .map(Set::isEmpty)
                                         .filter(Boolean::booleanValue)
                                         .firstOrError()
                                         .ignoreElement();
        });
    }

    private Set<HttpConnection> getConnections()
    {
        synchronized (this)
        {
            return Set.copyOf(this.connections);
        }
    }

    public Vertx getVertx()
    {
        return this.vertx;
    }
}
