# 0002. pgvector in the same Postgres

Status: accepted

## Context

Chat and quizzes retrieve chunks of the user's own documents. Each chunk needs an embedding (1536 dimensions, `text-embedding-3-small`) and metadata (`userId`, `fileId`, `chunkIndex`). Every search must be restricted to the current user's data.

## Decision

Store the embeddings in the existing Postgres with the pgvector extension, through Spring AI's `PgVectorStore` (table `vector_store`). Isolation between users is a metadata filter on `userId` applied in every retrieval, covered by its own test.

## Consequences

- No extra system to run, back up or secure. Relational data and vectors share one database and one `pgvector/pgvector` image.
- Metadata filters are plain SQL over a JSON column, so filtering by user and file needs no special vector-database feature.
- `ai-service` reads `vector_store` with SQL (to feed quizz generation), but the table is written by `ingestion-service`. A schema change on one side breaks the other at runtime. Making one service the owner of chunks and retrieval is an open question for the RAG phase.
- Filtering after an approximate (HNSW) index search can return fewer than `topK` results when many users share the index. This needs a recall test with many users before any public demo.
- Several services still share one database and one database user. Giving each service its own schema and credentials is on the roadmap.

## Alternatives considered

- **A dedicated vector database (Qdrant, Weaviate, Pinecone).** Better at large scale, but it adds a system, a sync problem between the two stores, and nothing here needs that scale. Revisit if retrieval metrics show pgvector is the bottleneck.
- **Embeddings kept in memory.** Lost on restart, and not shareable between `ingestion-service` and `ai-service`.
