# bpmt-lite Design

## Purpose

Create `bpmt-lite` as a simplified, Docker-first distribution of the existing BPMT low-code platform.

BPMT means BPM + table. The core product is the existing custom workflow and dynamic table web application. The migration must preserve current behavior and keep the legacy runtime stack: Java 8, Tomcat 7, Maven, and MariaDB. This project is a structure, packaging, configuration, and deployment simplification. It is not a feature rewrite and not a technology upgrade.

## Source Baseline

The source baseline is the current RiverSoft trunk at:

`/Users/wenzhewang/workspace/bpmt_project/riversoft/trunk`

Use the current trunk as a reference, but only carry over changes that are necessary and verified for Java 8 build stability or Docker runtime support. Do not blindly copy experimental local changes from the trunk working copy.

The local `bpmt-lite` repository corresponds to:

`https://github.com/wodenwang/bpmt-lite.git`

## Scope

In scope:

- Create a clean Git repository for `bpmt-lite`.
- Keep the minimum Maven multi-module structure needed to build and run the platform.
- Keep `platform` as the core web application.
- Build a Docker image with Tomcat 7 and Java 8.
- Deploy `platform` as Tomcat `ROOT`.
- Deploy the rich text editor as a separate `/ueditor` web application.
- Provide a user-facing `docker-compose.yml` that runs one web container and one MariaDB container.
- Support database initialization through a local `db/init/kyq.sql` file that is not committed to git.
- Expose legacy properties through docker compose configuration.
- Persist database data, database logs, attachments, downloads, platform logs, and Tomcat logs on the host.
- Document maintainer build requirements for private or old Maven dependencies.

Out of scope:

- Upgrade Java, Tomcat, Spring, Hibernate, Activiti, MariaDB driver, or the application framework.
- Convert the application to Spring Boot.
- Change BPMT product features.
- Add new product features.
- Commit `kyq.sql` to git.
- Commit Aspose, JPedal, `ueditor.war`, `patch-implementation`, or other private historical binaries to git.
- Migrate the old offline `package`, `tools`, or `support` distribution system.

## Repository Structure

The new repository should keep only the minimum source modules:

```text
bpmt-lite/
  pom.xml
  parent/
  util/
  magic/
    magic-api/
    magic-api-impl/
  dbtools/
  platform/
  docker/
  deploy/
  db/
    init/
  runtime/
  docs/
```

The exact Docker file layout can be adjusted during implementation, but the design intent is:

- Source modules remain recognizable and close to the original project.
- Runtime/deployment files are separated from Java source modules.
- The old `package`, `tools`, and `support` modules are not migrated.
- Generated build output, local runtime data, secrets, `kyq.sql`, and large binary dependencies are ignored by git.

## Maven Modules

Keep these modules:

- `parent`: dependency and plugin management.
- `util`: shared RiverSoft utilities.
- `magic/magic-api`: license/magic API contract.
- `magic/magic-api-impl`: implementation needed by platform runtime.
- `dbtools`: database utility dependency used by platform.
- `platform`: main WAR.

Remove from the new project:

- `package`: legacy offline zip distribution.
- `tools`: old operational tool bundle.
- `support`: archetype, generator, hbm2ddl, lightly build, and other auxiliary modules.

This keeps the existing module boundaries without carrying the old release machinery.

## Build Model

Maintainer build remains Maven-based and Java 8 based. The repository should commit `settings.example.xml`, and maintainers should copy it to an ignored `settings.local.xml` when they need host-specific Maven repository paths or credentials. A typical build command should be:

```bash
mvn -s settings.local.xml -pl platform -am -Pdocker-image verify
```

The project may also provide a convenience wrapper such as `make image` or `scripts/build-image.sh`, but the wrapper must call the Maven build rather than introduce a new build system.

The Docker image build process should:

1. Build `platform.war`.
2. Resolve `ueditor.war` from Maven.
3. Build a Tomcat 7 + Java 8 image.
4. Expand `platform.war` into `/usr/local/tomcat/webapps/ROOT`.
5. Expand `ueditor.war` into `/usr/local/tomcat/webapps/ueditor`.

## Historical Maven Dependencies

Some old dependencies are not expected to be available from public Maven repositories, including Aspose, JPedal, `ueditor.war`, and `patch-implementation`.

The repository should not commit these binaries. Instead:

