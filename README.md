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
- Docker com Compose (banco de desenvolvimento)

Crie o `.env` da raiz e preencha `JWT_JWK_SET_URI`, `JWT_AUDIENCE` e `AI_API_KEY` (veja [1.2 Configuração](#12-configuração)):

```bash
cp example.env .env
```

Suba o banco de desenvolvimento:

```bash
docker compose -f docker-compose.dev.yaml up -d
```

Aplique as migrations do Liquibase (`migrations/core-api/root-changelog.yaml`). O plugin usa `JDBC_URL`, `DB_USERNAME` e `DB_PASSWORD` do ambiente, que por padrão apontam para o banco do `docker-compose.dev.yaml`:

```bash
set -a && . ./.env && set +a
cd migrations && ./mvnw liquibase:update -Dliquibase.changeLogFile=core-api/root-changelog.yaml && cd ..
```

Rode o mesmo comando sempre que uma migration nova for adicionada.

Gere as classes do jOOQ e rode a API em http://localhost:8080. O jOOQ gera as classes a partir do schema do banco de `JDBC_URL`, com `DB_USERNAME` e `DB_PASSWORD` do ambiente (por padrão, o banco do `docker-compose.dev.yaml`), então o banco precisa estar no ar e com as migrations aplicadas. Rode no mesmo shell em que o `.env` foi carregado:

```bash
cd core-api
./mvnw clean install -DskipTests
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O fuso horário da aplicação é o do servidor, que deve estar no horário de Brasília (`America/Sao_Paulo`). A imagem Docker já o define (`TZ`); rodando fora dela, use `TZ=America/Sao_Paulo`.

Pelo Maven, o `.env` lido é o da raiz. Para usar outro arquivo, passe `-Ddotenv.file=/caminho/.env`.

### 1.2 Configuração

O Core API lê cada variável do **ambiente** e, se ela não estiver lá, do **`.env` da raiz**, cujo modelo é o `example.env`. Em produção, os valores vêm gravados na imagem (veja [3 Deploy](#3-deploy)). Uma variável definida no ambiente vence a do `.env`.

O `.env` é lido como um arquivo `.properties`: `CHAVE=valor`, um por linha, comentários com `#`. Não use `export` nem aspas, porque elas viram parte do valor.

| Variável | Obrigatória | Descrição | Exemplo |
|---|---|---|---|
| `JDBC_URL` | não | URL JDBC do banco. Padrão: o do `docker-compose.dev.yaml`. | `jdbc:postgresql://localhost:5432/core_api` |
| `DB_USERNAME` | não | Usuário do banco. Padrão: `pg`, o do `docker-compose.dev.yaml`. | `pg` |
| `DB_PASSWORD` | não | Senha do banco. Padrão: `pgpw`, o do `docker-compose.dev.yaml`. | `pgpw` |
| `JWT_JWK_SET_URI` | sim | URL das chaves públicas (JWKS) do emissor dos tokens | `https://auth.exemplo.com/.well-known/jwks.json` |
| `JWT_AUDIENCE` | sim | Valor que o `aud` do token precisa conter | `core-api` |
| `JWT_ISSUER` | não | Se preenchida, o `iss` do token precisa ser igual a ela | `https://auth.exemplo.com` |
| `JWT_ORGANIZATION_CLAIM` | não | Claim com o id da organização. Padrão: `organization`. | `organization` |
| `AI_API_KEY` | sim | Chave da API do provedor do modelo de IA (hoje, o DeepSeek) | `sk-...` |
| `CORS_ALLOWED_ORIGINS` | não | Origens liberadas, separadas por vírgula. Padrão: `http://localhost:5173`. | `https://dashboard.exemplo.com,http://localhost:5173` |
| `TUITION_PAYMENTS_JOB_CRON` | não | Quando lançar as mensalidades, em cron de seis campos (segundo, minuto, hora, dia, mês, dia da semana), no fuso do servidor. Elas também são lançadas ao iniciar a API, caso ela estivesse fora do ar na virada do mês. `-` desliga os dois. Padrão: `0 0 0 1 * *`. | `0 0 3 1 * *` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | não | Coletor OTLP/HTTP que recebe traces, métricas e logs. Sem ela, traces e logs não são exportados, mas as métricas continuam tentando `http://localhost:4318`. Ignorada no perfil `dev`. As demais variáveis `OTEL_*` padrão do OpenTelemetry também são aceitas (veja [1.3 Observabilidade](#13-observabilidade)). O caminho de cada sinal (`/v1/traces`, `/v1/metrics`, `/v1/logs`) é acrescentado a ela. | `https://alvarengacarlos.com/otel-collector` |
| `OTEL_EXPORTER_OTLP_HEADERS` | não | Headers enviados ao coletor, no formato `chave=valor` separados por vírgula, com os valores codificados para URL (o espaço vira `%20`). | `Authorization=Bearer%20<token>` |
| `SPRING_PROFILES_ACTIVE` | não | `dev` escreve os logs em texto. | `dev` |
| `DOTENV_FILE` | não | Caminho de outro arquivo `.env`. Padrão: `.env` no diretório atual. | `/etc/unnamed/.env` |

### 1.3 Observabilidade

O Core API é instrumentado com o OpenTelemetry pelo `spring-boot-starter-opentelemetry` e exporta os três sinais por OTLP/HTTP para `OTEL_EXPORTER_OTLP_ENDPOINT`, com `service.name=core-api` e `service.namespace=unnamed`:

- **Traces**: requisições HTTP (com `request.id` e `organization.id`), filtros do Spring Security, consultas e conexões JDBC (as do jOOQ inclusive), o job das mensalidades e as chamadas ao modelo de IA e às suas tools (Spring AI). Todas as requisições são amostradas; o `traceparent` (W3C) recebido é respeitado.
- **Métricas**: HTTP (com histograma de latência), JVM, processo, HikariCP, JDBC, tarefas agendadas e Spring AI, enviadas a cada minuto.
- **Logs**: os mesmos do console, com `trace_id`, `span_id`, `requestId`, `organizationId` e os campos do log de acesso (`method`, `path`, `status`, `durationMs`).

As variáveis padrão do OpenTelemetry vindas do ambiente são aceitas e vencem o `application.yaml`, por exemplo `OTEL_EXPORTER_OTLP_HEADERS` (autenticação no coletor), `OTEL_TRACES_SAMPLER_ARG` (fração amostrada, de `0` a `1`), `OTEL_METRIC_EXPORT_INTERVAL` (em ms), `OTEL_METRICS_EXPORTER=none` e `OTEL_SDK_DISABLED=true`. Elas só valem quando estão no ambiente, não lidas do `.env` pela aplicação: carregue o `.env` no shell (`set -a && . ./.env && set +a`), como em [1.1 Desenvolvimento](#11-desenvolvimento).

No perfil `dev` o OpenTelemetry fica desligado: nada é exportado, nem com `OTEL_EXPORTER_OTLP_ENDPOINT` definida, e as variáveis `OTEL_*` são ignoradas.

Nos testes nada é exportado.

### 1.4 Testes

Todos os comandos rodam em `core-api/`.

Os testes de integração e de API rodam no banco de `JDBC_URL`, o mesmo usado para gerar as classes do jOOQ, e esvaziam as tabelas antes de cada teste: os dados desse banco são apagados. O banco precisa estar no ar e com as migrations aplicadas.

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

O Woodpecker (`.woodpecker/`) aplica as migrations, testa e faz o build das imagens, enviando-as para o registry `alvarengacarlos.com:2001`. A configuração de produção é gravada nas imagens durante o build, então os containers sobem sem nenhuma variável:

- **Core API**: os `build_args` do `.woodpecker/core-api.yaml` viram `ENV` da imagem. Os valores fixos estão no próprio arquivo; os sensíveis vêm dos secrets do Woodpecker `db_user`, `db_password`, `ai_api_key` e `otel_exporter_otlp_headers` (no formato `Authorization=Bearer%20<token>`).
- **Dashboard**: as `VITE_*` do `.woodpecker/dashboard.yaml` são gravadas no build.

Atenção: o registry é público, então qualquer um que baixe a imagem do Core API consegue ler esses valores, inclusive a senha do banco, a chave da IA e o token do coletor (`docker inspect`).

Para mudar um valor, altere o pipeline (ou o secret) e rode-o de novo. Para sobrescrever sem um novo build, passe a variável ao executar o container (`docker run -e VARIAVEL=valor`): ela vence a da imagem.

As imagens são `alvarengacarlos.com:2001/unnamed/core-api` (porta 8080) e `alvarengacarlos.com:2001/unnamed/dashboard` (porta 80).

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
