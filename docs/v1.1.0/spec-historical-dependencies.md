# v1.1.0 历史 Maven 依赖分发 spec

## 背景

`v1.0.0` 已经移除退役 RiverSoft 私服配置，并把当前 checkout 的有效 Maven 本地仓库固定为 `/Volumes/vm/maven/repository`。这个做法让本机可以稳定构建，但新环境仍会遇到公共 Maven 仓库无法下载旧 jar 的问题。

进入本 spec 实现前，必须先完成 `docs/v1.1.0/pretask-office-dependency-assessment.md` 中的取舍决策。已确认 v1.1.0 默认发行割舍 Aspose、JPedal、JODConverter 三组依赖，它们不进入历史依赖分发范围。

2026-04-26 使用空本地 Maven 仓库和公共仓库设置实测，全仓编译第一批失败在 `util` 模块，缺失依赖为：

| Maven 坐标 | 当前本机是否已有 artifact |
| --- | --- |
| `org.hyperic:sigar:1.6.5.132-6` | 已确认割舍 |
| `org.apache.commons:commons-jexl:2.1.2` | 已回退到公共仓库可下载的 `2.1.1` |
| `org.artofsolving.jodconverter:jodconverter-core:3.1.1` | 是 |
| `com.aspose:aspose-slides:15.9.0` | 是 |
| `com.aspose:aspose-words:14.12.0` | 是 |
| `com.aspose:aspose-cells:8.6.3` | 是 |
| `com.jpedal:pdf2image:4.92b23` | 是 |

这只是第一批缺口。导入这些 artifact 后，后续模块仍可能暴露下一批缺失依赖。因此实现必须支持迭代发现和扩展清单。

其中 `com.aspose:*`、`com.jpedal:pdf2image`、`org.artofsolving.jodconverter:jodconverter-core` 已进入前置取舍评估，并确认不作为 v1.1.0 默认分发对象。

后续公共仓库空仓验证继续暴露数据库工具链依赖缺口：

| Maven 坐标 | 处理方式 |
| --- | --- |
| `org.jumpmind.symmetric:symmetric-util:3.7.19` | 官方仓库 `https://maven.jumpmind.com/repo` 的 metadata 仍列出 `3.7.19`，但实际 pom/jar 返回 404；已微调到同一 `3.7.x` 线可下载的 `3.7.38` |
| `org.jumpmind.symmetric:symmetric-db:3.7.19` | 同上，已微调到 `3.7.38` |
| `org.jumpmind.symmetric:symmetric-jdbc:3.7.19` | 同上，已微调到 `3.7.38` |
| `com.oracle:ojdbc6:11.2.0.4` | 已切换到 Central 可下载的官方坐标 `com.oracle.database.jdbc:ojdbc6:11.2.0.4` |
| `com.microsoft:sqlserver-jdbc:4.0.0` | 已切换到 Central 可下载的 Java 8 兼容坐标 `com.microsoft.sqlserver:mssql-jdbc:6.4.0.jre8` |

完成 JDBC 坐标切换后，空本地仓继续编译已能下载 Oracle、SQL Server 和 JumpMind 依赖，下一批阻断推进到：

| Maven 坐标 | 状态 |
| --- | --- |
| `com.taobao:taobao-sdk:1.1` | 待处理 |
| `com.github.kenglxn.QRGen:javase:2.1.0` | 待处理 |
| `com.github.kenglxn.QRGen:core:2.1.0` | 待处理 |

## 目标

- 提供一份机器可读的历史依赖清单。
- 提供一个 GitHub 或用户自备分发包，包含 Maven 仓库目录结构下的 jar、pom 和校验信息。
- 提供一个一键导入脚本，把分发包复制到目标 Maven 本地仓库。
- 让维护者可以在新机器上不访问退役私服也能完成 Java 8 Maven 编译。

## 非目标

- 不替换这些 jar。
- 不修改业务代码绕过依赖。
- 不重新引入退役 RiverSoft 私服。
- 不把二进制 jar 直接提交到 git 工作树。
- 不绕过第三方依赖的再分发许可审查。

