# v1.4.1 API 验收清单

## 验收范围

- Nginx 单入口：`/`、`/ueditor/`、`/api/docs/`、`/api/openapi.json`
- API 动态表模块路径重整
- 数据库操作模块：`query`、`find`、`save`、`exec`
- issue #9：默认 `SAFE_ROLE` 不再阻断快照能力
- issue #10：`/flow/CommonFlowAction/taskList.shtml` 点击查看/处理可正常打开任务页面

## 命令清单

```bash
docker compose config
```

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -s settings.local.xml -pl api -am -Dtest=ApiDocsContractTest,ApiServletTest,DynamicTableControllerTest,DynamicTableServiceTest,DynamicTableValidatorTest,HmacSignatureTest,ApiUserContextTest,DatabaseOperationServiceTest -DfailIfNoTests=false test
```

```bash
scripts/build-api-image.sh
```

```bash
curl -fsSI http://127.0.0.1/
curl -fsSI http://127.0.0.1/ueditor/
curl -fsSI http://127.0.0.1/api/openapi.json
curl -fsSI http://127.0.0.1/api/docs/
```

```bash
scripts/smoke-api.sh
```

## 通过标准

- 所有命令返回成功。
- 入口探活均为 `HTTP/1.1 200`。
- API 签名 smoke 通过。
- 动态表路径为 `/api/v1/dynamic-tables/*` 新基线。
- 数据库操作模块 4 个接口可用，且写操作默认受配置开关保护。
- 默认 `safe.role=DEV_SYS` 已写入运行时配置生成逻辑。
- issue #10 复现场景回归通过，不再出现异常 `.view` 跳转链接。
