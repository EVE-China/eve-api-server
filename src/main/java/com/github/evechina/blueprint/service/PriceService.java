package com.github.evechina.blueprint.service;


import com.github.evechina.blueprint.utils.PgPoolHelper;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Tuple;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 价格服务类
 */
public class PriceService {

  private static PriceService instance = null;

  private final WebClient webClient;

  private final PgPool pgPool = PgPoolHelper.getPgPool();

  private PriceService(Vertx vertx) {
    this.webClient = WebClient.create(vertx);
  }

  public static void init(Vertx vertx) {
    instance = new PriceService(vertx);
  }

  public static PriceService getInstance() {
    return Objects.requireNonNull(instance, "请初始化后再调用");
  }

  private String getTypePriceUrl(int typeId) {
    return "https://www.ceve-market.org/api/market/region/10000002/system/30000142/type/" + typeId
      + ".json";
  }

  /**
   * 查询指定物品的最低卖价
   *
   * @param typeId typeId
   * @return 查询结果
   */
  public Single<JsonObject> query(int typeId) {
    String url = getTypePriceUrl(typeId);
    return webClient.getAbs(url)
      .putHeader(HttpHeaders.CONTENT_TYPE.toString(), "application/json")
      .rxSend().flatMap(rsp -> {
        JsonObject jsonObject = rsp.body().toJsonObject();
        return Single.just(jsonObject.getJsonObject("sell"));
      }).observeOn(Schedulers.io());
  }

  /**
   * 更新预估价格
   * @param typeId 物品编号
   * @param adjustedPrice 调整价格
   * @param averagePrice 平均价格
   * @return 更新结果
   */
  public Completable updateEIV(int typeId, Float adjustedPrice, Float averagePrice) {
    LocalDateTime now = LocalDateTime.now();
    String sql = "INSERT INTO item_eiv(item_id, adjusted_price, average_price, updated_at) VALUES($1, $2, $3, $4) ON CONFLICT(item_id) DO UPDATE SET adjusted_price = $2, average_price = $3, updated_at = $4";
    Tuple params = Tuple.of(typeId, adjustedPrice, averagePrice, now);
    return pgPool.preparedQuery(sql).rxExecute(params).ignoreElement();
  }
}
