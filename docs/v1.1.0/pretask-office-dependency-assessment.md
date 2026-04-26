# v1.1.0 前置任务：Office/PDF 依赖取舍评估

## 背景

`v1.1.0` 原计划通过历史 Maven 依赖分发解决公共仓库无法下载旧 jar 的问题。进一步分析后，`com.aspose:*`、`com.jpedal:pdf2image`、`org.artofsolving.jodconverter:jodconverter-core` 三组依赖存在较高的合规或维护成本。

本前置任务用于回答一个问题：当前项目有哪些功能真实依赖这三组库，这些功能是否属于 bpmt-lite 的必要能力。

## 依赖声明

| 依赖 | 声明位置 | 版本来源 |
| --- | --- | --- |
| `com.aspose:aspose-slides` | `util/pom.xml` | `parent/pom.xml` 中的 `aspose-slides.version=15.9.0` |
| `com.aspose:aspose-words` | `util/pom.xml` | `parent/pom.xml` 中的 `aspose-words.version=14.12.0` |
| `com.aspose:aspose-cells` | `util/pom.xml` | `parent/pom.xml` 中的 `aspose-cells.version=8.6.3` |
| `com.jpedal:pdf2image` | `util/pom.xml` | `parent/pom.xml` 中的 `jpedal.version=4.92b23` |
| `org.artofsolving.jodconverter:jodconverter-core` | `util/pom.xml`、`platform/pom.xml` | `parent/pom.xml` 中的 `jodconverter.version=3.1.1` |

## 代码调用面

### Aspose

集中在 `util/src/main/java/com/riversoft/util/OfficeUtils.java`：

- `ppt` / `pptx` 转图片、PDF、HTML。
- `doc` / `docx` 转 PDF、HTML。
- `xls` / `xlsx` 转 PDF、图片、SVG、HTML。
- 静态初始化中尝试读取 `aspose/slides-license.xml`、`aspose/words-license.xml`、`aspose/cells-license.xml`。

业务入口：

- `platform/src/main/java/com/riversoft/wx/qy/AgentHelper.java`
  - `file2mpnews(...)` 把 `ppt`、`xls`、`doc`、`pdf` 转成图片，用于企业微信图文素材。
- `platform/src/main/java/com/riversoft/wx/mp/MpHelper.java`
  - `file2mpnews(...)` 把 `ppt`、`xls`、`doc`、`pdf` 转成图片，用于公众号图文素材。

### JPedal

集中在 `util/src/main/java/com/riversoft/util/OfficeUtils.java`：

- `pdf2jpgs(...)` 把 PDF 每页转成 JPG。

业务入口：

- `AgentHelper.file2mpnews(...)`
  - `doc` 先通过 Aspose 转 PDF，再通过 JPedal 转图片。
  - `pdf` 直接通过 JPedal 转图片。
- `MpHelper.file2mpnews(...)`
  - 同上。

额外编译耦合：

- `platform/src/main/java/com/riversoft/core/exception/ExceptionType.java`
  - `PDF(1200, "PDF文件处理异常", PdfException.class)` 直接引用 `org.jpedal.exception.PdfException`。
  - 即使删除 `OfficeUtils` 中的 PDF 转图逻辑，也需要处理该异常类型依赖。

### artofsolving / JODConverter

集中在 `platform/src/main/java/com/riversoft/platform/office/ConverterHelper.java`：

- 通过 `ExternalOfficeManagerConfiguration` 连接外部 Office 服务。
- 通过 `OfficeDocumentConverter` 执行 Office 文档转换。
- 转换开关为 `office.flag`。

业务入口：

- `platform/src/main/java/com/riversoft/module/widget/FileAction.java`
  - `downloadOffice(...)` 中，非 PDF 文件会调用 `convertOfficeFile(...)` 转 PDF 后预览。
  - `convertOfficeFile(...)` 对 `doc`、`docx`、`xls`、`xlsx`、`ppt`、`pptx` 转 PDF。
