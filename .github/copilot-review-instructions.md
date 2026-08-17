---
applyTo: "**"
---

# Code Review Instructions — infotrygd-replikering

Flag for human reviewer — do not block.

## Kotlin

- Parameterized SQL queries (`?` or named params), never string concatenation
- Use JUnit 5 + AssertJ conventions in tests
- Prefer sealed classes for state modeling

## General Checks

- **Scope**: Reasonable scope, no unrelated changes bundled in
- **Branch name**: Expected prefix `feature/`, `fix/`, `chore/`, `docs/`, `refactor/`

## Security-Critical Changes (always flag)

### Secrets and Credentials

- Hardcoded tokens, passwords, API keys, or credentials in code or config
- Vault references added or changed (`spring.cloud.vault`, `VAULT_*`)
- Environment variables containing `SECRET`, `TOKEN`, `CREDENTIAL`, `PASSWORD`
- `.env` files committed (should be in `.gitignore`)

### Sensitive Data in Output

- FNR (fødselsnummer), aktørId, navn, or other PII in log statements
- Token values, request bodies, or headers logged
- Exception messages containing sensitive context

### Authentication and Authorization

- Azure AD configuration changed (`azure.app.client-id`, token-validation-spring)
- New or modified API endpoints without `@ProtectedWithClaims` or `@Unprotected` annotation
- `noauth`-profilen aktivert i noe annet enn lokal utvikling/test
- CORS configuration changes

## Infrastructure Changes (flag for review)

### GitHub Actions

Changes to `.github/workflows/`:

- Deployment targets or environments changed
- Test steps removed or weakened (`mvn test` skipped)
- Unpinned action versions (use SHA, not `@main` or `@v3`)
- Secrets used in workflow steps
- `pull_request_target` trigger (security risk)
- New permissions granted to workflow

### Nais Configuration

Changes to `nais/`:

- `accessPolicy` — inbound/outbound rules changed
- `env` — new secrets, scopes, or credentials
- `azure.application` configuration changed
- Resource limits (CPU, memory) significantly reduced

## Code Quality (flag if concerning)

### Test Coverage

- Test files removed or test assertions deleted
- `@Disabled` added without explanation
- `mvn package -DskipTests` in a context where tests should run
- Coverage-reducing changes to scheduling or metric logic

### Error Handling

- Catch-all exception handlers swallowing errors silently
- Missing error handling in `@Scheduled` methods
- SQL built by string concatenation (use named params or `?`)

### Over-editing

Flag changes where the diff is disproportionate to the stated goal:

- Renamed variables or parameters not related to the fix
- Restructured working code without justification
- Added refactoring outside the PR scope

### Norsk tekst

- Bruk norsk bokmål for brukerrettet tekst og vedvarende dokumentasjon i repoet
- Unngå unødige anglifismer når det finnes gode norske alternativer

