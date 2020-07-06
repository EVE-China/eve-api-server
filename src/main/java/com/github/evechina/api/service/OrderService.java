package com.github.evechina.api.service;

import com.github.evechina.api.utils.PgPoolHelper;
import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Tuple;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class OrderService {

  private static OrderService instance = null;

  public static void init() {
    instance = new OrderService();
  }

  public static OrderService getInstance() {
    return Objects.requireNonNull(instance, "请初始化后再调用");
  }

  private final PgPool pool = PgPoolHelper.getPgPool();

  /**
   * 清理过期数据
   */
  public Completable cleanupExpiredData() {
    // 删除1小时之前创建的数据
    String sql = "DELETE FROM market_order WHERE extract(hour from current_timestamp - updated) > 1";
    return pool.query(sql).rxExecute().ignoreElement();
  }

  /**
   * 更新订单数据
   *
   * @param regionId 星域编号
   * @param array    订单数据
   * @return 更新结果
   */
  public Completable updateOrders(long regionId, JsonArray array) {
    String sql = "INSERT INTO market_order(order_id, duration, is_buy_order, issued, location_id, min_volume, price, range, system_id, type_id, volume_remain, volume_total, updated, region_id) VALUES($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, current_timestamp, $13)" +
      " ON CONFLICT(order_id) DO UPDATE SET duration = $2, is_buy_order = $3, issued = $4, location_id = $5, min_volume = $6, price = $7, range = $8, system_id = $9, type_id = $10, volume_remain = $11, volume_total = $12, updated = current_timestamp, region_id = $13";
    List<Tuple> tuples = array.stream().map(item -> (JsonObject) item).map(item -> {
      Tuple tuple = Tuple.tuple();
      tuple.addLong(item.getLong("order_id"));
      tuple.addInteger(item.getInteger("duration"));
      tuple.addBoolean(item.getBoolean("is_buy_order"));
      String issued = item.getString("issued");
      tuple.addOffsetDateTime(OffsetDateTime.parse(issued, DateTimeFormatter.ISO_OFFSET_DATE_TIME));
      tuple.addLong(item.getLong("location_id"));
      tuple.addInteger(item.getInteger("min_volume"));
      tuple.addDouble(item.getDouble("price"));
      tuple.addString(item.getString("range"));
      tuple.addLong(item.getLong("system_id"));
      tuple.addLong(item.getLong("type_id"));
      tuple.addInteger(item.getInteger("volume_remain"));
      tuple.addInteger(item.getInteger("volume_total"));
      tuple.addLong(regionId);
      return tuple;
    }).collect(Collectors.toList());
    return pool.preparedQuery(sql).rxExecuteBatch(tuples).ignoreElement();
  }

}
