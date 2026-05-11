# AGENTS.md — infotrygd-replikering

`infotrygd-replikering` overvåker replikering av Infotrygd-tabeller og eksponerer forsinkelse som Prometheus-metrikker.

## Bygg- og testkommandoer

```bash
mvn test        # Kjør tester
mvn verify      # Bygg og test
mvn package     # Pakk uten tester: mvn package -DskipTests
```

## Prosjektstruktur

```text
mock-oidc/
nais/
src/
```

## Kode- og endringsstil

### Minimale endringer

Når du retter en feil eller implementerer en funksjon, skal du bare endre det som er nødvendig.
Ikke gi variabler nye navn, omstrukturer fungerende kode eller refaktorer utover oppgaven du løser.
Hold diffene små og fokuserte, slik at de er enkle å gjennomgå.

## Git-flyt

Jobb på grenen som er opprettet for oppgaven, og hold endringene små og målrettede.

## Rammer

### ✅ Alltid

- Kjør tester etter endringer
- Følg eksisterende mønstre i prosjektet
- Bevar eksisterende kodestruktur — ikke reorganiser eller refaktorer utover oppgaven
- Valider all ekstern input

### ⚠️ Avklar først

- Endring av autentiseringsmekanismer
- Nye avhengigheter
- Endring av databaseskjema

### 🚫 Aldri

- Commit aldri hemmeligheter eller påloggingsinformasjon
- Hopp aldri over inputvalidering på eksterne grenser
