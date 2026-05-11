# Copilot Instructions for infotrygd-replikering

<!-- This file captures repository-specific context.
     Nav-wide language and framework conventions are provided by installed Copilot instructions. -->

## Repository Overview

`infotrygd-replikering` er et monitoreringsverktøy for replikering av Infotrygd-tabeller fra on-prem Oracle til andre systemer. Tjenesten sjekker hvert 30. sekund hvor gammel den nyeste raden i hver overvåket tabell er (basert på kolonnen `OPPDATERT`), og eksponerer forsinkelsen som en Prometheus-metrikk (`infotrygd_replikering_tabellforsinkelse`, i millisekunder). Grafana brukes for visualisering og varsling.

Tabellene som skal overvåkes konfigureres i databasetabellen `replikering_status`. En tabell må ha en indeksert `OPPDATERT`-kolonne og `READY = true` for å bli inkludert.

Tjenesten brukes internt av team som er avhengig av at Infotrygd-data er oppdatert — primært for å oppdage og varsle om replikeringsforsinkelser.

**Nøkkelbeslutninger:**
- Oracle-database med credentials fra Vault (on-prem)
- Azure AD (client_credentials) for tjeneste-til-tjeneste-autentisering
- Prometheus + Grafana for observabilitet (ingen egne dashboards i repoet)

## Tech Stack

- Kotlin + Spring Boot (Maven)
- Oracle DB (OJDBC, JPA/Hibernate, NamedParameterJdbcTemplate)
- Vault for database-credentials
- Azure AD (token-validation-spring) for autentisering
- Prometheus (Micrometer) for metrikker
- Springdoc/OpenAPI for Swagger-dokumentasjon

## Key Patterns

**Replikeringsovervåking:**
`ReplikeringsstatusService` kjøres med `@Scheduled(fixedDelay = 1000 * 30)`. For hver tabell med `ready = true` i `replikering_status` hentes `max(OPPDATERT)` via raw SQL, og forsinkelsen beregnes som `current_timestamp - max(OPPDATERT)`.

**Prometheus-metrikker:**
Gauge-metrikker initialiseres én gang per tabell (lazy, ved første oppdatering) og leses fra en thread-safe `ReplikeringsstatistikkHolder`. Tagg: `tabell=<schema>.<tabellnavn>`.

**Autentisering:**
Azure AD med `@EnableJwtTokenValidation`. Profilen `noauth` deaktiverer sikkerhet (brukes i lokal utvikling/test). Endepunkter som skal være åpne annoteres med `@Unprotected`.

**Database-tilkobling:**
HikariCP med `maximum-pool-size: 2` (Oracle on-prem, lavt volum). Schema settes via `hikari.schema`. DDL-validering via Hibernate (`ddl-auto: validate`).

## Minimale endringer

Når du retter en feil eller implementerer en funksjon, skal du bare endre det som er nødvendig.
Ikke gi variabler nye navn, omstrukturer velfungerende kode, eller refaktorer utover det oppgaven krever.
Hold diffene små og fokuserte, slik at de er enkle å gjennomgå.
