# Office Dependency Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove Aspose, JPedal, and JODConverter from the default bpmt-lite build while preserving clear disabled behavior for Office/PDF conversion paths.

**Architecture:** Keep existing public Java entrypoints so callers continue to compile, but replace conversion implementations with explicit unsupported behavior. Remove Maven dependency declarations and direct third-party imports, then verify Java 8 compile and repository hygiene.

**Tech Stack:** Java 8, Maven 3, Tomcat 7, MariaDB, existing bpmt-lite shell scripts.

---

## File Structure

- `parent/pom.xml`: remove version properties and dependencyManagement entries for `com.aspose:*`, `com.jpedal:pdf2image`, and `org.artofsolving.jodconverter:jodconverter-core`.
- `util/pom.xml`: remove direct dependencies on Aspose, JPedal, and JODConverter.
- `platform/pom.xml`: remove direct dependency on JODConverter.
- `util/src/main/java/com/riversoft/util/OfficeUtils.java`: keep method names used by WeChat helpers, but make all conversion methods throw a clear unsupported exception without third-party imports.
- `platform/src/main/java/com/riversoft/platform/office/ConverterHelper.java`: replace JODConverter adapter with no-op implementation that always reports conversion disabled.
- `platform/src/main/java/com/riversoft/core/exception/ExceptionType.java`: remove `PdfException` import and class binding.
- `util/src/test/java/com/riversoft/util/OfficeUtilsTest.java`: replace ignored conversion tests with compile-safe disabled-behavior tests.
- `platform/src/test/java/com/riversoft/platform/office/pdf/ConverterHelperTest.java`: replace ignored converter tests with compile-safe disabled-behavior tests.
- `platform/src/test/java/com/riversoft/platform/office/pdf/LibreOfficeStarter.java`: remove JODConverter dependency by replacing it with a message-only stub or deleting it.
- `docs/v1.1.0/pretask-office-dependency-assessment.md`: record final decision.
- `docs/v1.1.0/spec-historical-dependencies.md`: state that the three libraries are excluded from v1.1.0 artifact distribution.
- `docs/maintenance.md`: document that Office/PDF conversion is intentionally disabled in v1.1.0 default distribution.

### Task 1: Remove Maven Dependency Declarations

**Files:**
- Modify: `parent/pom.xml`
- Modify: `util/pom.xml`
- Modify: `platform/pom.xml`

- [ ] **Step 1: Delete version properties from `parent/pom.xml`**

Remove these exact property lines from `<properties>`:

```xml
<aspose-slides.version>15.9.0</aspose-slides.version>
<aspose-words.version>14.12.0</aspose-words.version>
<aspose-cells.version>8.6.3</aspose-cells.version>
<jpedal.version>4.92b23</jpedal.version>
<jodconverter.version>3.1.1</jodconverter.version>
```

- [ ] **Step 2: Delete dependencyManagement entries from `parent/pom.xml`**

Remove these dependency blocks:

```xml
<dependency>
  <groupId>com.aspose</groupId>
  <artifactId>aspose-slides</artifactId>
  <version>${aspose-slides.version}</version>
</dependency>
<dependency>
  <groupId>com.aspose</groupId>
  <artifactId>aspose-words</artifactId>
  <version>${aspose-words.version}</version>
</dependency>
<dependency>
  <groupId>com.aspose</groupId>
  <artifactId>aspose-cells</artifactId>
  <version>${aspose-cells.version}</version>
</dependency>
<dependency>
  <groupId>com.jpedal</groupId>
  <artifactId>pdf2image</artifactId>
  <version>${jpedal.version}</version>
</dependency>
<dependency>
  <groupId>org.artofsolving.jodconverter</groupId>
  <artifactId>jodconverter-core</artifactId>
  <version>${jodconverter.version}</version>
</dependency>
```

- [ ] **Step 3: Delete direct dependencies from `util/pom.xml`**

Remove:

