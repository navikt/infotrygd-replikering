# infotrygd-replikering

Overvåker replikering av Infotrygd-tabeller fra on-prem Oracle og eksponerer forsinkelse som Prometheus-metrikker.

## Kom i gang

**Forutsetninger:** Java 25, Maven, Docker (for integrasjonstester med Oracle)

```bash
git clone https://github.com/navikt/infotrygd-replikering
cd infotrygd-replikering
mvn verify              # Bygg og kjør tester
```

Lokal kjøring med Docker Compose (inkluderer Oracle, mock-OIDC og Wonderwall):

```bash
docker compose build
docker compose up
```

Appen er da tilgjengelig på `http://localhost:8080`, med innlogging via `http://localhost:3000`.

## Avhengighetsrapportering

Workflowen «Submit dependency graph» sender Maven-avhengighetsgrafen til GitHub
for Dependabot. Den bruker Java 25 og kjører ved POM- eller workflow-endringer
på `master`, eller manuelt fra GitHub Actions.

Etter merge må vellykket innsending bekrefte at faktiske avhengighetsversjoner
og transitive avhengigheter er rapportert. SBOM-generering og deploy i Nais er
uendret.

OpenTelemetry-bibliotekene styres samlet av BOM-versjon `1.67.0`, som retter
CVE-2026-118259. Dette endrer ikke versjonen på Nais-agenten.

## Tech stack

- **Språk:** Kotlin, Java 25
- **Rammeverk:** Spring Boot 4.1.0
- **Database:** Oracle (on-prem, via Vault-credentials)
- **Auth:** Azure AD (token-validation-spring)
- **Metrikker:** Micrometer → Prometheus
- **Plattform:** Nais (dev-fss / prod-fss)

## Arkitektur

```
┌─────────────────────────────────────────────────────┐
│  ReplikeringsstatusService (@Scheduled, 30 sek)     │
│                                                     │
│  1. Les tabeller fra `replikering_status`           │
│     (kun de med READY = true)                       │
│  2. For hver tabell: SELECT max(OPPDATERT)          │
│  3. Beregn forsinkelse = current_timestamp - max    │
│  4. Oppdater Prometheus gauge per tabell            │
└─────────────────────────────────────────────────────┘
         │
         ▼
  Prometheus scraper /actuator/prometheus
         │
         ▼
  Grafana dashboard → varsling ved høy forsinkelse
```

**Metrikknavn:** `infotrygd_replikering_tabellforsinkelse` (millisekunder)
**Tags:** `tabell=<schema>.<tabellnavn>`

Ved manglende måling (lesefeil, tom tabell eller deaktivert overvåking) rapporterer en tidligere registrert gauge `NaN`, ikke null forsinkelse. Gauges rapporterer også `NaN` mens en ny oppdatering pågår eller hvis oppslaget av overvåkede tabeller feiler. `infotrygd_replikering_oppdatering_vellykket` har verdien `1` når alle overvåkede tabeller ble lest ved siste oppdatering, og `0` før første vellykkede oppdatering, under kjøring eller etter feil.

### Krav til overvåkede tabeller

- Må ha en kolonne `OPPDATERT` (timestamp)
- Må ha indeks på `OPPDATERT`
- Må være registrert i tabellen `replikering_status` med `READY = true`
- Sett `READY = false` dersom indeksen er utilgjengelig

### Datamodell

Tabellen `replikering_status`:

| Kolonne | Type | Beskrivelse |
|---------|------|-------------|
| `ID_REP_STATUS` | NUMBER | Primærnøkkel (auto-generert) |
| `SCHEMA_NAME` | VARCHAR | Oracle-schema (f.eks. `INFOTRYGD_P`) |
| `TABLE_NAME` | VARCHAR | Tabellnavn |
| `READY` | NUMBER(1) | 1 = aktiv overvåking, 0 = deaktivert |

## API

