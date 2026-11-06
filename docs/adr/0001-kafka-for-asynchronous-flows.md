# 0001. Kafka for asynchronous flows

Status: accepted

## Context

Three things in LeAIrning are slow or must fan out, so they cannot run inside an HTTP request:

- **Ingestion.** Extracting text, chunking and embedding a PDF takes seconds to minutes and calls a paid API.
- **Quizz generation.** One or more LLM calls per document, up to several minutes.
- **Deletion.** Removing a file must clean up S3, the vector store and the chat sources, which live in different services.

## Decision

Services communicate through Kafka topics instead of calling each other over HTTP:

| Topic | Producer | Consumers |
|---|---|---|
| `file.uploaded` | file-service | ingestion-service |
| `file.ingested` | ingestion-service | file-service (marks the file `READY` or failed) |
| `file.deleted` | file-service | ingestion-service, ai-service |
| `quizz.requested` | ai-service | ai-service |
| `notification.email` | ai-service | notification-service |

The upload and quizz endpoints answer immediately (the quizz endpoint with `202 Accepted` and the quizz id). Files move through `UPLOADING`, `UPLOADED`, `READY` or `FAILED`; the frontend polls the quizz list every 5 seconds while a quizz is generating.

## Consequences

- Upload latency does not depend on the OpenAI API, and a slow or failing ingestion never blocks the user-facing service.
- `file.deleted` is a real fan-out: each consumer group handles it independently, and new consumers need no change in file-service.
- Known gaps, accepted for now and tracked in the roadmap:
  - The database write and the Kafka publish are not atomic. A crash between them leaves a file or quizz stuck in `PENDING`. The fix is the transactional outbox pattern.
  - Consumers have no dead-letter topic, so a message that cannot be deserialized can block a partition.
  - `quizz.requested` is consumed one message at a time, so a long quizz delays the next one. `max.poll.interval.ms` is raised to 30 minutes to avoid rebalances meanwhile.
- Running Kafka adds operational weight to the local setup (one broker in KRaft mode in `docker-compose.yml`).

## Alternatives considered

- **Synchronous HTTP between services.** Simpler, but ties request latency to the slowest step and loses work if a service is down.
- **In-process `@Async` or a thread pool.** No new infrastructure, but a restart loses queued work and it does not cross service boundaries, which the deletion fan-out needs.
- **A database-backed job queue.** Fewer moving parts, but it reimplements consumer groups and fan-out by hand.