- Keep Maven coordinates in POM files.
- Provide `settings.example.xml`.
- Document that maintainers need either the historical private Maven repository or a local Maven repository containing these artifacts.
- Treat long-term dependency archival and supply-chain cleanup as a separate future project.

End users should not need Maven or these private dependencies. They should consume the published Docker image.

## Published Runtime

The user-facing deployment should use a published image by default:

```yaml
image: ghcr.io/wodenwang/bpmt-lite:<version>
```

The normal user path is:

1. Create a deployment directory.
2. Place `docker-compose.yml` in it.
3. Optionally place the initial database dump at `db/init/kyq.sql`.
4. Run `docker compose up -d`.

The default runtime has two services:

- `web`: Tomcat 7 + Java 8 + `ROOT` + `/ueditor`.
- `mariadb`: MariaDB database named `kyq`.

## Database

The runtime database is MariaDB. The default schema name is `kyq`.

Expose simple database settings in compose:

```yaml
DB_HOST: mariadb
DB_PORT: 3306
DB_NAME: kyq
DB_USER: root
DB_PASSWORD: 123456
```

Do not expose a raw JDBC URL in the normal compose file. The container entrypoint builds the JDBC URL from the simple database settings:

```properties
jdbc.driverClassName=com.mysql.jdbc.Driver
jdbc.url=jdbc:mysql://mariadb:3306/kyq?useUnicode=true&characterEncoding=UTF-8
jdbc.username=root
jdbc.password=123456
hibernate.dialect=org.hibernate.dialect.MySQL5InnoDBDialect
```

The entrypoint also generates `db.properties` with the matching `db.def.*` values.

Database initialization:

- `db/init/kyq.sql` is the expected local path for the initial dump.
- `kyq.sql` is not committed to git.
- MariaDB imports it on first initialization through `/docker-entrypoint-initdb.d`.
- Documentation must explain that changing `kyq.sql` after MariaDB data already exists will not re-import automatically.

MariaDB should use settings suitable for the large legacy dump, including UTF-8 defaults, `lower_case_table_names=1`, large packet size, and increased read/write timeouts.

## Runtime Volumes

Use host-mounted volumes for all state that must survive container replacement:

```yaml
web:
  volumes:
    - ./runtime/attachment:/usr/local/tomcat/webapps/attachment
    - ./runtime/download:/usr/local/tomcat/webapps/download
    - ./runtime/platform-logs:/usr/local/tomcat/webapps/logs
    - ./runtime/tomcat-logs:/usr/local/tomcat/logs

mariadb:
  volumes:
    - ./db/data:/var/lib/mysql
    - ./db/init:/docker-entrypoint-initdb.d
    - ./db/logs:/var/log/mysql
```

The attachment and download paths intentionally follow the original package layout. In the old runtime, `platform`, `ueditor`, `attachment`, and related directories sit under the package deployment root. In the container, `/usr/local/tomcat/webapps` plays that same role:

- `/usr/local/tomcat/webapps/ROOT`: original `platform`.
- `/usr/local/tomcat/webapps/ueditor`: rich text editor webapp.
- `/usr/local/tomcat/webapps/attachment`: default attachment directory.
- `/usr/local/tomcat/webapps/download`: default download directory.
- `/usr/local/tomcat/webapps/logs`: BPMT platform logs.

Do not expose `file.attachment.path` by default. Let the application use its existing default path under the platform root. Only add an override if later validation proves it is needed.

MariaDB log configuration must ensure `./db/logs` receives useful database logs rather than being an unused mount.

## Properties Configuration

The compose file is the deployment configuration entrypoint. Users should not need to edit the Docker image or WAR files to configure the application.

The legacy application reads `*.properties` files from `WEB-INF/classes`. The Docker entrypoint bridges this by generating the same properties files at container startup from defaults plus environment variables.

Property files to generate include:

- `jdbc.properties`
- `db.properties`
- `page.properties`
- `safe.properties`
- `sms.properties`
- `wx.properties`
- `mail.properties`
- `office.properties`
- `log.properties`
- `hazelcast.properties`
- `activiti.properties`
- `redis.properties`
- `quartz.properties`

Defaults should come from the original package configuration where possible, with these intentional changes:

