package com.github.evechina.api.service;

import com.github.evechina.api.utils.PgPoolHelper;
import domain.*;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.reactivex.sqlclient.Row;
import io.vertx.reactivex.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 蓝图服务类
 */
public class BluePrintService {

  private static final Logger log = LoggerFactory.getLogger(BluePrintService.class);

  private static BluePrintService instance;

  public static void init() {
    instance = new BluePrintService();
  }

  public static BluePrintService getInstance() {
    return Objects.requireNonNull(instance, "请初始化后再调用");
  }

  private final PgPool client = PgPoolHelper.getPgPool();

  private BluePrintService() {
  }

  /**
   * 按照名称查询蓝图
   *
   * @param name 名称
   * @return 符合条件的蓝图
   */
  public Single<List<BluePrint>> findAllByName(String name) {
    String sql = "select bp.id, bp.maxProductionLimit, (select value from eve.type_i18n where typeId = bp.id and key = 'name' and language = 'zh') AS name from eve.blueprint bp where bp.id in (select typeId from eve.type_i18n where key = 'name' and language = 'zh' and value like '%' || $1 || '%');";
    Tuple params = Tuple.of(name);
    return client.preparedQuery(sql).rxExecute(params).flatMap(resultSet -> {
      try {
        List<BluePrint> bluePrints = new ArrayList<>(resultSet.size());
        for (Row row : resultSet) {
          int id = row.getInteger("id");
          String bluePrintName = row.getString("name");
          int maxProductionLimit = row.getInteger("maxProductionLimit".toLowerCase());
          BluePrint bluePrint = new BluePrint(id, bluePrintName, maxProductionLimit);
          bluePrints.add(bluePrint);
        }
        return Single.just(bluePrints);
      } catch (RuntimeException e) {
        return Single.error(new RuntimeException("按名称查询蓝图失败", e));
      }
    }).observeOn(Schedulers.io());
  }

  /**
   * 获取指定蓝图的制造业数据
   *
   * @param typeId 蓝图编号
   * @return 制造业数据
   */
  public Single<Manufacturing> getManufacturing(int typeId) {
    String sql = "select ba.time from eve.blueprint_activity ba where ba.id = $1 and ba.type = 2;";
    Tuple params = Tuple.of(typeId);
    return client.preparedQuery(sql).rxExecute(params).flatMap(resultSet -> {
      if (resultSet.rowCount() == 0) {
        return Single.error(new RuntimeException("该蓝图不能进行制造"));
      }
      Row row = resultSet.iterator().next();
      long time = row.getLong("time");
      Manufacturing manufacturing = new Manufacturing();
      manufacturing.setTime(time);
      return Single.just(typeId).flatMap(id -> {
        // Materials
        return getBluePrintMaterials(id).flatMap(materials -> {
          manufacturing.setMaterials(materials);
          return Single.just(id);
        });
      }).flatMap(id -> {
        // Product
        return getBluePrintProduct(id).flatMap(product -> {
          manufacturing.setProduct(product);
          return Single.just(manufacturing);
        });
      });
    }).observeOn(Schedulers.io());
  }

  private Single<List<Item>> getBluePrintMaterials(int id) {
    String sql = "select bm.typeId as id, ti.value as name, t.volume, bm.quantity from eve.blueprint_material bm left join eve.type t on bm.typeId = t.id left join eve.type_i18n ti on t.id = ti.typeId where bm.id = $1 and bm.activityType = 2 and ti.language = 'zh' and ti.key = 'name';";
    List<Item> materials = new ArrayList<>();
    Tuple params = Tuple.of(id);
    return client.preparedQuery(sql).rxExecute(params).flatMap(resultSet -> {
      for (Row row : resultSet) {
        int materialId = row.getInteger("id");
        String name = row.getString("name");
        float volume = row.getFloat("volume");
        Type type = new Type(materialId, name, volume);
        long quantity = row.getLong("quantity");
        materials.add(new Item(type, quantity));
      }
      return Single.just(materials);
    });
  }

  /**
   * 暂时不涉及技能
   */
  private Map<Skill, Integer> getBluePrintSkills(int id) {
    /*String sql = "select bs.typeId as id, ti.value as name, bs.level as level from blueprint_skill bs left join type_i18n ti on bs.typeId = ti.typeId where bs.id = ? and bs.activityType = 2 and ti.language = 'zh' and ti.key = 'name';";
    Map<Skill, Integer> skills = new HashMap<>();
    try (PreparedStatement prepareStatement = connection.prepareStatement(sql)) {
      prepareStatement.setInt(1, id);
      ResultSet resultSet = prepareStatement.executeQuery();
      while (resultSet.next()) {
        int skillId = resultSet.getInt("id");
        String name = resultSet.getString("name");
        int level = resultSet.getInt("level");
        Skill skill = new Skill(skillId, name, level, level);
        skills.put(skill, level);
      }
      resultSet.close();
      return skills;
    } catch (SQLException e) {
      throw new ProjectException("查询蓝图技能失败", e);
    }*/
    return null;
  }

  private Single<Item> getBluePrintProduct(int id) {
    String sql = "select bp.typeId as id, ti.value as name, t.volume as volume, bp.quantity as quantity from eve.blueprint_product bp left join eve.type t on bp.typeId = t.id left join eve.type_i18n ti on bp.typeId = ti.typeId where bp.id = $1 and bp.activityType = 2 and ti.language = 'zh' and ti.key = 'name';";
    Tuple params = Tuple.of(id);
    return client.preparedQuery(sql).rxExecute(params).flatMap(resultSet -> {
      Row row = resultSet.iterator().next();
      int productId = row.getInteger("id");
      String productName = row.getString("name");
      float volume = row.getFloat("volume");
      long quantity = row.getLong("quantity");
      Type type = new Type(productId, productName, volume);
      Item item = new Item(type, quantity);
      return Single.just(item);
    });
  }
}
