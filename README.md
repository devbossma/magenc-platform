# Magenc Platform

Multi-tenant marketing agency orchestration SaaS.

**Stack:** Java 25 - Spring Boot 4.0.5 - Spring Modulith 2.0.5 - PostgreSQL 17 - Maven - Docker

---

## First-Time Setup (Windows + WSL2)

### 1. Install WSL2 + Ubuntu

In PowerShell as Administrator:

```powershell
wsl --install -d Ubuntu-24.04
```

Reboot, open Ubuntu, create your user.

### 2. Install Docker Desktop on Windows

Download from <https://www.docker.com/products/docker-desktop>. After install:

1. **Settings -> General**, ensure "Use the WSL 2 based engine" is checked.
2. **Settings -> Resources -> WSL Integration**, enable integration with your Ubuntu distro.
3. Restart Docker Desktop.

Verify from inside WSL2:

```bash
docker version
docker compose version
```

### 3. Install SDKMAN, Java 25, Maven (inside WSL2)

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

sdk install java 25.0.2-tem
sdk install maven

java --version
mvn --version
```

### 4. Place this project on the WSL2 filesystem

**CRITICAL:** put the project under your WSL2 home directory, NOT under `/mnt/c/`.
Cross-filesystem I/O is 10-50x slower and will make Maven and Spring Boot crawl.

```bash
mkdir -p ~/projects
cd ~/projects
# Extract the magenc-platform.zip here
unzip /mnt/c/Users/you/Downloads/magenc-platform.zip -d .
cd magenc-platform
```

### 5. Add the dev hostname to your hosts file

For tenant-subdomain routing to work locally, edit `C:\Windows\System32\drivers\etc\hosts` (run Notepad as Administrator) and add:

```text
127.0.0.1   magenc.local
127.0.0.1   acme.magenc.local
127.0.0.1   beta.magenc.local
```

### 6. Open the project in IntelliJ IDEA

Open IntelliJ on Windows. **File -> Open** and navigate to:

```
\\wsl$\Ubuntu-24.04\home\<your-user>\projects\magenc-platform
```

IntelliJ will detect it as a Maven project and import dependencies.

Set the project SDK: **File -> Project Structure -> Project -> SDK**, "Add SDK -> Add JDK from disk", point at `~/.sdkman/candidates/java/25.0.2-tem`.

### 7. Run the app

In IntelliJ, find `MagencPlatformApplication` and click run. On first run:

1. Spring Boot Docker Compose support starts Postgres, Redis, MinIO, MailHog, Jaeger.
2. Flyway runs `V1__baseline_admin_schema.sql` against the `admin` schema.
3. App listens on port 8080.

### 8. Verify it works

```bash
curl http://localhost:8080/v1/health

curl -H "Host: acme.magenc.local" http://localhost:8080/v1/health/tenant
```

The tenant call returns:

```json
{
  "status": "ok",
  "tenant": "acme",
  "timestamp": "2026-04-06T..."
}
```

---

## Local URLs

| Service       | URL                                              | Credentials               |
| ------------- | ------------------------------------------------ | ------------------------- |
| App           | <http://localhost:8080>                          | -                         |
| Swagger UI    | <http://localhost:8080/swagger-ui/index.html>    | -                         |
| Postgres      | `localhost:5432`                                 | magenc / magenc_local_dev |
| Redis         | `localhost:6379`                                 | -                         |
| MinIO console | <http://localhost:9001>                          | magenc / magenc_local_dev |
| MailHog UI    | <http://localhost:8025>                          | -                         |
| Jaeger UI     | <http://localhost:16686>                         | -                         |

---

## Useful Commands

```bash
# Run all tests including module verification
mvn test

# Run only the architecture tests
mvn test -Dtest=ApplicationModulesTest

# Reset all local infrastructure (drops Postgres data, MinIO buckets)
docker compose down -v

# Watch logs for one service
docker compose logs -f postgres
```

---

## Module Boundaries

This is a modular monolith. Each top-level package under `com.magenc.platform` is a Spring Modulith module. The build will FAIL if a module reaches across a boundary it should not.

| Module          | Owns                                                           |
| --------------- | -------------------------------------------------------------- |
| `tenancy`       | Agency entity, subdomain resolution, schema/database routing   |
| `iam`           | Users, roles, permissions, JWT issuance                        |
| `clients`       | Agency clients, brand profiles, client types                   |
| `agents`        | Agent definitions, role catalog, skill registry                |
| `tasks`         | Task lifecycle, orchestrator, execution state                  |
| `conversations` | Message context, SSE streaming                                 |
| `llm`           | Provider abstraction, BYOK credential vault, token accounting  |
| `integrations`  | Meta, Google, Notion, Zoom OAuth and connectors                |
| `billing`       | Plans, usage metering, Stripe                                  |
| `audit`         | Hash-chained event log                                         |
| `compliance`    | GDPR export, erasure, PII inventory                            |
| `notifications` | Email, in-app, webhooks                                        |

To see the live module graph:

```bash
mvn test -Dtest=ApplicationModulesTest#generateDocumentation
# Then open target/spring-modulith-docs/*.puml in IntelliJ (PlantUML plugin)
```

---

## Troubleshooting

### Port already in use
Stop the conflicting service or change the port in `compose.yaml`.

### Docker compose did not start automatically
Verify Docker Desktop is running and WSL2 integration is enabled. Run `docker ps` from inside WSL2 to confirm.

### Hostname not resolving
Check `C:\Windows\System32\drivers\etc\hosts` was edited as Administrator. Flush DNS: `ipconfig /flushdns` in PowerShell.

### IntelliJ cannot find Java 25
Project Structure -> Platform Settings -> SDKs -> Add JDK from disk -> point at the SDKMAN-managed JDK inside WSL2 at `\\wsl$\Ubuntu-24.04\home\<user>\.sdkman\candidates\java\25.0.2-tem`.

### Slow builds
Confirm the project lives on the WSL2 filesystem (`~/projects/`), NOT on `/mnt/c/`. Run `pwd` from a WSL2 terminal to check.

### Flyway migration fails on first start
Drop the database volume and retry: `docker compose down -v && mvn spring-boot:run`.

---

## What is Next

- [ ] Wire `TenantProvisioningService` to a signup endpoint
- [ ] Add JWT issuance and validation in the `iam` module
- [ ] Create the `clients` and `agents` domain models
- [ ] Build the LLM provider abstraction with the BYOK credential vault
- [ ] Stand up the Next.js frontend in a sibling repo
