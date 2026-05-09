# Agent Guide

This is the short entry point for agents working in DataSync. Keep durable detail in focused docs and runnable scripts.

## Read First

- [ARCHITECTURE.md](ARCHITECTURE.md): current system map, module boundaries, invariants, risks.
- [docs/harness/README.md](docs/harness/README.md): harness operating model and task lifecycle.
- [docs/harness/quality-gates.md](docs/harness/quality-gates.md): verification commands and hard gates.
- [docs/harness/agent-protocol.md](docs/harness/agent-protocol.md): JSONL protocol for orchestrator/executor/evaluator work.
- [docs/harness/subagent-workflows.md](docs/harness/subagent-workflows.md): delegation patterns when the user explicitly asks for subagents.
- [docs/exec-plans/tech-debt-tracker.md](docs/exec-plans/tech-debt-tracker.md): visible baseline debt and cleanup queue.

## Project Snapshot

DataSync is a distributed file sync and backup system based on CDC chunking.

- `server/`: Spring Boot service, REST API, MySQL/Redis integration, Netty sync server, storage-side file reconstruction.
- `client-app/`: Spring Boot local client proxy, SQLite metadata, REST API for the desktop UI, Netty sync client.
- `sync-app/`: Electron + Vue 3 desktop UI, Axios calls to `client-app`.
- Shared Java sync logic lives under `dataSync/` packages in both Java modules.
- Existing user-facing docs: `README.md`, `API.md`, and `Database Tables.md`.

## Commands

- Harness audit: `powershell -ExecutionPolicy Bypass -File scripts/harness-check.ps1 -Mode audit`
- Harness verify: `powershell -ExecutionPolicy Bypass -File scripts/harness-check.ps1 -Mode verify`
- Server tests: from `server/`, run `.\mvnw.cmd test`
- Client proxy tests: from `client-app/`, run `.\mvnw.cmd test`
- Desktop lint: from `sync-app/`, run `npm run lint`
- Desktop build: from `sync-app/`, run `npm run build`

## Working Rules

- Use research-plan-implement for multi-file, architecture, auth, encryption, external-system, async, file I/O, data-loss, or unclear behavior changes.
- Do not edit `node_modules/`, `target/`, `build/`, `out/`, `dist/`, `.idea/`, `.tmp-pdf/`, `tmp/`, `output/`, generated logs, local databases, or build artifacts unless the task explicitly requires it.
- Do not add hard-coded secrets, credentials, tokens, private keys, or production endpoint values.
- Treat sync upload/download code as data-loss-sensitive: tests should use temp directories and fixtures, never real user folders or live storage.
- Keep `sync-app` talking to `client-app`; keep server-only APIs behind the client proxy unless a documented architecture change is approved.
- Add or update tests when changing behavior. If full verification cannot run because local services are missing, record the exact command and blocker.
- Update harness docs when module boundaries, verification commands, or repeated failure patterns change.
- **Group authorization pattern**: server-side mutations must use `GroupServiceImpl.canManage()` or `findOwned()` — do not inline `ownerEmail` / `admins` checks. If adding a new group operation, classify it as owner-only or admin-accessible and call the appropriate helper.
- **GroupPage.vue pattern**: all mutations return the updated `Group` and patch via `patchGroup(res?.data ?? res)`. Never call `loadGroups()` after a single mutation — that doubles network traffic for no benefit.
- **Local user cache pattern**: `client-app` SQLite `User.email` is unique, and login/signup/profile writes must preserve the server `User.id`; task APIs depend on `userMapper.selectByEmail(email).getId()` for `File.user_id`.
- **Lombok/NetBeans LSP warning**: `Can't initialize javac processor` warnings appear in IDE diagnostics for the Java modules. This is a known Lombok/Java 21 version incompatibility in the NetBeans language server used by VS Code; it does not affect Maven builds. Do not treat it as a real compilation error.
- **`/server/**` security**: all `/server/**` routes are `permitAll()` in `SecurityConfig`. This is the current development posture — do not assume server endpoints are auth-guarded at the HTTP layer.
