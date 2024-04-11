# Monitorieringsverktøy for Infotrygd Replikering

## Tekniske krav til tabeller

- Må ha en kolonne som heter `OPPDATERT`
- Må ha index på kolonne `OPPDATERT`
- Tabeller som skal overvåkes legges inn i tabell "replikering_status"
- Når indeks for overvåket tabell ikke er tilgjengelig må "READY" settes til false i "replikering_status"