- `platform/src/main/java/com/riversoft/module/view/viewer/ViewerViewAction.java`
  - 导出 PDF 时调用 `ConverterHelper.convert(...)` 把 HTML 或 Excel 输出转 PDF。
- `platform/src/main/java/com/riversoft/module/development/SystemAction.java`
  - 系统设置页读写 `office.flag`、`office.prepare`、`office.port` 等配置。
- `platform/src/main/webapp/xhtml/development/SystemAction/office_setting.jsp`
  - 提供文档转换相关配置 UI。

当前 Docker 默认值：

- `OFFICE_FLAG=false`
- `OFFICE_PREPARE=false`
- `OFFICE_PORT=2002`

因此默认 Docker 运行路径下，JODConverter 对应能力默认关闭。

## 功能分组

| 功能 | 依赖 | 入口 | 当前默认是否开启 | 初步重要性 |
| --- | --- | --- | --- | --- |
| 微信/企业微信文件转图文素材 | Aspose、JPedal | `AgentHelper.file2mpnews`、`MpHelper.file2mpnews` | 取决于微信模块配置 | 可选 |
| PDF 转图片 | JPedal | `OfficeUtils.pdf2jpgs` | 取决于微信文件转图文路径 | 可选 |
| 附件 Office 在线预览 | JODConverter | `FileAction.downloadOffice` | `office.flag=false` 时不可用 | 可选 |
| 报表/视图导出 PDF | JODConverter | `ViewerViewAction` | `office.flag=false` 时不可用 | 可选偏重要 |
| Office 转换系统设置页 | JODConverter | `SystemAction`、`office_setting.jsp` | UI 仍存在 | 可选配置面 |

## 初步取舍判断

### 已确认割舍：Aspose + JPedal

这两组库主要服务于微信图文素材中的文件转图片能力，不属于 bpmt-lite 的核心 BPM、动态表格、流程、基础表单运行链路。

执行方向：

- 将 `OfficeUtils` 中依赖 Aspose/JPedal 的实现从默认构建中移除。
- 微信文件转图文素材能力降级为不支持 `ppt`、`xls`、`doc`、`pdf` 自动转图片。
- 保留明确错误提示，避免运行期 `ClassNotFoundException`。
- 移除 `ExceptionType` 对 `PdfException.class` 的直接引用。

### 已确认割舍：artofsolving / JODConverter

JODConverter 支撑附件 Office 预览和部分 PDF 导出。虽然当前 Docker 默认关闭，但“在线预览”和“导出 PDF”更接近平台通用能力。

执行方向：

- v1.1.0 默认发行删除 JODConverter 依赖。
- 用户影响需要在维护文档中明确：
  - 非 PDF Office 附件无法在线转 PDF 预览。
  - 视图导出 PDF 可能不可用或只保留 Word/Excel/HTML 导出。
- `ConverterHelper` 保留同名入口，但固定返回转换不可用。

## 推荐前置任务结论

已确认 v1.1.0 执行“默认发行裁剪”：

1. 割舍 Aspose/JPedal 相关功能，不进入默认构建和历史依赖分发。
2. 割舍 JODConverter 相关功能，运行行为按 `office.flag=false` 收口。
3. 保留基础上传、普通下载、PDF 文件直接预览等不依赖三组库的能力。
4. 对 Office/PDF 转换相关入口返回清晰“不支持”提示，而不是运行期类缺失。
5. 更新历史依赖分发 spec：
   - 默认 bundle 不包含 Aspose/JPedal/JODConverter。
   - 后续只处理仍然必须保留且许可允许的历史 artifact。

## 验收建议

前置任务完成后，应至少验证：

- 移除 Aspose/JPedal/JODConverter 后，`mvn -s settings.local.xml -DskipTests compile` 通过。
- 移除 Aspose/JPedal/JODConverter 后，公共仓库缺失清单不再包含这三组依赖。
- 默认 Docker compose 启动后，`/` 和 `/ueditor/` 仍返回 200。
- 微信文件转图文素材相关入口在不支持 Office/PDF 转图时返回清晰错误。
- `office.flag=false` 时，附件上传、普通下载、PDF 文件直接预览不受影响。
