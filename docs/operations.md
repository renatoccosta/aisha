# Operations and Release Guide

This document centralizes operational and release details for AI$HA.

## Docker Distribution (GHCR)

The project includes a production-ready Docker image and a GitHub Actions release pipeline that publishes images to GitHub Container Registry (GHCR).

- Registry: `ghcr.io/<owner>/<repo>`
- Published tags:
  - `X.Y.Z` (SemVer release)
  - `latest`

### Local Docker Build

```bash
docker build -t aisha:local .
docker run --rm -p 8080:8080 aisha:local
```

## Automated Release Flow (SemVer by PR Label)

When a pull request is merged into `main`, apply one of these labels to control the version bump:

- `release:patch`
- `release:minor`
- `release:major`

Workflow behavior:

1. Read latest git tag `vX.Y.Z`.
2. Calculate next SemVer based on label.
3. Create and push new git tag `vX.Y.Z`.
4. Build and publish image to GHCR as:
   - `ghcr.io/<owner>/<repo>:X.Y.Z`
   - `ghcr.io/<owner>/<repo>:latest`

See workflow: `.github/workflows/release-ghcr.yml`

## Self-Hosted Deployment

- Compose file: `compose.yml`
- Full self-hosted guide: [docs/deploy-self-hosted.md](deploy-self-hosted.md)