| Metode | Sti | Beskrivelse | Auth |
|--------|-----|-------------|------|
| GET | `/actuator/health` | Health check (liveness + readiness) | Åpen |
| GET | `/actuator/prometheus` | Prometheus-metrikker | Åpen |
| GET | `/swagger-ui.html` | Swagger UI | Åpen |

## Konfigurasjon

| Variabel | Beskrivelse | Påkrevd |
|----------|-------------|---------|
| `APP_DATASOURCE_URL` | Oracle JDBC URL | Ja (fra Vault) |
| `APP_DATASOURCE_USERNAME` | DB-bruker | Ja (fra Vault) |
| `APP_DATASOURCE_PASSWORD` | DB-passord | Ja (fra Vault) |
| `APP_DEFAULT_SCHEMA` | Oracle-schema | Ja |
| `APP_DDL_AUTO` | Hibernate DDL-modus | Nei (default `validate`, typisk `none` i prod) |
| `APP_GRUPPE_ADMIN` | Azure AD-gruppe for tilgang | Ja |
| `AZURE_APP_WELL_KNOWN_URL` | OIDC discovery-URL | Ja (injisert av Nais) |
| `AZURE_APP_CLIENT_ID` | App-registrering i Azure | Ja (injisert av Nais) |

## Deploy

Deployes til Nais via GitHub Actions ved push til `master`.

- **Dev:** Automatisk ved push til `master`
- **Prod:** Automatisk ved push til `master` (med mindre commit inneholder `ci skip`)
- **Cluster:** dev-fss, prod-fss
- **Manifester:** `nais/app/backend.yaml` med variabler fra `nais/dev.json` / `nais/prod.json`

## Observabilitet

- **Metrikker:** Prometheus gauge `infotrygd_replikering_tabellforsinkelse` (ms per tabell)
- **Logger:** Elastic + Loki (konfigurert i Nais-manifest)
- **Tracing:** OpenTelemetry auto-instrumentering (Java agent)
- **Ingress:** `https://infotrygd-replikering.intern.nav.no` (prod)

### Alerting

Konfigurert i `nais/alert.yml`. Varsler på Slack-kanalen `#infotrygd_replikering_alerts` dersom en tabell har mer enn **1 times forsinkelse**.

Alertet trigges kun **hverdager 08–17 CEST** (06–15 UTC) for å unngå falske alarmer i helger og netter, da replikering normalt er forsinket utenfor arbeidstid.

> **Merk:** Helligdager som faller på ukedager vil fortsatt trigge alertet — dette kan ikke filtreres i PromQL.

## Lokal utvikling

Profilen `noauth` deaktiverer Azure AD-validering for lokal testing.

Start `DevMain.kt` med OpenTelemetry-agenten:

```bash
make backend-demo
```

Make laster ned en versjonslåst agent til gitignored
`target/otel/opentelemetry-javaagent.jar` og starter en egen JVM med `-javaagent`.
Dette bruker samme instrumenteringsmekanisme som Nais, men versjonen er ikke
automatisk synkronisert med Nais. Make setter `OTEL_TRACES_EXPORTER=none`,
`OTEL_METRICS_EXPORTER=none` og `OTEL_LOGS_EXPORTER=none`, så ingen telemetri eksporteres.
Ingen egen Spring-konfigurasjon, Maven-profil eller endring i produksjonsbygget er lagt til.

For IntelliJ: kjør `make otel-agent`, og bruk repo-roten som **Working directory**
for `DevMain.kt`. Legg til i **VM options**:

```text
-javaagent:target/otel/opentelemetry-javaagent.jar
```

Sett disse **Environment variables** i samme run-konfigurasjon:

```text
OTEL_SERVICE_NAME=infotrygd-replikering;OTEL_TRACES_EXPORTER=none;OTEL_METRICS_EXPORTER=none;OTEL_LOGS_EXPORTER=none
```

Kjør `make otel-agent` igjen etter `mvn clean`.

Eller bruk Docker Compose som beskrevet under «Kom i gang».

## Team

- **Team:** #infotrygd
- **Namespace:** infotrygd
