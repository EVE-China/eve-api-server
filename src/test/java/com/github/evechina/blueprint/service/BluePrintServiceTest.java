package com.github.evechina.blueprint.service;

import com.github.evechina.blueprint.utils.PgPoolHelper;
import com.github.evechina.blueprint.verticle.MainVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.pgclient.PgPool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class BluePrintServiceTest {

  @BeforeAll
  static void init(Vertx vertx) {
    JsonObject config = vertx.fileSystem().readFileBlocking("conf/config.json").toJsonObject();
    PgPool client = MainVerticle.initPgPool(vertx, config.getJsonObject("db"));
    PgPoolHelper.init(client);
    BluePrintService.init();
  }

  @Test
  void findAllByName(VertxTestContext testContext) {
    BluePrintService bluePrintService = BluePrintService.getInstance();
    bluePrintService.findAllByName("").subscribe(bluePrints -> {
      if (bluePrints.size() > 0) {
        testContext.completeNow();
      } else {
        testContext.failNow(new RuntimeException("没有查询到数据"));
      }
    }, e -> {
      throw new RuntimeException("查询蓝图失败", e);
    });
  }
}
