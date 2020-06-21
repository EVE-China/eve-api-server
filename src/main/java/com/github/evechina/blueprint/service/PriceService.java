package com.github.evechina.blueprint.service;


import com.github.evechina.blueprint.utils.PgPoolHelper;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.client.WebClient;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.RowIterator;
import io.vertx.reactivex.sqlclient.Tuple;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
      }).flatMap(sell -> {
        return getItemEIV(typeId).flatMap(eiv -> {
          sell.put("eiv", eiv);
          return Single.just(sell);
        });
      });
  }

  /**
   * @param typeId 编号
   * @return 查询结果, 如果没记录则返回空json
   */
  public Single<JsonObject> getItemEIV(int typeId) {
    String sql = "SELECT adjusted_price, average_price, updated_at FROM item_eiv WHERE item_id = $1";
    return pgPool.preparedQuery(sql).rxExecute(Tuple.of(typeId)).flatMap(rowSet -> {
      RowIterator<Row> iterator = rowSet.iterator();
      JsonObject eiv = new JsonObject();
      if (iterator.hasNext()) {
        Row row = iterator.next();
        eiv.put("adjusted_price", row.getFloat("adjusted_price"));
        eiv.put("average_price", row.getFloat("average_price"));
        eiv.put("updated_at", row.getFloat("updated_at"));
      }
      return Single.just(eiv);
    });
  }

  /**
   * 更新预估价格
   *
   * @param array 等待更新的数据
   * @return 更新结果
   */
  public Completable updateEIV(JsonArray array) {
    return Observable.<List<Tuple>>create(emitter -> {
      List<Tuple> tuples = array.stream().map(item -> (JsonObject) item).map(item -> {
        Integer typeId = item.getInteger("type_id");
        Float adjustedPrice = item.getFloat("adjusted_price");
        Float averagePrice = item.getFloat("average_price");
        return Tuple.of(typeId, adjustedPrice, averagePrice);
      }).collect(Collectors.toList());
      emitter.onNext(tuples);
    }).subscribeOn(Schedulers.computation()).flatMap(tuples -> {
      String sql = "INSERT INTO item_eiv(item_id, adjusted_price, average_price, updated_at) VALUES($1, $2, $3, current_timestamp) ON CONFLICT(item_id) DO UPDATE SET adjusted_price = $2, average_price = $3, updated_at = current_timestamp";
      return pgPool.preparedQuery(sql).rxExecuteBatch(tuples).toObservable();
    }).ignoreElements();
  }
}
