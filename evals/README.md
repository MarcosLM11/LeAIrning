# Evals

Reference dataset to measure retrieval and answer quality.

- `corpus/`: RFC 7519 (JWT), RFC 8259 (JSON) and RFC 6455 (WebSocket), from rfc-editor.org. RFCs can be redistributed under the IETF Trust legal provisions.
- `questions.jsonl`: one question per line.
  - `type`: `answerable` or `unanswerable` (the corpus does not answer it; the system should abstain).
  - `doc`: file in `corpus/` that holds the answer.
  - `gold_passages`: verbatim texts that answer the question; list every distinct passage of the document that does. A retrieved chunk is a hit if it contains any of them after collapsing whitespace. They are text, not chunk ids, so the dataset survives chunking changes.
  - `answer`: reference answer, for the generation metrics.

Each gold passage was checked against the text PDFBox extracts, the same reader the ingestion uses, and each appears exactly once in its document.

## Running

Both evals need `OPENAI_API_KEY`, Docker and JDK 27, and are skipped by a plain `mvn test`:

```bash
cd ingestion-service && OPENAI_API_KEY=... ./mvnw test -Peval   # evals/results/retrieval.json
cd ai-service && OPENAI_API_KEY=... ./mvnw test -Peval          # evals/results/generation.json
```

`generation.json` has faithfulness (claims supported by the retrieved chunks, over answers that did not abstain), correctness against `answer`, false abstention on answerable questions, and abstention on unanswerable ones. Each question runs 3 times and an LLM judge grades it. `generation-details.json` keeps every answer and verdict so the judge can be spot-checked by hand, plus how many chunks the model saw and whether one held a reference passage (`goldInContext`). `goldInContext` only knows the listed `gold_passages`, so a chunk with another valid passage counts as a miss.
