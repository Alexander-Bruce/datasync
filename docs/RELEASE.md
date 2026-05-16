# DataSync Release Guide

This document describes the release process used by the current project.

Current official release: **v1.0.7**.

---

## Release Sources

| Source                  | Current value                      | Notes                                    |
| ----------------------- | ---------------------------------- | ---------------------------------------- |
| Git tag                 | `v1.0.7`                           | Current public release tag.              |
| Desktop package version | `sync-app/package.json` -> `1.0.7` | Used by Electron Builder artifact names. |
| GitHub Release          | `DataSync v1.0.7`                  | Published by GitHub Actions.             |
| Release workflow        | `.github/workflows/release.yml`    | Builds Windows and Linux artifacts.      |

Keep the tag version and `sync-app/package.json` version aligned.

---

## Official Release Definition

A DataSync release is considered official when:

- It has a `v*` Git tag.
- GitHub Actions release workflow finished successfully.
- The GitHub Release is not a draft.
- The GitHub Release is not marked prerelease.
- Windows and Linux artifacts are attached.
- The release is the latest visible release on GitHub.

For `v1.0.7`, the release is already published as a non-draft, non-prerelease release.

---

## Assets

The release workflow publishes these asset types:

| Asset                                   | Platform | Description                |
| --------------------------------------- | -------- | -------------------------- |
| `sync-app-<version>-setup.exe`          | Windows  | NSIS installer.            |
| `sync-app-<version>-setup.exe.blockmap` | Windows  | Electron updater blockmap. |
| `latest.yml`                            | Windows  | Electron updater metadata. |
| `sync-app-<version>.AppImage`           | Linux    | Portable AppImage.         |
| `sync-app_<version>_amd64.deb`          | Linux    | Debian package.            |
| `latest-linux.yml`                      | Linux    | Electron updater metadata. |

macOS packaging is configured in `sync-app/electron-builder.yml`, but the current GitHub release workflow does not build macOS artifacts.

---

## Pre-Release Checklist

Before creating a new tag:

1. Confirm `main` is clean and up to date.
2. Update `sync-app/package.json` version.
3. Update README current release references if needed.
4. Run Java checks:

```bash
cd server
./mvnw spotless:check --no-transfer-progress
./mvnw compile -DskipTests --no-transfer-progress
```

```bash
cd client-app
./mvnw spotless:check --no-transfer-progress
./mvnw compile -DskipTests --no-transfer-progress
./mvnw test -Dtest=SqliteSchemaInitializerTest --no-transfer-progress
```

5. Run frontend checks:

```bash
cd sync-app
npm ci
npx prettier --check "src/**/*.{js,vue,css,html}"
npm run lint
```

6. Confirm no secrets or generated artifacts are staged:

```bash
git status --short
git diff --cached
```

---

## Create A Release

From a clean `main`:

```bash
git checkout main
git pull --ff-only
git tag v1.0.8
git push origin v1.0.8
```

The release workflow runs automatically for `v*` tags.

Manual dispatch is also supported:

1. Open GitHub Actions.
2. Select `Release`.
3. Run workflow.
4. Enter a tag such as `v1.0.8`.

---

## Verify A Release

After the workflow completes:

1. Open the GitHub Release page.
2. Confirm the release name matches the tag.
3. Confirm `prerelease` is false.
4. Confirm all expected assets are attached.
5. Download at least one small metadata file (`latest.yml` or `latest-linux.yml`) to ensure assets are reachable.
6. Confirm the release compare link is correct.

Optional CLI checks:

```bash
gh release view v1.0.8 --repo Alexander-Bruce/datasync
gh release list --repo Alexander-Bruce/datasync
```

These commands require `gh auth login`.

---

## Delete Older GitHub Releases

When keeping only the current official release visible on GitHub, delete older release objects but normally keep tags unless there is a specific reason to remove source history labels.

Recommended behavior:

- Delete GitHub Release records for tags older than the current official release.
- Keep Git tags such as `v1.0.0`, `v1.0.1`, etc. for source history.
- Do not delete the current release.

CLI example:

```bash
gh release delete v1.0.6 --repo Alexander-Bruce/datasync --yes
```

Do not pass `--cleanup-tag` unless you intentionally want to remove the Git tag too.

---

## Rollback

If a new release is broken:

1. Mark the broken release as prerelease or delete the release object.
2. Re-publish the last good tag as the visible official release if needed.
3. If updater metadata points to the broken version, upload corrected `latest.yml` / `latest-linux.yml` or cut a fixed patch release.
4. Do not rewrite Git tags that users may already have fetched unless absolutely necessary.

Preferred path:

```text
v1.0.8 broken -> fix forward with v1.0.9
```

---

## Current Release Notes Summary

`v1.0.7` is the current official binary release line. Documentation updates can land on `main` without rebuilding desktop assets.

If a new binary release is needed later, create a new tag so the release workflow rebuilds and republishes the desktop installers/packages.
