# 0003. Los quizzes viven en ai-service

Traducción de [la versión en inglés](../0003-quizzes-live-in-ai-service.md), que es la de referencia.

Estado: aceptado

## Contexto

Los quizzes se generan a partir de un documento ingerido, se guardan (quizz, preguntas, intentos) y se corrigen. El roadmap podría haber pedido un `quizz-service` aparte.

## Decisión

Mantener los quizzes en `ai-service`, en su propio paquete (`quizz`), separado de `chat`. Dentro del paquete:

- `QuizzGenerator` solo habla con el modelo y valida su salida.
- `QuizzService` se encarga de la persistencia y de publicar `quizz.requested`.
- `QuizzListener` consume la petición y ejecuta el generador.

La generación es asíncrona (ver [ADR 0001](0001-kafka-para-flujos-asincronos.md)). Los intentos se corrigen en el servidor; la vista del quizz que recibe el cliente no incluye las respuestas correctas, que solo vuelven en el resultado de un intento.

## Consecuencias

- Un único desplegable es dueño de todo lo que llama al modelo de lenguaje: misma configuración de OpenAI, misma observabilidad y un solo lugar donde añadir límites de coste y evaluaciones.
- La generación de quizzes lee los chunks del documento del almacén vectorial, que `ai-service` ya consulta para el chat. Un servicio aparte necesitaría su propio acceso a los mismos datos.
- Los quizzes y el chat escalan juntos. La generación corre en un consumidor de Kafka y no bloquea las peticiones del chat, pero un quizz largo sigue ocupando un hueco del consumidor, así que hay que subir la concurrencia de `quizz.requested` antes de un uso intensivo.
- La frontera del paquete mantiene viable una extracción: nada fuera de `quizz` depende de sus clases.

## Alternativas consideradas

- **Un `quizz-service` aparte.** Propiedad más clara, pero un desplegable más, más configuración y acceso duplicado al modelo y al almacén vectorial para casi el mismo código. `notification-service` ya es tan pequeño como debería ser un servicio.
- **Generar los quizzes dentro de la petición HTTP.** Descartado: tarda minutos y ocuparía un hilo de petición y al cliente.