## 推荐方案

优先使用用户自备或私有分发包；只有确认 artifact 允许公开再分发时，才使用 GitHub Release asset 分发压缩包：

```text
bpmt-lite-maven-artifacts-1.1.0.tar.gz
```

压缩包内保持 Maven 本地仓库目录结构：

```text
repository/
  org/artofsolving/jodconverter/jodconverter-core/3.1.1/
  com/aspose/aspose-slides/15.9.0/
  com/aspose/aspose-words/14.12.0/
  com/aspose/aspose-cells/8.6.3/
  com/jpedal/pdf2image/4.92b23/
manifest.json
SHA256SUMS
```

仓库内提交脚本和清单，不提交 jar：

```text
config/maven-artifacts/v1.1.0-manifest.json
scripts/import-maven-artifacts.sh
scripts/build-maven-artifacts-bundle.sh
```

`scripts/import-maven-artifacts.sh` 支持两种来源：

- `--bundle <path>`：从本地压缩包导入，适合维护者验证或离线环境。
- `--release-url <url>`：从 GitHub Release asset 下载并导入，适合普通使用者。

默认目标仓库读取 `settings.local.xml` 中的 `<localRepository>`；如果读取失败，则允许显式传入：

```bash
scripts/import-maven-artifacts.sh --bundle /path/to/bpmt-lite-maven-artifacts-1.1.0.tar.gz --repo /Volumes/vm/maven/repository
```

## 备选方案

### 方案 A：GitHub Release asset

优点：

- 不污染 git 历史。
- 适合较大二进制包。
- 与版本发布流程一致。

缺点：

- 需要发布资产和 checksum 管理。
- 如果第三方 jar 不允许公开再分发，需要改为私有 release 或只提供内部包。

### 方案 B：Git LFS

优点：

- artifact 和代码版本绑定更紧。

缺点：

- 使用者需要额外理解 LFS。
- 仓库克隆体验变重。
- 不适合当前简化发行目标。

### 方案 C：脚本逐个 `mvn install:install-file`

优点：

- 每个 artifact 的 Maven 坐标最明确。

缺点：

- 仍然需要先分发 jar 文件。
- 对当前问题没有比复制 Maven repo layout 更简单。

推荐采用方案 A，并在导入脚本里优先使用 Maven repo layout 复制方式。只有当 artifact 缺少 pom 时，才在构建 bundle 阶段补齐最小 pom。

## 许可和公开边界

`com.aspose:*`、`com.jpedal:pdf2image` 和 `org.artofsolving.jodconverter:jodconverter-core` 已确认割舍，不进入本 spec 的 artifact bundle。

- 如果允许公开再分发，可以作为公开 GitHub Release asset 发布。
- 如果只允许内部使用，应发布到私有 GitHub Release 或私有仓库，并在 README 中说明普通用户需要自行提供 artifact bundle。
- 如果不能再分发，只保留 manifest 和导入脚本，不发布对应 jar。

这个许可检查是发布 gate，不应在实现阶段被跳过。

## 验收标准

使用空 Maven 本地仓库验证：

1. 使用公共 Maven settings 执行编译，确认会因历史依赖缺失失败。
2. 执行 `scripts/import-maven-artifacts.sh` 导入 GitHub 分发包。
3. 再次执行 Java 8 Maven 全仓编译。
4. 编译结果为 `BUILD SUCCESS`。

验收命令应在实现计划中固定为可复制命令，并记录到 `docs/maintenance.md`。

## 对现有文件的影响

预计新增：

- `config/maven-artifacts/v1.1.0-manifest.json`
- `scripts/import-maven-artifacts.sh`
- `scripts/build-maven-artifacts-bundle.sh`

预计修改：

- `README.md`
- `docs/maintenance.md`
- `settings.example.xml`
- `scripts/verify-repo.sh`

`settings.example.xml` 仍不应重新引入退役 RiverSoft 私服地址。
