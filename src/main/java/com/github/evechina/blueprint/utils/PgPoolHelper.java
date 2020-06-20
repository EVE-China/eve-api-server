package com.github.evechina.blueprint.utils;

import io.vertx.reactivex.pgclient.PgPool;

import java.util.Objects;

public final class PgPoolHelper {

  private static PgPool instance;

  private PgPoolHelper() {

  }

  public static void init(PgPool pgPool) {
    instance = pgPool;
  }

  public static PgPool getPgPool() {
    return Objects.requireNonNull(instance, "请先初始化后再调用");
  }

}
