package com.intracom.common.vaggelis;

import com.intracom.common.web.VertxBuilder;

import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.PgPoolOptions;
import io.vertx.reactivex.core.Vertx;

/**
 * 
 */
public class MyTest
{

    private final static Vertx vertx = new VertxBuilder().build();

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        PgPoolOptions options = new PgPoolOptions().setPort(8432)
                                                   .setHost("10.158.166.125")
                                                   .setDatabase("best")
                                                   .setUser("best")
                                                   .setPassword("best")
                                                   .setMaxSize(5)
                                                   .setSsl(false)
                                                   .setConnectTimeout(10)
                                                   .setSsl(false);
        System.out.println(options.getUser());
        System.out.println(options.getPassword());
        System.out.println(options.getDatabase());
        System.out.println(options.getHost());
        System.out.println(options.getPort());

        // Create the pooled client
        PgPool client = PgClient.pool(vertx.getDelegate(), options);

        io.reactiverse.reactivex.pgclient.PgPool newClient = new io.reactiverse.reactivex.pgclient.PgPool(client);
        newClient.rxGetConnection() //
                 .flatMap(conn -> conn.rxQuery("SELECT * FROM chat")//
                                      .doAfterTerminate(conn::close))
                 .subscribe(result ->
                 {
                     System.out.println("Found " + result.rowCount());
                     result.value().forEach(row -> System.out.println("tatatoutou: " + row.getString(1)));
                 }, throwable ->
                 {
                     System.out.println("Error " + throwable);
                 });
    }

}
