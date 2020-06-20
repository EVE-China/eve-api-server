package com.github.evechina.blueprint.verticle;

import com.github.evechina.blueprint.service.PriceService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 定时任务
 */
public class ScheduledTaskVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(ScheduledTaskVerticle.class);

  private WebClient webClient;

  private final PriceService priceService = PriceService.getInstance();

  @Override
  public Completable rxStart() {
    webClient = WebClient.create(vertx);
    // 1小时更新一次eiv
    updateEIV();
    vertx.timerStream(3600000).toObservable().subscribe();
    return super.rxStart();
  }

  private void updateEIV() {
    webClient
      .getAbs("https://esi.evepc.163.com/latest/markets/prices/?datasource=serenity").putHeader("accept", "application/json")
      .rxSend()
      .subscribe(rsp -> {
        Observable.fromIterable(rsp.bodyAsJsonArray())
          .flatMap(item -> {
            return Observable.just((JsonObject)item);
          }).flatMap(item -> {
            Integer typeId = item.getInteger("type_id");
            Float adjustedPrice = item.getFloat("adjusted_price");
            Float averagePrice = item.getFloat("average_price");
            return priceService.updateEIV(typeId, adjustedPrice, averagePrice)
              .toObservable().flatMap(unused -> {
                return Observable.just(typeId);
              });
          }).subscribe(typeId -> {
            log.debug("{} eiv更新成功", typeId);
          }, err -> {
            log.error("更新eiv失败", err);
          });
      }, err -> {
        log.error("获取eiv数据失败", err);
      });
  }
}