# 0002. pgvector en el mismo Postgres

Traducción de [la versión en inglés](../0002-pgvector-in-the-same-postgres.md), que es la de referencia.

Estado: aceptado

## Contexto

El chat y los quizzes recuperan fragmentos de los documentos del propio usuario. Cada fragmento necesita un embedding (1536 dimensiones, `text-embedding-3-small`) y metadatos (`userId`, `fileId`, `chunkIndex`). Cada búsqueda debe limitarse a los datos del usuario actual.

## Decisión

Guardar los embeddings en el Postgres existente con la extensión pgvector, mediante el `PgVectorStore` de Spring AI (tabla `vector_store`). El aislamiento entre usuarios es un filtro de metadatos por `userId` aplicado en cada recuperación y cubierto por su propio test.

## Consecuencias

- No hay un sistema extra que ejecutar, respaldar o proteger. Los datos relacionales y los vectores comparten una base de datos y una imagen `pgvector/pgvector`.
- Los filtros de metadatos son SQL sobre una columna JSON, así que filtrar por usuario y archivo no exige ninguna función especial de una base vectorial.
- `ai-service` lee `vector_store` con SQL (para alimentar la generación de quizzes), pero la tabla la escribe `ingestion-service`. Un cambio de esquema en un lado rompe el otro en ejecución. Que un único servicio sea dueño de los chunks y de la recuperación es una pregunta abierta para la fase de RAG.
- Filtrar después de una búsqueda con índice aproximado (HNSW) puede devolver menos de `topK` resultados cuando muchos usuarios comparten el índice. Hace falta una prueba de recall con muchos usuarios antes de cualquier demo pública.
- Varios servicios siguen compartiendo una base de datos y un usuario de base de datos. Dar a cada servicio su propio esquema y credenciales está en el roadmap.

## Alternativas consideradas

- **Una base vectorial dedicada (Qdrant, Weaviate, Pinecone).** Mejor a gran escala, pero añade un sistema, un problema de sincronización entre los dos almacenes, y nada aquí necesita esa escala. Se revisaría si las métricas de recuperación muestran que pgvector es el cuello de botella.
- **Embeddings en memoria.** Se pierden al reiniciar y no se pueden compartir entre `ingestion-service` y `ai-service`.
