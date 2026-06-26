# Self-Hosted Deployment (Docker + GHCR)

This document describes how to run AI$HA in a self-hosted environment using the Docker image published on GitHub Container Registry (GHCR).

## Prerequisites

- Docker Engine 24+
- Docker Compose Plugin 2.20+
- GitHub account with permission to pull the image (if the package is private)

## 1) Pull the image from GHCR

Image format:

- `ghcr.io/<owner>/<repo>:<semver-version>`
- `ghcr.io/<owner>/<repo>:latest`

Example:

```bash
docker pull ghcr.io/OWNER/REPOSITORY:1.2.3
```

If the package is private, log in first:

```bash
echo "$GITHUB_TOKEN" | docker login ghcr.io -u GITHUB_USERNAME --password-stdin
```

## 2) Start the stack with Docker Compose

Use the repository `compose.yml` file.

Adjust at least:

- application `image` (correct owner/repo/tag)
- `POSTGRES_PASSWORD`
- `DB_PASSWORD`
- `AISHA_SECURITY_SEED_PASSWORD`
- `TZ` (default in Dockerfile: `UTC`)
- `JAVA_OPTS` (default in Dockerfile includes container runtime flags)
- backup volume mapping for `/var/lib/aisha/backups`

Start services:

```bash
docker compose up -d
```

Application URL:

- `http://localhost:8080`

## 3) Profiles and database

The image starts with support for the profiles below:

- `postgres`: enables PostgreSQL datasource
- `prod`: enables production policies (for example, secure cookie)

Schema is applied automatically at startup by Flyway (`db/migration`).

## 4) Backup persistence

In the `prod` profile, server-side backup archives are written to:

```text
/var/lib/aisha/backups
```

This path must be backed by a Docker volume or a host bind mount. Otherwise, generated backup files are stored only in the container filesystem and are lost when the container is removed.

The example Compose file maps a named volume:

```yaml
services:
  aisha:
    volumes:
      - backup_data:/var/lib/aisha/backups

volumes:
  backup_data:
```

For host-managed backups, replace the named volume with a bind mount, for example:

```yaml
services:
  aisha:
    volumes:
      - ./data/backups:/var/lib/aisha/backups
```

## 5) Version upgrade

To upgrade:

1. Update the image tag in `compose.yml` to a published SemVer version (for example, `1.3.0`).
2. Run:

```bash
docker compose pull
docker compose up -d
```

## 6) Automated release flow (CI/CD)

A release is triggered when a PR is merged into `main` with one of these labels:

- `release:patch`
- `release:minor`
- `release:major`

Pipeline steps:

1. Read latest tag `vX.Y.Z`.
2. Calculate next SemVer version.
3. Create and publish new git tag `vX.Y.Z`.
4. Build and publish GHCR image tags:
   - `ghcr.io/<owner>/<repo>:X.Y.Z`
   - `ghcr.io/<owner>/<repo>:latest`

## 7) Snapshot release flow (manual)

To generate a manual snapshot release, run workflow:

- `.github/workflows/release-snapshot-ghcr.yml`

Required input:

- `source_branch`: source branch used for checkout (latest commit from that branch).

Generated git tag format:

- `<nextVersion>-snapshot.<branch>.<shortsha>`

Where:

- `nextVersion`: next `minor` computed from the latest stable tag `vX.Y.Z` in the main flow.
- `branch`: sanitized branch name (for example, `feature/new-screen` becomes `feature-new-screen`).
- `shortsha`: short SHA of the commit used for the snapshot.

GHCR publication:

- The image is published using the same tag format.

GitHub Release publication:

- Each run publishes a GitHub Release with the Maven build JAR artifact.
- Naming conventions:
  - final release: `aisha-X.Y.Z.jar`
  - snapshot: `aisha-<nextVersion>-snapshot.<branch>.<shortsha>.jar`

## 8) Manual final release flow

To generate a manual final release from the current `main` state, run:

- `.github/workflows/release-manual-ghcr.yml`

Required input:

- `release_type`: `major`, `minor`, or `patch`.

Pipeline runs the same process as automated release:

1. Read latest tag `vX.Y.Z`.
2. Calculate next SemVer using `release_type`.
3. Create and publish new git tag `vX.Y.Z`.
4. Build and publish multi-arch GHCR images (`linux/amd64`, `linux/arm64`):
   - `ghcr.io/<owner>/<repo>:X.Y.Z`
   - `ghcr.io/<owner>/<repo>:latest`
5. Create GitHub Release attaching renamed JAR:
   - `aisha-X.Y.Z.jar`