- Database defaults target MariaDB `kyq`, not H2.
- Sensitive fields default to empty values or harmless placeholders.
- External integrations default to disabled when possible.

Environment variable naming follows a mechanical rule:

- Convert the property key to uppercase.
- Replace dots with underscores.

Examples:

```text
page.title -> PAGE_TITLE
page.frame.new -> PAGE_FRAME_NEW
page.browser.msg -> PAGE_BROWSER_MSG
safe.sync.threads -> SAFE_SYNC_THREADS
sms.ali.enable -> SMS_ALI_ENABLE
wx.web.login.qrcode -> WX_WEB_LOGIN_QRCODE
mail.flow.subject.type -> MAIL_FLOW_SUBJECT_TYPE
office.flag -> OFFICE_FLAG
log.keepdays -> LOG_KEEPDAYS
hazelcast.group.name -> HAZELCAST_GROUP_NAME
activiti.font -> ACTIVITI_FONT
```

The user-facing compose file should list the common and important variables. `page.properties` should be represented nearly completely because those values are often deployment-facing branding and UI behavior.

Long values, such as mail notification templates, must support two paths:

- Environment variables for simple edits directly in compose.
- Optional mounted override files under `./config/overrides/*.properties` for values that are too long or awkward for YAML.

When an override file is present, it should take precedence over generated defaults for that same properties file. The default compose should stay readable while still showing how to mount overrides.

## Docker Image Boundary

The image can contain:

- Tomcat 7 + Java 8 runtime.
- Expanded `ROOT` webapp.
- Expanded `/ueditor` webapp.
- A generic entrypoint script.
- Default properties templates.

The image must not contain environment-specific configuration, database dumps, local secrets, or host-specific paths.

At startup, the entrypoint:

1. Ensures required runtime directories exist.
2. Generates `WEB-INF/classes/*.properties` from defaults and environment variables.
3. Starts Tomcat with `catalina.sh run`.

## Docker Compose Boundary

The default compose file is for end users and should reference the published image. It should not build Java source.

The repository may include a separate maintainer-only build compose file or scripts, but they must be clearly named so users do not confuse build-time and runtime flows.

The default exposed HTTP port can be configurable:

```yaml
BPMT_HTTP_PORT: 8080
```

The web service should wait for MariaDB health before startup.

## Git And Repository Hygiene

The local repository should use `main` and `origin=https://github.com/wodenwang/bpmt-lite.git`.

Do not commit:

- `kyq.sql`
- `db/data`
- `db/logs`
- `runtime`
- Maven `target`
- `.svn`
- private binary dependencies
- `settings.local.xml` or any local settings file with secrets
- cookies or temporary browser files

The first implementation pass should include `.gitignore` and clear README guidance.

## Validation

Validation must prove the migration preserved behavior while simplifying operations.

Repository validation:

- Confirm only the minimum modules and Docker/docs files are present.
- Confirm no old `package`, `tools`, `support`, `.svn`, `target`, `kyq.sql`, secrets, or private binaries are committed.
- Confirm git remote points to `https://github.com/wodenwang/bpmt-lite.git`.

Build validation:

- Build with Java 8.
- Use documented Maven settings.
- Produce `platform.war`.
- Build the Docker image.
- Confirm the image contains both `ROOT` and `ueditor`.

Container validation:

- Start MariaDB and web with compose.
- Verify MariaDB healthcheck.
- Verify optional `db/init/kyq.sql` import on first startup.
- Verify generated properties files exist and reflect compose settings.
- Verify Tomcat starts and deploys both applications.

Behavior validation:

- Access `http://127.0.0.1:<port>/`.
- Access the main login/home path used by the original platform.
- Access `/ueditor/`.
- Exercise a minimal upload path if credentials and initialized data allow it.
- Confirm attachment files persist under `./runtime/attachment`.
- Confirm downloads persist under `./runtime/download`.
- Confirm BPMT platform logs persist under `./runtime/platform-logs`.
- Confirm Tomcat logs persist under `./runtime/tomcat-logs`.
- Confirm MariaDB logs persist under `./db/logs`.

No validation step should add new product behavior.

## Open Follow-Up

The handling of old private dependencies such as Aspose, JPedal, `ueditor.war`, and `patch-implementation` is intentionally deferred. The current project documents the required Maven access path and keeps these binaries out of git. A later project can define a durable dependency archival and licensing strategy.