```xml
<dependency>
  <groupId>org.artofsolving.jodconverter</groupId>
  <artifactId>jodconverter-core</artifactId>
</dependency>
<dependency>
  <groupId>com.aspose</groupId>
  <artifactId>aspose-slides</artifactId>
</dependency>
<dependency>
  <groupId>com.aspose</groupId>
  <artifactId>aspose-words</artifactId>
</dependency>
<dependency>
  <groupId>com.aspose</groupId>
  <artifactId>aspose-cells</artifactId>
</dependency>
<dependency>
  <groupId>com.jpedal</groupId>
  <artifactId>pdf2image</artifactId>
</dependency>
```

- [ ] **Step 4: Delete direct dependency from `platform/pom.xml`**

Remove:

```xml
<dependency>
  <groupId>org.artofsolving.jodconverter</groupId>
  <artifactId>jodconverter-core</artifactId>
</dependency>
```

- [ ] **Step 5: Verify no POM references remain**

Run:

```bash
rg -n "aspose|jpedal|jodconverter|artofsolving" --glob 'pom.xml' --glob '*/pom.xml' --glob '*/*/pom.xml' .
```

Expected: no output.

### Task 2: Replace Conversion Implementations With Disabled Behavior

**Files:**
- Modify: `util/src/main/java/com/riversoft/util/OfficeUtils.java`
- Modify: `platform/src/main/java/com/riversoft/platform/office/ConverterHelper.java`
- Modify: `platform/src/main/java/com/riversoft/core/exception/ExceptionType.java`

- [ ] **Step 1: Replace `OfficeUtils.java`**

Replace the file with this implementation:

```java
package com.riversoft.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Office/PDF conversion helpers are disabled in bpmt-lite default distribution.
 */
public class OfficeUtils {

    private static final String UNSUPPORTED_MESSAGE = "bpmt-lite 默认发行版本不支持 Office/PDF 转换。";

    private OfficeUtils() {
    }

    public static List<File> ppt2jpgs(File ppt) throws IOException {
        throw unsupported();
    }

    public static List<File> ppt2jpgs(File ppt, float scale) throws IOException {
        throw unsupported();
    }

    public static List<File> ppt2jpgs(InputStream ppt, String pptName) throws IOException {
        throw unsupported();
    }

    public static List<File> ppt2jpgs(InputStream ppt, String pptName, float scale) throws IOException {
        throw unsupported();
    }

    public static File ppt2pdf(File ppt) throws IOException {
        throw unsupported();
    }

    public static File ppt2pdf(InputStream ppt, String pptName) throws IOException {
        throw unsupported();
    }

    public static File ppt2html(File ppt) throws IOException {
        throw unsupported();
    }

    public static File ppt2html(InputStream ppt, String pptName) throws IOException {
        throw unsupported();
    }

    public static File word2pdf(File word) throws Exception {
        throw unsupported();
    }

    public static File word2pdf(InputStream word, String wordName) throws Exception {
        throw unsupported();
    }

    public static File word2html(File word) throws Exception {
        throw unsupported();
    }

    public static File word2html(File word, boolean imageBase64) throws Exception {
        throw unsupported();
    }

    public static File word2html(InputStream word, String wordName) throws Exception {
        throw unsupported();
    }

    public static File word2html(InputStream word, String wordName, boolean imageBase64) throws Exception {
        throw unsupported();
    }

    public static File excel2pdf(File excel) throws Exception {
        throw unsupported();
    }

    public static File excel2pdf(InputStream excel, String excelName) throws Exception {
        throw unsupported();
    }

    public static List<File> excel2jpgs(File excel) throws Exception {
        throw unsupported();
    }

    public static List<File> excel2jpgs(InputStream excel, String excelName) throws Exception {
        throw unsupported();
    }

    public static List<File> excel2svgs(File excel) throws Exception {
        throw unsupported();
    }

    public static List<File> excel2svgs(InputStream excel, String excelName) throws Exception {
        throw unsupported();
    }

    public static File excel2html(File excel) throws Exception {
        throw unsupported();
    }

    public static File excel2html(InputStream excel, String excelName) throws Exception {
        throw unsupported();
    }

    public static List<File> pdf2jpgs(File pdf) throws IOException {
        throw unsupported();
    }

    public static List<File> pdf2jpgs(File pdf, float scaling) throws IOException {
        throw unsupported();
    }

    public static List<File> pdf2jpgs(InputStream pdf, String pdfName) throws IOException {
        throw unsupported();
    }

    public static List<File> pdf2jpgs(InputStream pdf, String pdfName, float scaling) throws IOException {
        throw unsupported();
    }

    private static IOException unsupported() {
        return new IOException(UNSUPPORTED_MESSAGE);
    }
}
```

