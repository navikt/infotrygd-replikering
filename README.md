# infotrygd-replikering

Overvåker replikering av Infotrygd-tabeller fra on-prem Oracle og eksponerer forsinkelse som Prometheus-metrikker.

Kotlin og Spring Boot, med Oracle og Micrometer. Kjører på Nais.

## Kom i gang

**Forutsetninger:** Java 25, Maven, Docker, Make og jq.

```bash
git clone https://github.com/navikt/infotrygd-replikering
cd infotrygd-replikering
mvn verify
```

Start mock-OIDC i én terminal:

```bash
docker compose up mock-oidc
```

Start appen i en annen terminal fra repo-roten:

```bash
APP_DEFAULT_SCHEMA=test make backend-demo
```

Dette starter [DevMain.kt](src/test/kotlin/DevMain.kt) med profilene `dev`, `demoData` og `debug-sql`. Testcontainers starter Oracle med testdata. Appen kjører på `http://localhost:8080`, med token-validering mot lokal mock-OIDC på port 8102.

Make laster ned en versjonslåst OpenTelemetry-agent til `target/otel/opentelemetry-javaagent.jar` og slår av eksport av traces, metrikker og logger. Agentversjonen er ikke automatisk synkronisert med Nais.

### IntelliJ

Kjør `make otel-agent`. Start `DevMain.kt` med repo-roten som **Working directory** og følgende **VM options**:

```text
-javaagent:target/otel/opentelemetry-javaagent.jar
```

Sett **Environment variables**:

```text
OTEL_SERVICE_NAME=infotrygd-replikering;OTEL_TRACES_EXPORTER=none;OTEL_METRICS_EXPORTER=none;OTEL_LOGS_EXPORTER=none
```

Mock-OIDC må også kjøre. Kjør `make otel-agent` igjen etter `mvn clean`.

### Test runtime-imaget

Docker-imaget bruker Navs Chainguard JRE for Java 25. Lokalt kreves tilgang til Navs Chainguard-registry.

```bash
mvn verify
docker build --pull -t infotrygd-replikering:smoke .
mvn -Pcontainer-smoke -Dapp.image=infotrygd-replikering:smoke failsafe:integration-test failsafe:verify
```

Smoketesten starter det ferdige appimaget, en isolert Oracle-testdatabase og lokal mock-OIDC. Den bruker syntetiske data og midlertidige credentials via filer, og kontrollerer readiness og at replikeringsmetrikken endres etter en databaseoppdatering. Appcontaineren når mock-OIDC via Testcontainers, uten Nais-proxyen. Testen bruker åpne health- og metrikkendepunkter uten å deaktivere token-validering. Testressursene ryddes opp etter kjøring.

Vanlig `mvn verify` kompilerer smoketesten, men starter ikke appimaget. Kjør derfor container-steget etter imagebygg. Ved endring av testkoden må `mvn verify` kjøres på nytt før container-steget.

For lokal OS-skanning kreves også Trivy:

```bash
trivy image --scanners vuln --pkg-types os --list-all-pkgs --format json --output target/image-scan.json infotrygd-replikering:smoke
bash scripts/check-image-scan.sh target/image-scan.json
```

## Overvåking

Appen leser aktive tabeller fra `replikering_status`, finner `max(OPPDATERT)` og beregner forsinkelsen mot databasens `current_timestamp`. Det er 30 sekunders pause mellom oppdateringene.

En overvåket tabell må ha en indeksert `OPPDATERT`-kolonne (timestamp) og være registrert med `READY = true`. Sett `READY = false` hvis indeksen er utilgjengelig.

| Metrikk | Betydning |
|---------|-----------|
| `infotrygd_replikering_tabellforsinkelse` | Forsinkelse i millisekunder, med taggen `tabell=<schema>.<tabellnavn>` |
| `infotrygd_replikering_oppdatering_vellykket` | `1` når alle aktive tabeller ble lest ved siste oppdatering; ellers `0`, også før første vellykkede oppdatering og under kjøring |

Tidligere registrerte forsinkelsesgauges rapporterer `NaN`, ikke null forsinkelse, ved lesefeil, tom tabell eller deaktivert overvåking. De rapporterer også `NaN` under oppdatering og hvis oppslaget av overvåkede tabeller feiler.

## API og tilgang

| Metode | Sti | Beskrivelse |
|--------|-----|-------------|
| GET | `/actuator/health` | Health check, med probes på `/liveness` og `/readiness` under samme sti |
| GET | `/actuator/prometheus` | Prometheus-metrikker |
| GET | `/swagger-ui.html` | Swagger UI |

Disse endpointene er åpne. `/tables` er ikke implementert og svarer med feil.

Appen bruker Entra ID via `token-validation-spring`. Nais begrenser brukertilgang til den konfigurerte admin-gruppen, se [manifestet](nais/app/backend.yaml). Profilen `noauth` deaktiverer token-validering og er kun for lokal utvikling/test.

## Drift

[GitHub Actions](.github/workflows/main.yml) bygger og tester ved push til `master`. Det ferdige imaget må bestå container-smoketesten og OS-skanningen for `CVE-2026-115506` før deployment. Skanningen må også ha gjenkjent OS og pakkeinnhold. Skannerresultat og imagereferanse lagres som artifactet `image-verification`.

Dev-steget bruker Nais sin venting på en fullført rollout med alle ønskede instanser ready. Kontrollen går via Nais API, ikke direkte mot det private Kubernetes-API-et. Prod-deployment bruker samme testede imagereferanse og kjører bare hvis de foregående stegene lykkes. Prod-steget utelates fortsatt hvis commit-meldingen inneholder `ci skip`. [Nais-manifestet](nais/app/backend.yaml) bruker variabler fra [dev](nais/dev.json) og [prod](nais/prod.json).

Readiness-gaten er ikke en kontroll av alle metrikker i dev. Kontroller replikeringsmetrikker, Vault-tilgang og Java-instrumentering i dev og prod som del av driftsoppfølgingen. Ved rollback til det gamle imaget kan sårbarheten bli gjeninnført.

Logger sendes til Elastic og Loki. Nais aktiverer OpenTelemetry auto-instrumentering med Java-agent. Prometheus-metrikkene brukes i Grafana for visualisering og varsling.

[Alertet](nais/alert.yml) varsler `#infotrygd_replikering_alerts` når en tabell har over én times forsinkelse i 15 minutter. Det evaluerer hverdager kl. 06–15 UTC (08–17 sommertid, 07–16 vintertid i Norge). Helligdager på ukedager er ikke unntatt.

## Team

Team og namespace: `infotrygd`.

## Lisens

[MIT](LISENSIERING.md)
