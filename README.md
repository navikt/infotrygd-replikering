# infotrygd-replikering

Overvåker replikering av Infotrygd-tabeller fra on-prem Oracle og eksponerer forsinkelse som Prometheus-metrikker.

## Kom i gang

**Forutsetninger:** Java 21, Maven, Docker (for integrasjonstester med Oracle)

```bash
git clone https://github.com/navikt/infotrygd-replikering
cd infotrygd-replikering
mvn verify              # Bygg og kjør tester
```

Lokal kjøring med Docker Compose (inkluderer Oracle, mock-OIDC og Wonderwall):

```bash
docker compose up
```

Appen er da tilgjengelig på `http://localhost:8080`, med innlogging via `http://localhost:3000`.

## Tech stack

- **Språk:** Kotlin, Java 21
- **Rammeverk:** Spring Boot 4.0
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
| GET | `/swagger-ui.html` | Swagger UI | Azure AD |

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

## Lokal utvikling

Profilen `noauth` deaktiverer Azure AD-validering for lokal testing.

Kjør `DevMain.kt` fra testmappen for å starte appen med mock-konfigurasjon:

```bash
mvn test-compile
# Kjør DevMain.kt fra IDE med profil "noauth"
```

Eller bruk Docker Compose som beskrevet under «Kom i gang».

## Team

- **Team:** #infotrygd
- **Namespace:** infotrygd
