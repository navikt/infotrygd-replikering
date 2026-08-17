# AGENTS.md — infotrygd-replikering

`infotrygd-replikering` overvåker replikering av Infotrygd-tabeller og eksponerer forsinkelse som Prometheus-metrikker.

## Tech Stack

- Java 25, Kotlin 2.4.0, Spring Boot 4.1.0, Maven
- Oracle DB (OJDBC) med HikariCP (pool-size 2)
- Vault for database-credentials (on-prem)
- Azure AD (token-validation-spring) for autentisering
- Prometheus / Micrometer for metrikker
- Springdoc/OpenAPI for Swagger

## Build & Test Commands

```bash
mvn test        # Run tests
mvn verify      # Build and test
mvn package     # Package without running tests: mvn package -DskipTests
```

## Project Structure

```text
mock-oidc/          # Lokal mock-OIDC-server for utvikling uten Azure AD
nais/               # Nais-manifest (app, dev, prod, alerts)
src/
  main/kotlin/no/nav/historisk/innsyn/
    config/         # DatasourceConfig, SecurityConfiguration
    exception/      # Feilkoder og exception-typer
    integration/    # REST-klienter og interceptors
    model/          # Domenemodeller og value objects (TabellRef, Ready, osv.)
    repository/     # ReplikeringsstatusRepository (JDBC)
    rest/           # Controllers, ExceptionHandler, LogFilter, SwaggerInfo
    Application.kt  # Spring Boot entry point
    Profiles.kt     # Spring-profiler (noauth, osv.)
  test/             # Integrasjons- og enhetstester
```

## Environment Variables

| Variabel | Beskrivelse |
|----------|-------------|
| `APP_DATASOURCE_URL` | Oracle JDBC URL |
| `APP_DATASOURCE_USERNAME` | DB-bruker (settes av Vault) |
| `APP_DATASOURCE_PASSWORD` | DB-passord (settes av Vault) |
| `APP_DEFAULT_SCHEMA` | Standardschema for HikariCP og Hibernate |
| `APP_GRUPPE_ADMIN` | Azure AD-gruppe for admin-tilgang |
| `APP_DDL_AUTO` | Hibernate DDL-strategi (default: `validate`) |
| `NAIS_APP_NAME` | Applikasjonsnavn (default: `infotrygd-replikering`) |

## Lokal utvikling

Start med `noauth`-profilen for å deaktivere Azure AD-sjekk:

```bash
SPRING_PROFILES_ACTIVE=noauth mvn spring-boot:run
```

Mock-OIDC (for testing med autentisering):

```bash
cd mock-oidc && docker build -t mock-oidc . && docker run -p 8090:8090 mock-oidc
```

## Code Style

### Minimal Editing

When fixing a bug or implementing a feature, change only what is necessary.
Do not rename variables, restructure working code, or refactor beyond the task at hand.
Keep diffs small and focused so they are easy to review.

## Git Workflow

Create a short-lived branch for each change from the main branch.
Keep the branch focused on a single task and update it with the latest main branch before merging.
Merge changes through a pull request after tests pass and required review is complete.

## Boundaries

### ✅ Always

- Run tests after changes
- Follow existing code patterns in the project
- Preserve existing code structure — do not reorganize or refactor beyond the task
- Validate all external input

### ⚠️ Ask First

- Changing authentication mechanisms
- Adding new dependencies
- Modifying database schema

### 🚫 Never

- Commit secrets or credentials
- Skip input validation on external boundaries
