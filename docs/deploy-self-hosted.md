# Deploy Self-Hosted (Docker + GHCR)

Este documento descreve como executar o AI$HA em ambiente self-hosted usando imagem Docker publicada no GitHub Container Registry (GHCR).

## Pré-requisitos

- Docker Engine 24+
- Docker Compose Plugin 2.20+
- Conta GitHub com permissão para puxar a imagem (se o pacote for privado)

## 1) Obter imagem do GHCR

Formato da imagem:

- `ghcr.io/<owner>/<repo>:<versao-semver>`
- `ghcr.io/<owner>/<repo>:latest`

Exemplo:

```bash
docker pull ghcr.io/OWNER/REPOSITORY:1.2.3
```

Se o pacote estiver privado, faça login antes:

```bash
echo "$GITHUB_TOKEN" | docker login ghcr.io -u GITHUB_USERNAME --password-stdin
```

## 2) Subir stack com docker-compose

Use o arquivo `compose.yml` do repositório.

Ajuste ao menos:

- `image` da aplicação (owner/repo/tag corretos)
- `POSTGRES_PASSWORD`
- `DB_PASSWORD`
- `AISHA_SECURITY_SEED_PASSWORD`
- `TZ` (default no Dockerfile: `UTC`)
- `JAVA_OPTS` (default no Dockerfile com flags para execução em container)

Inicie os serviços:

```bash
docker compose up -d
```

Aplicação disponível em:

- `http://localhost:8080`

## 3) Perfis e banco

A imagem já inicia com suporte aos perfis abaixo:

- `postgres`: ativa datasource PostgreSQL
- `prod`: ativa políticas de produção (ex.: cookie secure)

O schema é criado automaticamente no startup (`ddl-auto: update`).

## 4) Atualização de versão

Para atualizar:

1. Ajuste a tag da imagem no `compose.yml` para uma versão SemVer publicada (ex.: `1.3.0`).
2. Execute:

```bash
docker compose pull
docker compose up -d
```

## 5) Fluxo de release automático (CI/CD)

O release é acionado quando um PR é mergeado em `main` contendo um dos labels:

- `release:patch`
- `release:minor`
- `release:major`

A pipeline executa:

1. Lê a última tag `vX.Y.Z`.
2. Calcula a próxima versão SemVer.
3. Cria e publica nova tag Git `vX.Y.Z`.
4. Builda e publica imagem no GHCR com tags:
   - `ghcr.io/<owner>/<repo>:X.Y.Z`
   - `ghcr.io/<owner>/<repo>:latest`

## 6) Fluxo de snapshot release (manual)

Para gerar release de snapshot manualmente, execute o workflow:

- `.github/workflows/release-snapshot-ghcr.yml`

Entrada obrigatória:

- `source_branch`: branch de origem usada no checkout (último commit da branch).

Formato da git tag gerada:

- `<nextVersion>-snapshot.<branch>.<shortsha>`

Onde:

- `nextVersion`: próximo `minor` calculado a partir da última tag estável `vX.Y.Z` do fluxo principal.
- `branch`: nome da branch sanitizado (ex.: `feature/nova-tela` vira `feature-nova-tela`).
- `shortsha`: SHA curto do commit usado no snapshot.

Publicação no GHCR:

- A imagem é publicada com o mesmo formato da git tag.

Publicação de GitHub Release:

- Cada execução publica um GitHub Release com artefato JAR do build Maven.
- Convenções de nome:
  - release final: `aisha-X.Y.Z.jar`
  - snapshot: `aisha-<nextVersion>-snapshot.<branch>.<shortsha>.jar`

## 7) Fluxo de release final manual

Para gerar release final manualmente a partir do estado atual da `main`, execute:

- `.github/workflows/release-manual-ghcr.yml`

Entrada obrigatória:

- `release_type`: `major`, `minor` ou `patch`.

A pipeline executa o mesmo processo do release automático:

1. Lê a última tag `vX.Y.Z`.
2. Calcula a próxima versão SemVer com base no `release_type`.
3. Cria e publica a nova tag Git `vX.Y.Z`.
4. Builda e publica imagem multi-arch no GHCR (`linux/amd64`, `linux/arm64`):
   - `ghcr.io/<owner>/<repo>:X.Y.Z`
   - `ghcr.io/<owner>/<repo>:latest`
5. Cria GitHub Release anexando o JAR renomeado:
   - `aisha-X.Y.Z.jar`
