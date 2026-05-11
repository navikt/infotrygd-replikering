# AGENTS.md — infotrygd-replikering

`infotrygd-replikering` er et monitoreringsverktøy for replikering av Infotrygd-tabeller fra on-prem Oracle til andre systemer. Tjenesten sjekker hvert 30. sekund hvor gammel den nyeste raden i hver overvåket tabell er (basert på kolonnen `OPPDATERT`), og eksponerer forsinkelsen som en Prometheus-metrikk (`infotrygd_replikering_tabellforsinkelse`, i millisekunder). Grafana brukes for visualisering og varsling.

Tabellene som skal overvåkes konfigureres i databasetabellen `replikering_status`. En tabell må ha en indeksert `OPPDATERT`-kolonne og `READY = true` for å bli inkludert.

Tjenesten brukes internt av team som er avhengig av at Infotrygd-data er oppdatert — primært for å oppdage og varsle om replikeringsforsinkelser.

## Build & Test Commands

```bash
mvn test        # Run tests
mvn verify      # Build and test
mvn package     # Package without running tests: mvn package -DskipTests
```

## Project Structure

```text
mock-oidc/
nais/
src/
```

## Code Style

### Minimal Editing

When fixing a bug or implementing a feature, change only what is necessary.
Do not rename variables, restructure working code, or refactor beyond the task at hand.
Keep diffs small and focused so they are easy to review.

## Git Workflow

<!-- TODO: Document your branching and merge strategy -->

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
