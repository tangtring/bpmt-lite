# v1.1.0 docker compose 配置瘦身 spec

## 背景

`v1.0.0` 的 `docker-compose.yml` 把大量历史 properties 参数暴露成环境变量。这样虽然透明，但普通使用者第一次看到 compose 文件时负担很重。

当前 `docker/docker-entrypoint.sh` 已经为这些配置提供默认值，并保留 `config/overrides/*.properties` 追加覆盖机制。因此 `v1.1.0` 可以把 compose 中的环境变量收敛到必要项，把低频参数隐藏到 entrypoint 默认值和 override 文件中。

## 目标

- 默认 `docker-compose.yml` 只暴露快速启动需要的配置。
- 保留 `BPMT_HTTP_PORT`、`BPMT_DB_PORT`、`BPMT_IMAGE_TAG` 的宿主机覆盖方式。
- 保留数据库连接相关必要变量。
- 保留运行目录和 `config/overrides` 覆盖机制。
- README 中只展示常用配置，高级配置引导到 `config/overrides/*.properties`。

## 非目标

- 不删除 `docker/docker-entrypoint.sh` 中的默认 properties 生成逻辑。
- 不取消高级配置能力。
- 不引入 `.env` 作为必须步骤。
- 不调整 v1.0.0 已确认的默认访问地址和目录语义。
- 不把 Redis、Office、短信、微信、邮件等已关闭能力重新设计成默认能力。

## 默认 compose 保留项

`web.environment` 建议只保留：

```yaml
TZ: Asia/Shanghai
DB_HOST: mariadb
DB_PORT: 3306
DB_NAME: kyq
DB_USER: root
DB_PASSWORD: 123456
```

可选保留：

```yaml
LOG_LEVEL: info
SAFE_ADMIN: admin
```

不建议在默认 compose 中继续暴露：

- `PAGE_*`
- `SAFE_*` 中除 `SAFE_ADMIN` 外的低频项
- `REDIS_*`
- `OFFICE_*`
- `MAIL_*`
- `SMS_*`
- `WX_*`
- `HAZELCAST_*`
- `ACTIVITI_*`
- `QUARTZ_*`
- JDBC pool 和 SQL 统计低频参数

这些参数继续由 `docker/docker-entrypoint.sh` 默认值生成；如需修改，通过 `config/overrides/<name>.properties` 覆盖。

## override 示例

新增示例文件可以采用 `.example` 后缀，避免被运行时误读：

```text
config/overrides/page.properties.example
config/overrides/log.properties.example
config/overrides/jdbc.properties.example
```

README 中给出最小示例：

```properties
page.title=BPMT Lite
```

## 验收标准

1. 默认 `docker compose up -d` 可启动。
2. 未提供 `config/overrides` 时，entrypoint 仍生成完整 properties 文件。
3. `/` 返回 200。
4. `/ueditor/` 返回 200。
5. 添加 `config/overrides/page.properties` 后，容器内 `ROOT/WEB-INF/classes/page.properties` 末尾包含 override 内容。
6. `README.md` 的常用配置表只保留常用项。
7. `docker-compose.yml` 不再暴露已裁剪能力的配置项，例如 `OFFICE_*`、`SMS_ALI_*`。

## 实施顺序

1. 先瘦身 `docker-compose.yml`。
2. 保持 `docker/docker-entrypoint.sh` 默认值不变，只在发现无效或已裁剪配置时再清理。
3. 补充 `config/overrides/*.example`。
4. 更新 README 和 `docs/maintenance.md`。
5. 用默认 compose 验证启动和访问。
6. 用一个 override 文件验证覆盖机制。
