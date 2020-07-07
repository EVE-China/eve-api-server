package com.github.evechina.api.verticle;

import com.github.evechina.api.service.OrderService;
import com.github.evechina.api.service.PriceService;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

/**
 * 定时任务
 */
public class ScheduledTaskVerticle extends AbstractVerticle {

  private static final Logger log = LoggerFactory.getLogger(ScheduledTaskVerticle.class);

  private WebClient webClient;

  private final PriceService priceService = PriceService.getInstance();

  private final OrderService orderService = OrderService.getInstance();

  @Override
  public Completable rxStart() {
    webClient = WebClient.create(vertx);
    // 1小时更新一次eiv
    updateEIV();
    vertx.periodicStream(3600000).handler(unused -> {
      updateEIV();
    });
    // 1小时更新一次订单
    updateOrder(1, 3600000);
    return super.rxStart();
  }

  /**
   * 更新eiv
   */
  private void updateEIV() {
    webClient
      .getAbs("https://esi.evepc.163.com/latest/markets/prices/?datasource=serenity").putHeader("accept", "application/json")
      .rxSend().flatMap(rsp -> Single.just(rsp.bodyAsJsonArray()))
      .subscribe(array -> {
        priceService.updateEIV(array).subscribe(() -> {
          log.info("eiv更新成功");
        }, err -> {
          log.error("更新eiv失败", err);
        });
      }, err -> {
        log.error("获取eiv数据失败", err);
      });
  }

  /**
   * 更新订单
   *
   * @param page  页码
   * @param delay 更新间隔
   */
  private void updateOrder(int page, int delay) {
    Runnable nextDelay = () -> {
      vertx.timerStream(delay).handler(unused -> {
        updateOrder(1, delay);
      });
    };
    Consumer<Integer> next = nextPage -> {
      updateOrder(nextPage, delay);
    };
    webClient
      .getAbs("https://esi.evepc.163.com/latest/markets/10000002/orders/?datasource=serenity&order_type=all&page=" + page).putHeader("accept", "application/json")
      .rxSend()
      .subscribe(rsp -> {
        JsonArray array;
        try {
          array = rsp.bodyAsJsonArray();
        } catch (DecodeException e) {
          log.error("获取第{}页订单数据失败", page, e);
          next.accept(page + 1);
          return;
        }
        if (array.size() > 0) {
          if (1 == page) {
            log.info("开始处理订单数据");
          }
          orderService.updateOrders(10000002, array).subscribe(() -> {
            log.debug("获取第{}页订单数据成功", page);
            next.accept(page + 1);
          }, err -> {
            log.error("获取第{}页订单数据失败", page, err);
            next.accept(page + 1);
          });
        } else {
          log.info("订单数据处理结束");
          // 清理数据
          orderService.cleanupExpiredData().subscribe();
          nextDelay.run();
        }
      }, err -> {
        log.error("获取订单数据失败", err);
        // 重新开始
        nextDelay.run();
      });
  }
}
