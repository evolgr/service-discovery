package com.intracom.common.postgres;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.intracom.common.web.VertxBuilder;

import io.reactiverse.pgclient.PgClient;
import io.reactiverse.pgclient.PgPool;
import io.vertx.reactivex.core.Vertx;

public class ChatServiceTest {
    
    private static final Logger log = LoggerFactory.getLogger(ChatServiceTest.class);
    private final Vertx vertx = new VertxBuilder().build();
    private PgPool pgPool;
    
  @BeforeClass
  public void beforeClass() {
      
      var parameters = new ChatParameters("10.158.166.125", //
                                          8432, //
                                          "best", //
                                          10, //
                                          "best", //
                                          "best");
      this.pgPool = PgClient.pool(this.vertx.getDelegate(), parameters.getPgPoolOptions());
  }

  @AfterClass
  public void afterClass() {
      // this.pgPool.close();
      this.vertx.close();
  }

  @Test
  public void getMessagesTest() {
      var service = ChatService.start(this.pgPool, result -> {
          if (result.succeeded())
              log.info("VAGGELIS");
          else
              log.error("ZONG", result.cause());
      });
      
      service.getMessages(result -> {
          if (result.succeeded())
              result.result().forEach(row -> log.info("ZZZZZ {}", row));
//              log.info("ZZZ {}", result.result().encodePrettily());
          else
              log.info("XXXX {}", result.cause());
      });
  }

}