- [ ] **Step 2: Replace `ConverterHelper.java`**

Replace the file with this implementation:

```java
package com.riversoft.platform.office;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Office conversion is disabled in bpmt-lite default distribution.
 */
public class ConverterHelper {
    private static final Logger logger = LoggerFactory.getLogger(ConverterHelper.class);

    private ConverterHelper() {
    }

    public static boolean touch() {
        return false;
    }

    public static void reset() {
        logger.info("Office conversion is disabled in bpmt-lite default distribution.");
    }

    public static boolean convert(InputStream in, String inputPixel, OutputStream out, String outPixel) {
        logger.warn("Office conversion is disabled: {} -> {}", inputPixel, outPixel);
        return false;
    }

    public static boolean convert(File in, File out) {
        logger.warn("Office conversion is disabled: {} -> {}", in, out);
        return false;
    }
}
```

- [ ] **Step 3: Remove JPedal import from `ExceptionType.java`**

Delete:

```java
import org.jpedal.exception.PdfException;
```

Change:

```java
PDF(1200, "PDF文件处理异常", PdfException.class),
```

to:

```java
PDF(1200, "PDF文件处理异常"),
```

- [ ] **Step 4: Verify no production imports remain**

Run:

```bash
rg -n "com\\.aspose|org\\.jpedal|org\\.artofsolving|OfficeDocumentConverter|PdfDecoder" util/src/main/java platform/src/main/java
```

Expected: no output.

### Task 3: Fix Test Sources

**Files:**
- Modify: `util/src/test/java/com/riversoft/util/OfficeUtilsTest.java`
- Modify: `platform/src/test/java/com/riversoft/platform/office/pdf/ConverterHelperTest.java`
- Modify: `platform/src/test/java/com/riversoft/platform/office/pdf/LibreOfficeStarter.java`

- [ ] **Step 1: Replace `OfficeUtilsTest.java`**

Use compile-safe tests for disabled behavior:

```java
package com.riversoft.util;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class OfficeUtilsTest {

    @Test
    public void testOfficeConversionDisabled() {
        try {
            OfficeUtils.ppt2jpgs(new File("sample.ppt"));
            Assert.fail("Office conversion should be disabled.");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("不支持 Office/PDF 转换"));
        }
    }

    @Test
    public void testPdfConversionDisabled() {
        try {
            OfficeUtils.pdf2jpgs(new File("sample.pdf"));
            Assert.fail("PDF conversion should be disabled.");
        } catch (IOException e) {
            Assert.assertTrue(e.getMessage().contains("不支持 Office/PDF 转换"));
        }
    }
}
```

- [ ] **Step 2: Replace `ConverterHelperTest.java`**

Use compile-safe tests for disabled behavior:

```java
package com.riversoft.platform.office.pdf;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.Assert;
import org.junit.Test;

import com.riversoft.platform.office.ConverterHelper;

public class ConverterHelperTest {

    @Test
    public void testTouchReturnsFalseWhenOfficeConversionDisabled() {
        Assert.assertFalse(ConverterHelper.touch());
    }

    @Test
    public void testStreamConvertReturnsFalseWhenOfficeConversionDisabled() {
        Assert.assertFalse(ConverterHelper.convert(new ByteArrayInputStream(new byte[0]), "doc", new ByteArrayOutputStream(), "pdf"));
    }

    @Test
    public void testFileConvertReturnsFalseWhenOfficeConversionDisabled() {
        Assert.assertFalse(ConverterHelper.convert(new File("sample.doc"), new File("sample.pdf")));
    }
}
```

