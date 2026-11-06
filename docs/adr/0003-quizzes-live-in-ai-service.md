# 0003. Quizzes live in ai-service

Status: accepted

## Context

Quizzes are generated from an ingested document, stored (quizz, questions, attempts) and graded. The roadmap could have called for a separate `quizz-service`.

## Decision

Keep quizzes in `ai-service`, in their own package (`quizz`), separate from `chat`. Inside the package:

- `QuizzGenerator` only talks to the model and validates its output.
- `QuizzService` handles persistence and publishing `quizz.requested`.
- `QuizzListener` consumes the request and runs the generator.

Generation is asynchronous (see [ADR 0001](0001-kafka-for-asynchronous-flows.md)). Attempts are graded on the server; the quizz view the client receives has no correct answers; they come back only in the result of an attempt.

## Consequences

- One deployable owns everything that calls the language model: same OpenAI configuration, same observability, and one place to add cost limits and evals.
- Quizz generation reads the document's chunks from the vector store, which `ai-service` already queries for chat. A separate service would need its own access to the same data.
- Quizzes and chat scale together. Generation runs on a Kafka consumer and does not block chat requests, but a long quizz still takes a consumer slot, so concurrency for `quizz.requested` has to be raised before heavy use.
- The package boundary keeps extraction possible: nothing outside `quizz` depends on its classes.

## Alternatives considered

- **A separate `quizz-service`.** Clearer ownership, but one more deployable, more configuration, and duplicated model and vector-store access for roughly the same code. `notification-service` is already as small as a service should get.
- **Generating quizzes inside the HTTP request.** Rejected: it takes minutes and would tie up a request thread and the client.
