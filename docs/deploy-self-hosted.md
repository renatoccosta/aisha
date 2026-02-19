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