- [ ] **Step 3: Replace `LibreOfficeStarter.java`**

Use a no-dependency stub:

```java
package com.riversoft.platform.office.pdf;

public class LibreOfficeStarter {
    public static void main(String[] args) {
        System.out.println("Office conversion is disabled in bpmt-lite default distribution.");
    }
}
```

- [ ] **Step 4: Verify no test imports remain**

Run:

```bash
rg -n "com\\.aspose|org\\.jpedal|org\\.artofsolving|OfficeDocumentConverter|PdfDecoder" util/src/test platform/src/test
```

Expected: no output.

### Task 4: Document the Cut

**Files:**
- Modify: `docs/maintenance.md`
- Modify: `docs/v1.1.0/pretask-office-dependency-assessment.md`
- Modify: `docs/v1.1.0/spec-historical-dependencies.md`

- [ ] **Step 1: Add maintenance note**

In `docs/maintenance.md`, add a section after “数据和目录”:

```markdown
## v1.1.0 Office/PDF 转换裁剪

`v1.1.0` 默认发行不再包含 Aspose、JPedal 和 JODConverter。

影响范围：

- 不支持微信/企业微信文件自动转图文素材。
- 不支持非 PDF Office 附件在线转 PDF 预览。
- 不支持依赖 Office 服务的 PDF 导出。
- PDF 文件本身的普通上传、下载和直接预览不受影响。

默认 Docker 配置继续保持 `office.flag=false` 和 `office.prepare=false`。后续如果要恢复 Office/PDF 转换能力，应作为单独版本目标重新设计依赖来源、许可边界和运行服务。
```

- [ ] **Step 2: Verify docs mention final exclusion**

Run:

```bash
rg -n "Aspose|JPedal|JODConverter|office.flag=false" docs/v1.1.0 docs/maintenance.md
```

Expected: output includes the final exclusion decision and disabled default behavior.

### Task 5: Verify Build and Dependency Cut

**Files:**
- No direct edits.

- [ ] **Step 1: Run reference compile**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -s settings.local.xml -DskipTests compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Run test compile**

Run:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-1.8.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
mvn -s settings.local.xml -DskipTests test-compile
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Run repository hygiene script**

Run:

```bash
scripts/verify-repo.sh
```

Expected: `OK: repository hygiene checks passed`.

- [ ] **Step 4: Re-run public-only dependency discovery**

Use a temporary empty Maven repository and public-only settings, then compile.

Expected: if dependency resolution fails, failure list no longer includes:

```text
com.aspose:aspose-slides
com.aspose:aspose-words
com.aspose:aspose-cells
com.jpedal:pdf2image
org.artofsolving.jodconverter:jodconverter-core
```

### Task 6: Commit

**Files:**
- All files changed by this plan.

- [ ] **Step 1: Review diff**

Run:

```bash
git diff --stat
git diff --check
```

Expected: no whitespace errors.

- [ ] **Step 2: Commit**

Run:

```bash
git add parent/pom.xml util/pom.xml platform/pom.xml util/src/main/java/com/riversoft/util/OfficeUtils.java platform/src/main/java/com/riversoft/platform/office/ConverterHelper.java platform/src/main/java/com/riversoft/core/exception/ExceptionType.java util/src/test/java/com/riversoft/util/OfficeUtilsTest.java platform/src/test/java/com/riversoft/platform/office/pdf/ConverterHelperTest.java platform/src/test/java/com/riversoft/platform/office/pdf/LibreOfficeStarter.java docs/maintenance.md docs/v1.1.0 docs/superpowers/plans/2026-04-26-office-dependency-removal.md
git commit -m "裁剪 Office/PDF 转换历史依赖"
```

Expected: commit succeeds on branch `codex/v1.1-office-dependency-removal`.
