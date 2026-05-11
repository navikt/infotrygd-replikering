# AGENTS.md — infotrygd-replikering

`infotrygd-replikering` overvåker replikering av Infotrygd-tabeller og eksponerer forsinkelse som Prometheus-metrikker.

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
