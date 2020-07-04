# eve-api-server
提供一些api接口供其他项目使用

# 注意事项

1. 本项目虽然使用了[flywaydb](https://flywaydb.org/getstarted/), 但由于eve的sde数据太多, 并且有频繁变更的可能,
所以不将eve的sql数据加入到flywaydb的升级脚本中.
因此在初次使用时需要手动执行sde的sql脚本.
[sde数据来源](https://github.com/EVE-China/sde-to-sql)

# 项目启动

```sh
mvn clean compile exec:java 
```