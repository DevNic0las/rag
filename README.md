# RAG

API REST em Java que responde perguntas com base em documentos enviados pelo usuário (RAG — _Retrieval-Augmented Generation_).

O projeto é um MVP funcional: você envia um arquivo, ele é dividido em trechos, convertido em embeddings e salvo em um banco vetorial. Depois, ao fazer uma pergunta, os trechos mais parecidos são recuperados e enviados como contexto para um modelo de linguagem local (Ollama).

---

## Como funciona

```
POST /documents (arquivo)
        │
        ▼
TikaDocumentReader  →  extrai o texto do arquivo
        │
        ▼
TokenTextSplitter   →  divide em trechos de 500 tokens
        │
        ▼
nomic-embed-text    →  gera embeddings
        │
        ▼
PostgreSQL + pgvector  →  armazena os vetores
```

```
POST /chat/ask (pergunta)
        │
        ▼
nomic-embed-text  →  embedding da pergunta
        │
        ▼
pgvector (top 3)  →  trechos mais similares
        │
        ▼
llama3.2          →  responde usando os trechos como contexto
```

---

## Tecnologias

| Item | Versão / escolha |
|---|---|
| Java | 25 |
| Spring Boot | 4.1.2 |
| Spring AI | 2.0.1 |
| Modelo de chat | `llama3.2` (via Ollama) |
| Modelo de embeddings | `nomic-embed-text` (768 dimensões) |
| Banco vetorial | PostgreSQL 16 + pgvector (HNSW, distância de cosseno) |
| Extração de texto | Apache Tika (via `spring-ai-tika-document-reader`) |
| Build | Maven (wrapper incluído) |
| Lombok | usado em `IngestionService` |

---

## Estrutura

```
src/main/java/com/github/devnic0las/rag/
├── RagApplication.java    # main / configuração do Spring Boot
├── DocumentController.java # POST /documents  (ingestão de arquivos)
├── IngestionService.java   # leitura (Tika), chunking e gravação no vetor
└── ChatController.java     # POST /chat/ask   (pergunta + RAG)

src/main/resources/application.yaml  # Ollama, datasource e pgvector
src/test/java/.../RagApplicationTests.java  # único teste existente
docker-compose.yml                   # PostgreSQL com pgvector
```

---

## Pré-requisitos

- JDK 25
- Docker (para o PostgreSQL)
- [Ollama](https://ollama.com) rodando em `http://localhost:11434`

Baixe os modelos usados na configuração:

```bash
ollama pull llama3.2
ollama pull nomic-embed-text
```

---

## Como rodar

1. Suba o banco vetorial:

```bash
docker compose up -d
```

2. Inicie a aplicação:

```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. A tabela do pgvector é criada automaticamente (`initialize-schema: true`).

---

## Endpoints

### `POST /documents`

Envia um arquivo para a base de conhecimento. O campo do multipart deve se chamar `file`.

```bash
curl -X POST -F "file=@meu-documento.pdf" http://localhost:8080/documents
```

Resposta:

```json
{ "chunks": 12 }
```

`chunks` é a quantidade de trechos gerados e gravados no banco.

O formato é identificado pelo Tika, então a ideia é aceitar PDF, DOCX, HTML, TXT etc. O código não valida extensão, tamanho ou tipo do arquivo.

### `POST /chat/ask`

Envia uma pergunta em texto puro. A resposta também é texto puro.

```bash
curl -X POST -H "Content-Type: text/plain" \
  --data "O que diz o documento sobre prazos?" \
  http://localhost:8080/chat/ask
```

A busca vetorial usa `topK: 3` (os três trechos mais similares).

---

## Configuração

Tudo está em `src/main/resources/application.yaml`:

| Propriedade | Valor |
|---|---|
| `spring.ai.ollama.base-url` | `http://localhost:11434` |
| `spring.ai.ollama.chat.model` | `llama3.2` |
| `spring.ai.ollama.embedding.model` | `nomic-embed-text` |
| `spring.ai.vectorstore.pgvector.dimensions` | `768` |
| `spring.ai.vectorstore.pgvector.distance-type` | `COSINE_DISTANCE` |
| `spring.ai.vectorstore.pgvector.index-type` | `HNSW` |
| `spring.ai.vectorstore.pgvector.initialize-schema` | `true` |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/rag` |
| `spring.datasource.username` / `password` | `postgres` / `postgres` |

As credenciais estão fixas no arquivo (adequado apenas para desenvolvimento local).

O `docker-compose.yml` sobe a imagem `pgvector/pgvector:pg16` com banco `rag`, usuário `postgres` e volume `pgdata`.

---

## Testes

**O que existe hoje:** um único teste, `RagApplicationTests.contextLoads`, que apenas verifica se o contexto do Spring sobe.

**O que não existe:**

- testes de integração dos endpoints (`/documents`, `/chat/ask`);
- testes unitários (chunking, ingestão, montagem do prompt);
- testes end-to-end com Ollama e banco reais;
- testes de avaliação da qualidade das respostas (métricas de retrieval/groundedness).

Observação importante sobre o teste atual: ele está anotado com `@SpringBootTest` e declara um `PostgreSQLContainer` com `@ServiceConnection`, mas o `@Bean` está na própria classe de teste, sem `@TestConfiguration` — na prática o container não é aplicado e o teste usa o datasource do `application.yaml`. Ou seja, `./mvnw test` só passa com o PostgreSQL do `docker-compose` rodando em `localhost:5432`. Além disso, a imagem usada no teste (`postgres:16-alpine`) não possui a extensão pgvector, diferente da imagem do compose.

---

## O que este projeto ainda não tem

- Autenticação/autorização.
- Validação de tipo e tamanho de arquivo, limite de upload e tratamento de erros da API (as exceções sobem como erro 500).
- Endpoint para listar, remover ou reprocessar documentos.
- Retorno das fontes/trechos usados na resposta (a resposta é apenas o texto do modelo).
- Filtro por documento ou por metadado na busca (`source` é gravado, mas não é usado em filtros).
- Histórico de conversa / memória entre perguntas (cada chamada é independente).
- Prompt de sistema, template de prompt ou configuração de temperatura.
- Streaming da resposta (token a token).
- Reranking, controle de duplicidade na ingestão ou atualização de documentos já indexados.
- Migrações versionadas de banco (Flyway/Liquibase) — o schema é criado pelo próprio Spring AI.
- Documentação OpenAPI/Swagger.
- Interface web (apenas API).
- Dockerfile da aplicação, pipeline de CI e observabilidade.
- Testes de integração (ver seção acima).
- Licença definida (o `pom.xml` tem o campo de licença vazio).

## Próximos passos

O repositório não define um roadmap. As lacunas acima são os candidatos naturais de evolução, com destaque para: escrever testes de integração reais (Testcontainers com imagem pgvector) e adicionar tratamento de erros e validação de upload.

---

## Observações

- `HELP.md` é o arquivo padrão gerado pelo Spring Initializr, sem relação com a documentação do projeto.
- O modelo de chat roda localmente via Ollama, então a qualidade e o tempo de resposta dependem da máquina e do modelo escolhido.
