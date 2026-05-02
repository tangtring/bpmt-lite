# v1.4.0 API 验收清单

## 基线

- Web：`http://127.0.0.1:8080/`
- API：`http://127.0.0.1:8081/api/`
- OpenAPI：`http://127.0.0.1:8081/api/openapi.json`
- Web 文档：`http://127.0.0.1:8081/api/docs/`
- API 认证：HMAC-SHA256
- 技术用户：`BPMT_API_ACT_AS`，缺省或不存在时兜底 `admin`
- Web/API 缓存：两个容器各自内嵌 Hazelcast，通过 compose 网络组成同一集群

当前实现分支的 Maven 版本仍是 `1.3.0`，本地构建镜像 tag 也会跟随为 `1.3.0`。正式发布 v1.4.0 时再统一切换 Maven 版本、`BPMT_IMAGE_TAG`、`BPMT_API_IMAGE_TAG` 和 GHCR tag。

## 必过项

- `scripts/verify-repo.sh` 通过。
- `mvn -s settings.local.xml -DskipTests compile` 通过。
- `mvn -s settings.local.xml -pl api -am test` 通过。
- `docker compose config` 通过。
- `scripts/build-image.sh` 通过。
- `scripts/build-api-image.sh` 通过。
- `/api/openapi.json` 返回 200。
- `/api/docs/` 返回 200。
- 无签名访问 `/api/v1/dynamic-tables` 返回 401。
- 正确签名访问 `/api/v1/dynamic-tables` 返回 200，且响应 `success=true`。
- API 创建测试动态表后，MariaDB 中存在物理表和 `TB_TABLE`、`TB_COLUMN` 元数据。
- API 调整测试动态表后，不重启 Web，Web 动态表管理页可读到新结构。
- Web 和 API 日志显示 Hazelcast 加入同一集群。

## 明确不验收

- 动态表业务数据 CRUD。
- 删除动态表。
- OAuth2/OIDC。
- 独立 Hazelcast Server 容器。
- 第三方平台适配器封装。
