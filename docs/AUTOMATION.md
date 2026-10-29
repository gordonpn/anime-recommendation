# Repository Automation

Workflows in this repository are adapted from the automated quality gates and review infrastructure in `gordonpn/shunt-shim` and `gordonpn/snaptally`.

## Workflows Overview

| Workflow | File | Triggers | Required Secrets | Purpose |
|---|---|---|---|---|
| CI: Build & Test | `ci-build-test.yml` | Push `main`, PR, manual | None | Compiles and executes tests across the Java 25 toolchain, archiving test reports. |
| Coverage | `coverage.yml` | Push `main`, PR | `CODECOV_TOKEN` (Optional) | Executes tests with JaCoCo and uploads XML coverage reports to Codecov via OIDC or token. |
| CodeQL | `codeql.yml` | Push `main`, PR, Monday cron | None | Analyzes compiled Java classes for vulnerabilities via CodeQL manual build mode. |
| Reviewdog | `reviewdog.yml` | PR | None | Validates GitHub Actions workflow syntax using actionlint and annotates PR diffs. |
| DeepSeek Review | `deepseek-review.yml` | PR `opened`, `synchronize`, `ready_for_review` | `DEEPSEEK_API_KEY` | Performs skeptical correctness review focusing on runtime contracts and edge cases. |
| PR-Agent | `pr-agent.yml` | PR `opened`, `synchronize`, `ready_for_review`, comments | `DEEPSEEK_API_KEY` | Performs skeptical architecture review and handles PR commands. |

## Workflow Details

### 1. Build and Coverage Toolchains
- **Runtime Environment:** Compiles and tests against Temurin JDK 25 via Gradle toolchains. The Gradle daemon runs on JDK 21.
- **JaCoCo Reporting:** Configured via `jacocoTestReport` in `build.gradle` and finalized by the `test` task.
- **Codecov Upload:** Runs with least-privilege permissions (`contents: read`, `id-token: write`) and authenticates via GitHub OIDC (`use_oidc: ${{ secrets.CODECOV_TOKEN == '' }}`). If `CODECOV_TOKEN` is configured, it is passed directly.

### 2. CodeQL Static Analysis
- Configured for `java-kotlin` with `build-mode: manual`.
- Executes `task build` (`./gradlew clean bootJar`) to allow CodeQL to trace class generation.
- Permissions include `actions: read` and `security-events: write`.

### 3. Reviewdog & Actionlint
- Runs `actionlint` via `reviewdog/action-actionlint@v1` on all pull requests, including fork pull requests.
- Emits review comments directly onto GitHub PRs for any workflow syntax or type issues.

### 4. AI Code and Architecture Reviewers
- Reviewers trigger when same-repository pull requests are opened, synchronized, or marked ready for review. Draft PRs and bot-authored events are skipped automatically to conserve API tokens and prevent noise during active development.
- **DeepSeek Review:** Evaluates PR diffs using `hustcer/deepseek-review` to check correctness, boundary conditions, concurrency, and unverified assumptions.
- **PR-Agent:** Evaluates PR diffs on open/synchronize/ready and responds to commands in `issue_comment` events posted by authorized collaborators (`OWNER`, `MEMBER`, `COLLABORATOR`). Configured via `.pr_agent.toml` using DeepSeek models to challenge unnecessary abstractions, premature complexity, and architecture regressions. Each comment event uses an isolated concurrency group to avoid dropping concurrent commands.
