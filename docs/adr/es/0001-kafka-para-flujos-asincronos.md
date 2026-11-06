# 0001. Kafka para los flujos asíncronos

Traducción de [la versión en inglés](../0001-kafka-for-asynchronous-flows.md), que es la de referencia.

Estado: aceptado

## Contexto

Tres cosas en LeAIrning son lentas o deben repartirse entre servicios, así que no pueden ejecutarse dentro de una petición HTTP:

- **Ingesta.** Extraer el texto, trocearlo y generar los embeddings de un PDF tarda de segundos a minutos y llama a una API de pago.
- **Generación de quizzes.** Una o más llamadas al LLM por documento, hasta varios minutos.
- **Borrado.** Eliminar un archivo implica limpiar S3, el almacén vectorial y las fuentes del chat, que viven en servicios distintos.

## Decisión

Los servicios se comunican mediante topics de Kafka en lugar de llamarse por HTTP:

| Topic | Productor | Consumidores |
|---|---|---|
| `file.uploaded` | file-service | ingestion-service |
| `file.ingested` | ingestion-service | file-service (marca el archivo como `READY` o fallido) |
| `file.deleted` | file-service | ingestion-service, ai-service |
| `quizz.requested` | ai-service | ai-service |
| `notification.email` | ai-service | notification-service |

Los endpoints de subida y de quizz responden al instante (el de quizz con `202 Accepted` y el id del quizz). Los archivos pasan por `UPLOADING`, `UPLOADED`, `READY` o `FAILED`; el frontend consulta la lista de quizzes cada 5 segundos mientras se genera uno.

## Consecuencias

- La latencia de la subida no depende de la API de OpenAI, y una ingesta lenta o fallida nunca bloquea al servicio que atiende al usuario.
- `file.deleted` es un reparto real: cada grupo de consumidores lo procesa por separado, y añadir consumidores no exige cambios en file-service.
- Huecos conocidos, aceptados por ahora y recogidos en el roadmap:
  - La escritura en base de datos y la publicación en Kafka no son atómicas. Un fallo entre ambas deja un archivo o un quizz atascado en `PENDING`. La solución es el patrón outbox transaccional.
  - Los consumidores no tienen topic de mensajes fallidos (DLQ), así que un mensaje que no se puede deserializar puede bloquear una partición.
  - `quizz.requested` se consume de uno en uno, así que un quizz largo retrasa al siguiente. Mientras tanto, `max.poll.interval.ms` sube a 30 minutos para evitar rebalanceos.
- Kafka añade peso operativo al entorno local (un broker en modo KRaft en `docker-compose.yml`).

## Alternativas consideradas

- **HTTP síncrono entre servicios.** Más simple, pero liga la latencia de la petición al paso más lento y pierde trabajo si un servicio está caído.
- **`@Async` o un pool de hilos en el mismo proceso.** No añade infraestructura, pero un reinicio pierde el trabajo en cola y no cruza los límites entre servicios, que es lo que necesita el reparto del borrado.
- **Una cola de trabajos sobre la base de datos.** Menos piezas, pero reimplementa a mano los grupos de consumidores y el reparto.
