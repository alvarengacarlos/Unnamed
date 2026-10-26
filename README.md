# Unnamed

Sistema de gestão para organizações de artes marciais.

O projeto é agnóstico ao provedor de identidade: funciona com qualquer plataforma ou projeto que implemente o OpenID Connect. Basta que o token tenha:

- assinatura verificável pelas chaves publicadas no `jwks_uri` do discovery;
- `aud`;
- `iss`;
- um claim com o UUID da organização.

## 1 Core API
API com as regras de negócio do dia a dia da organização.

### 1.1 Desenvolvimento

Pré-requisitos:

- JDK 25 (o Maven vem pelo wrapper `./mvnw`)
- Docker com Compose (também usado pela build e pelos testes, via Testcontainers)

Crie o `.env` da raiz e preencha `JWT_JWK_SET_URI` e `JWT_AUDIENCE` (veja [1.2 Configuração](#12-configuração)):

```bash
cp example.env .env
```

Suba o banco de desenvolvimento:

```bash
docker compose -f docker-compose.dev.yaml up -d
```

> Não é preciso rodar as migrações: a API as aplica ao iniciar com o profile `dev`.

Gere as classes do jOOQ e rode a API em http://localhost:8080:

```bash
cd core-api
./mvnw clean install -DskipTests
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O fuso horário da aplicação é o do servidor, que deve estar no horário de Brasília (`America/Sao_Paulo`). A imagem Docker já o define (`TZ`); rodando fora dela, use `TZ=America/Sao_Paulo`.

Pelo Maven, o `.env` lido é o da raiz. Para usar outro arquivo, passe `-Ddotenv.file=/caminho/.env`.

### 1.2 Configuração

O Core API lê cada variável do **ambiente** e, se ela não estiver lá, do **`.env` da raiz**, cujo modelo é o `example.env`. Uma variável definida no ambiente vence a do `.env`.

O `.env` é lido como um arquivo `.properties`: `CHAVE=valor`, um por linha, comentários com `#`. Não use `export` nem aspas, porque elas viram parte do valor.

| Variável | Obrigatória | Descrição | Exemplo |
|---|---|---|---|
| `JDBC_URL` | sim | URL JDBC do banco | `jdbc:postgresql://localhost:5432/unnamed` |
| `DB_USERNAME` | sim | Usuário do banco. Em desenvolvimento, também cria o usuário do banco do `docker-compose.dev.yaml`. | `pg` |
| `DB_PASSWORD` | sim | Senha do banco. Em desenvolvimento, também define a senha do banco do `docker-compose.dev.yaml`. | `pgpw` |
| `JWT_JWK_SET_URI` | sim | URL das chaves públicas (JWKS) do emissor dos tokens | `https://auth.exemplo.com/.well-known/jwks.json` |
| `JWT_AUDIENCE` | sim | Valor que o `aud` do token precisa conter | `core-api` |
| `JWT_ISSUER` | não | Se preenchida, o `iss` do token precisa ser igual a ela | `https://auth.exemplo.com` |
| `JWT_ORGANIZATION_CLAIM` | não | Claim com o id da organização. Padrão: `organization`. | `organization` |
| `CORS_ALLOWED_ORIGINS` | não | Origens liberadas, separadas por vírgula. Padrão: `http://localhost:5173`. | `https://dashboard.exemplo.com,http://localhost:5173` |
| `DB_MIGRATE_ON_STARTUP` | não | `true` para aplicar as migrations ao iniciar. Padrão: `false`. | `true` |
| `TUITION_PAYMENTS_JOB_CRON` | não | Quando lançar as mensalidades, em cron de seis campos (segundo, minuto, hora, dia, mês, dia da semana), no fuso do servidor. Elas também são lançadas ao iniciar a API, caso ela estivesse fora do ar na virada do mês. `-` desliga os dois. Padrão: `0 0 0 1 * *`. | `0 0 3 1 * *` |
| `SPRING_PROFILES_ACTIVE` | não | `dev` aplica as migrations ao iniciar e escreve os logs em texto. | `dev` |
| `DOTENV_FILE` | não | Caminho de outro arquivo `.env`. Padrão: `.env` no diretório atual. | `/etc/unnamed/.env` |

### 1.3 Testes

Todos os comandos rodam em `core-api/`.

#### Unitários

```bash
./mvnw test
```

#### Repositórios

Roda também os unitários.

```bash
./mvnw verify -Dit.test='*RepositoryIT'
```

#### API

Roda também os unitários.

```bash
./mvnw verify -Dit.test='*ApiIT'
```

#### Uma classe

Roda também os unitários.

```bash
./mvnw verify -Dit.test=RankApiIT
```

#### Todos

Roda o checkstyle e todos os testes.

```bash
./mvnw verify
```

## 2 Dashboard

Painel web usado pela organização para administrar o dia a dia.

### 2.1 Desenvolvimento

Pré-requisitos:

- Node.js e npm

Crie o `.env` e preencha as variáveis do provedor OpenID Connect (veja [2.2 Configuração](#22-configuração)):

```bash
cd dashboard
cp example.env .env
```

Instale as dependências:

```bash
npm ci
```

Rode o app em http://localhost:5173:

```bash
npm run dev
```

### 2.2 Configuração
O Dashboard lê as variáveis do `dashboard/.env`, cujo modelo é o `dashboard/example.env`. As `VITE_*` são gravadas no build.

| Variável | Obrigatória | Descrição | Exemplo |
|---|---|---|---|
| `VITE_API_BASE_URL` | sim | URL do Core API, como o navegador a acessa, sem `/` no fim | `http://localhost:8080` |
| `VITE_OIDC_AUTHORITY` | sim | Issuer do provedor OpenID Connect que abre a sessão, sem `/` no fim. O Dashboard encontra os endpoints pelo discovery dele. | `https://auth.exemplo.com` |
| `VITE_OIDC_CLIENT_ID` | sim | Id do client público do Dashboard no provedor | `dashboard` |
| `E2E_USERNAME` | só nos testes contra o backend | Usuário de teste que faz o login. Use um usuário só para os testes: eles criam e apagam registros na organização dele. | `e2e` |
| `E2E_PASSWORD` | só nos testes contra o backend | Senha do usuário de teste | `e2e-senha` |
| `E2E_BASE_URL` | não | Endereço do app nos testes contra o backend. Padrão: `http://localhost:5173`. | `http://localhost:5173` |

### 2.3 Testes

Todos os comandos rodam em `dashboard/`.

#### Unitários

Converters, schemas, o cliente HTTP, os hooks e a sessão, sem navegador nem backend.

```bash
npm test
```

Para rodar a cada alteração: `npm run test:watch`.

#### E2E

Antes do primeiro teste e2e, instale o navegador:

```bash
npx playwright install firefox
```

##### Com o Core API e o provedor OpenID Connect simulados

```bash
npm run pw:test
```

##### Contra o backend de verdade

Precisa do Core API, do provedor OpenID Connect e do banco de dados rodando, e de `E2E_USERNAME` e `E2E_PASSWORD` no `dashboard/.env`.

```bash
npm run pw:test:backend
```

## 3 Deploy

1. Crie a rede externa do Traefik, se ela ainda não existir:

   ```bash
   docker network create traefik_net
   ```

2. Crie o `.env` da raiz e preencha todas as variáveis: as do Core API, com `JDBC_URL` apontando para o banco de produção (veja [1.2 Configuração](#12-configuração)), e as `VITE_*` do Dashboard (veja [2.2 Configuração](#22-configuração)):

   ```bash
   cp example.env .env
   ```

3. Gere o jar do Core API:

   ```bash
   (cd core-api && ./mvnw -B clean package -DskipTests)
   ```

4. Suba os containers:

   ```bash
   docker compose up -d --build
   ```

## 4 Documentação

Os diagramas ficam em [`docs/`](docs/), escritos em [PlantUML](https://plantuml.com). Eles usam o layout embutido do PlantUML (`smetana`), então renderizam sem o Graphviz, inclusive pelas extensões de PlantUML do VS Code e do IntelliJ.

| Arquivo | O que mostra |
|---|---|
| [`c4-1-system-context.puml`](docs/c4-1-system-context.puml) | [C4](https://c4model.com), nível 1 (contexto): a equipe da organização, o Unnamed e o provedor OpenID Connect. |
| [`c4-2-container.puml`](docs/c4-2-container.puml) | C4, nível 2 (containers): o conteúdo estático, o Dashboard, o Core API e o banco de dados, e como se comunicam. |
| [`c4-3-component.puml`](docs/c4-3-component.puml) | C4, nível 3 (componentes): as partes do Core API, da segurança ao acesso ao banco. |
| [`der.puml`](docs/der.puml) | Diagrama entidade-relacionamento do banco do Core API: as tabelas e as chaves estrangeiras entre elas. |

Para gerar as imagens pela linha de comando:

```bash
java -jar plantuml.jar docs/*.puml
```
