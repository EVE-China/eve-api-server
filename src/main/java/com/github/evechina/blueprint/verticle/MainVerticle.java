package com.github.evechina.blueprint.verticle;

import com.github.evechina.blueprint.service.BluePrintService;
import com.github.evechina.blueprint.service.PriceService;
import com.github.evechina.blueprint.utils.PgPoolHelper;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Verticle;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.file.FileSystem;
import io.vertx.reactivex.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;
import org.flywaydb.core.Flyway;

public class MainVerticle extends AbstractVerticle {

  private PgPool pgPool;

  @Override
  public Completable rxStart() {
    JsonObject config = getConfig();
    JsonObject db = config.getJsonObject("db");
    pgPool = initPgPool(vertx, db);
    PgPoolHelper.init(pgPool);
    BluePrintService.init();
    PriceService.init(vertx);
    return deploy(new HttpVerticle())
      .flatMap(unused -> deployWorker(new ScheduledTaskVerticle()))
      .ignoreElement();
  }

  @Override
  public Completable rxStop() {
    if (null != pgPool) {
      pgPool.close();
    }
    return Completable.complete();
  }

  private Single<String> deploy(Verticle verticle, DeploymentOptions options) {
    return vertx.rxDeployVerticle(verticle, options);
  }

  private Single<String> deployWorker(Verticle verticle) {
    DeploymentOptions options = new DeploymentOptions();
    options.setWorker(true);
    return vertx.rxDeployVerticle(verticle, options);
  }

  private Single<String> deploy(Verticle verticle) {
    DeploymentOptions deploymentOptions = new DeploymentOptions();
    deploymentOptions.setConfig(config());
    return deploy(verticle, deploymentOptions);
  }

  /**
   * 优先获取当前目录下的配置文件, 如果没有, 则从默认的config中获取
   *
   * @return 配置信息
   */
  private JsonObject getConfig() {
    String defaultPath = "conf/config.json";
    FileSystem fileSystem = vertx.fileSystem();
    if (fileSystem.existsBlocking(defaultPath)) {
      return fileSystem.readFileBlocking(defaultPath).toJsonObject();
    }
    return config();
  }

  public static PgPool initPgPool(Vertx vertx, JsonObject dbConfig) {
    if (null == dbConfig) {
      throw new RuntimeException("请配置数据源信息");
    }
    String host = dbConfig.getString("host");
    Integer port = dbConfig.getInteger("port");
    String database = dbConfig.getString("database");
    String user = dbConfig.getString("user");
    String password = dbConfig.getString("password");
    int maxPoolSize = dbConfig.getInteger("maxPoolSize", 30);
    PgConnectOptions connectOptions = new PgConnectOptions()
      .setHost(host)
      .setPort(port)
      .setDatabase(database)
      .setUser(user)
      .setPassword(password);
    PoolOptions poolOptions = new PoolOptions().setMaxSize(maxPoolSize);
    initFlywayDB(host, port, database, user, password);
    return PgPool.pool(vertx, connectOptions, poolOptions);
  }

  private static void initFlywayDB(String host, int port, String database, String user, String password) {
    String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
    Flyway flyway = Flyway.configure().dataSource(jdbcUrl, user, password).load();
    flyway.migrate();
  }
}
