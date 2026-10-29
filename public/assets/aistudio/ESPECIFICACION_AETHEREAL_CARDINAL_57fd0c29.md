---
title: "AETHEREAL CARDINAL"
subtitle: "Especificación técnica de corrección y estabilización"
author: "Especificación de implementación y criterios de aceptación"
date: "28 de octubre de 2026"
lang: es-MX
---

**Versión:** 1.0  
**Proyecto actual:** archivo `ae-cardinal.zip` entregado para revisión  
**Proyecto de referencia:** <https://github.com/yashab-cyber/opendroid>  
**Plataforma objetivo:** Android nativo, Kotlin y Jetpack Compose

---

# 0. Instrucción principal al agente de programación

Actúa como ingeniero Android senior responsable de corregir el proyecto AETHEREAL CARDINAL, no como consultor que solamente propone cambios. Inspecciona el proyecto disponible en el entorno de trabajo y ejecuta las modificaciones, pruebas y verificaciones descritas en este documento.

El resultado debe ser una aplicación Android mantenible y verificable, basada fielmente en OpenDroid en la medida que lo permita el repositorio original, con identidad propia AETHEREAL CARDINAL y con un runtime que no confíe ciegamente en las decisiones del modelo de IA.

**No te limites a entregar un plan.** Primero establece una línea base; después corrige por fases; ejecuta pruebas; inspecciona los resultados y entrega un informe final que indique qué se verificó, qué no pudo verificarse y por qué.

Reglas obligatorias:

1. No inventes archivos, funciones, pruebas, resultados de compilación ni capacidades.
2. No digas que una función funciona únicamente porque existe una clase con ese nombre.
3. No sustituyas implementaciones funcionales por mocks, placeholders o reglas simplificadas para conseguir que compile.
4. No elimines funcionalidades existentes sin identificar su alcance, explicar la necesidad y demostrar que la alternativa conserva el comportamiento.
5. No hagas una refactorización general al mismo tiempo que una corrección crítica, salvo que sea necesaria para resolver una dependencia técnica.
6. No ejecutes acciones del dispositivo de manera directa desde la salida del LLM. Todas las acciones deben atravesar validación, política de permisos, ejecución controlada y verificación.
7. No expongas API keys, datos sensibles de pantalla, mensajes privados o datos personales en logs.
8. Conserva la atribución, licencias y avisos del proyecto original y de sus dependencias.
9. Mantén la documentación propia del repositorio en un máximo de cinco archivos Markdown, consolidando información cuando tenga sentido. Los avisos y licencias legales obligatorios pueden conservar sus formatos correspondientes.
10. Ante un bloqueo externo real, continúa con las partes independientes del bloqueo y documenta el impedimento con evidencia. No simules que se completó el trabajo.

# 1. Contexto y hallazgos que deben verificarse

### Hallazgos que deben comprobarse y corregirse

Usa los siguientes puntos como hipótesis concretas. Si el código actual difiere porque ya fue modificado, registra la diferencia y conserva el requisito funcional.

1. **Procedencia del código.** El ZIP no incluye metadatos `.git` que acrediten su relación con OpenDroid; algunas clases tienen nombres propios parecidos a las capas de un agente. Compara código y commits del upstream antes de declarar que es un fork fiel.
2. **Compilación.** No se encontraron `settings.gradle.kts`, `build.gradle.kts` raíz, `gradlew`, `gradlew.bat` ni `gradle-wrapper.jar`, aunque sí existe `gradle/wrapper/gradle-wrapper.properties`. Restaura una configuración Gradle completa y compatible, sin inventar versiones.
3. **Identidad Android.** El módulo declara `namespace = "com.example"` y `applicationId = "com.aistudio.cardinal.acrdnl"`, mientras el código usa paquetes `com.example`. Unifica la identidad en `com.aethereal.cardinal`, salvo incompatibilidad real y documentada.
4. **AgentLoop.** No se observa manejo integral de excepciones/cancelación y `isPaused` es un booleano compartido. Implementa control de concurrencia, transiciones válidas y finalización garantizada.
5. **Verificación.** `PlanManager.evaluateStep()` considera exitoso un paso si `ActionResult.isSuccess` es verdadero. Separa ejecución técnica de la verificación de la postcondición u objetivo.
6. **Recuperación.** La reinserción de pasos alternativos modifica una lista mutable y sus índices; no se identifica un presupuesto sólido de replanificación. Añade IDs estables, contabilidad de intentos y protección contra bucles.
7. **Accesibilidad.** La resolución de elementos ambiguos, obsoletos o no accionables debe reforzarse y el recorrido del árbol debe estar limitado. Implementa snapshots acotados, resolución semántica única y revalidación inmediata.
8. **Visión.** `VisionEngine` anuncia percepción de dos niveles, pero el código inspeccionado no demuestra captura visual real y podría producir contexto simulado cuando el servicio no está disponible. Implementa solo capacidades reales y comunica explícitamente el modo degradado.
9. **Herramientas.** `ActionRegistry` anuncia unas 27 herramientas, pero su presencia en el catálogo no demuestra que cada una cumpla la función descrita. Audita individualmente parámetros, riesgos, permisos, idempotencia, timeouts, efectos y verificación.
10. **Proveedor LLM.** Existe implementación de Gemini y fallback determinista; esto no demuestra que todos los proveedores mencionados estén implementados. Expón solo integraciones funcionales y clasifica errores/fallbacks.
11. **Secretos.** La inspección detectó configuración sensible sin protección suficiente. No almacenes claves en texto plano ni las incluyas en el APK, repositorio o logs.
12. **Persistencia.** `MemoryStore` guarda resúmenes en `SharedPreferences`/JSON, no restaura el plan completo y puede silenciar errores. Room aparece como dependencia, pero no se acreditó una implementación completa. Persiste de forma fiable misiones, pasos, intentos y eventos; migra y prueba datos existentes.
13. **Pruebas.** Las pruebas actuales cubren solo una parte del comportamiento. Añade cobertura de cancelación, recuperación, permisos, objetivos UI obsoletos y verificación posterior.
14. **Documentación.** El README conserva instrucciones de Google AI Studio y pasos manuales. Actualízalo para AETHEREAL CARDINAL y elimina instrucciones que ya no describan el proceso real.

# 2. Objetivos, límites y prioridades

## 2.1 Objetivos obligatorios

- Recuperar un proceso de compilación Android reproducible.
- Establecer una identidad de aplicación coherente e independiente.
- Determinar qué código proviene realmente de OpenDroid y conservar las funcionalidades útiles de upstream.
- Corregir el ciclo de misión para que estados, errores, pausa, reanudación y cancelación sean coherentes.
- Introducir validación determinista entre el plan del LLM y la ejecución.
- Priorizar la accesibilidad semántica sobre las coordenadas de pantalla.
- Separar claramente percepción, planificación, validación, ejecución, verificación, persistencia y presentación.
- Hacer que logs e historial expliquen acciones, decisiones operativas y fallos sin almacenar razonamiento privado del modelo.
- Añadir pruebas que detecten regresiones antes de dar por terminada una fase.

## 2.2 Fuera del alcance de esta corrección

No se debe intentar crear autonomía avanzada nueva, entrenar modelos, añadir múltiples proveedores que no existan en upstream, construir una memoria vectorial, rehacer completamente la UI ni introducir una arquitectura distribuida. Primero hay que estabilizar lo que ya existe y restaurar la fidelidad al código de origen.

## 2.3 Orden de prioridades

1. Compilación reproducible e identidad correcta.
2. Seguridad operacional y control de estado.
3. Percepción y acciones de accesibilidad fiables.
4. Verificación y recuperación controlada.
5. Persistencia, secretos, proveedores y observabilidad.
6. Pruebas, documentación y entrega.

# 3. Fase 0: procedencia, auditoría y preservación de upstream

## 3.1 Obtener y comparar OpenDroid

1. Intenta obtener el repositorio `https://github.com/yashab-cyber/opendroid` con Git o consulta el código fuente y sus instrucciones oficiales.
2. Registra URL, rama, commit hash, fecha de acceso y licencia observada.
3. Identifica el commit o versión más cercana al proyecto actual. Si no existe una relación demostrable, no inventes un historial de fork.
4. Compara las estructuras de archivos y los contenidos de las áreas equivalentes: agente, planificación, LLM, acciones, accesibilidad, percepción, memoria, UI, pruebas y configuración Gradle.
5. Produce un inventario de archivos: `upstream sin cambios`, `upstream modificados`, `nuevos en Cardinal`, `omitidos o ausentes`.
6. Comprueba si el proyecto actual es una reconstrucción parcial, una plantilla generada por AI Studio o una adaptación directa del upstream.
7. Si falta parte sustancial del código original, recupera el código upstream y re-aplica de forma controlada el rebranding y los cambios propios que puedan preservarse. No reemplaces automáticamente la UI personalizada sin revisar su valor.
8. Conserva la licencia, copyright y avisos de terceros. Añade una nota de procedencia en el README.

## 3.2 No falsear la fidelidad

El entregable debe declarar honestamente si es:

- un fork directo con modificaciones localizadas;
- un fork parcialmente reconstruido;
- o una implementación inspirada en OpenDroid.

No debe etiquetarse como clon fiel si no hay evidencia de conservación del código y el comportamiento originales. Si el objetivo es mantener funcionalidades de OpenDroid, define equivalencias funcionales y pruebas que las demuestren.

## 3.3 Línea base

Antes de tocar el código funcional:

- registra los archivos existentes y su estado;
- intenta ejecutar la construcción disponible y guarda el error real;
- ejecuta las pruebas actuales y registra cuáles compilan y cuáles pasan;
- registra los recursos visuales y las pantallas existentes para no perderlas accidentalmente;
- identifica dependencias sin uso, claves, archivos generados y configuraciones de AI Studio.

**Criterio de salida de fase:** existe un informe breve de procedencia y línea base; los bloqueos iniciales se distinguen de los errores introducidos por los cambios posteriores.

# 4. Fase 1: restaurar una compilación reproducible

## 4.1 Archivos Gradle necesarios

El archivo recibido incluye `gradle/wrapper/gradle-wrapper.properties`, pero no los scripts del wrapper ni todos los archivos de Gradle del nivel raíz. Recupera o genera, según corresponda:

- `settings.gradle.kts`;
- `build.gradle.kts` del proyecto raíz;
- `gradlew`;
- `gradlew.bat`;
- `gradle/wrapper/gradle-wrapper.jar`;
- cualquier archivo mínimo requerido para resolver plugins y módulos.

La mejor fuente para recuperar estos archivos es el upstream correspondiente al commit seleccionado. Si se generan de nuevo, documenta por qué no puede utilizarse la configuración de origen.

## 4.2 Compatibilidad de versiones

No cambies versiones al azar ni actualices todas las dependencias de una sola vez. Evalúa de forma conjunta:

- Gradle Wrapper;
- Android Gradle Plugin (AGP);
- Kotlin y el plugin Compose;
- KSP;
- JDK requerido;
- compileSdk, targetSdk y minSdk;
- plugins de Google Services y Secrets;
- procesadores de anotaciones y dependencias AndroidX.

Usa una combinación mutuamente compatible respaldada por la documentación oficial y por una compilación real. Conserva versiones del proyecto de origen siempre que sean viables.

## 4.3 Reproducibilidad y secretos de construcción

- `gradlew` debe funcionar sin depender de una instalación global de Gradle.
- No incluyas `local.properties`, keystores reales, contraseñas o `.env` con secretos.
- Mantén `.env.example` solo con nombres y valores ficticios.
- Verifica que un clon limpio pueda resolver dependencias y ejecutar la compilación con los requisitos documentados.
- No uses una clave de firma de producción inventada ni el debug keystore para afirmar que hay una entrega de release firmada.
- No borres dependencias simplemente para evitar resolver un error; comprueba primero si son necesarias para funciones existentes.

## 4.4 Comandos mínimos de aceptación

Desde el directorio raíz, registra y ejecuta los equivalentes apropiados del wrapper:

```bash
./gradlew --version
./gradlew tasks
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Si el entorno tiene Android SDK y dispositivo o emulador, ejecuta también las pruebas instrumentadas pertinentes. Si el entorno no permite una de estas comprobaciones, adjunta el error exacto y marca la comprobación como bloqueada, no como aprobada.

**Criterio de salida de fase:** el proyecto tiene todos los archivos necesarios para una construcción limpia; la compilación de debug pasa o queda bloqueada por un requisito externo claramente demostrado.

# 5. Fase 2: identidad y rebranding de Android

## 5.1 Identidad canónica

Usa como propuesta canónica:

- nombre visible: `AETHEREAL CARDINAL`;
- `applicationId`: `com.aethereal.cardinal`;
- `namespace`: `com.aethereal.cardinal`;
- paquete Kotlin base: `com.aethereal.cardinal`.

Aplica estos valores de manera coherente. Si alguna incompatibilidad real obliga a usar otro identificador, aporta la evidencia y documenta la excepción antes de aplicarla. No mantengas sin motivo una combinación de `com.example`, `com.aistudio.cardinal.acrdnl` y nombres de paquete distintos.

## 5.2 Migración completa

Busca referencias tanto en código como en recursos, pruebas, manifest y configuración:

- declaraciones `package` e imports Kotlin;
- `namespace` y `applicationId`;
- clases y nombres relativos del Manifest;
- referencias `R`, nombres de recursos y tests;
- `provider authorities`, deep links e intent filters, si existen;
- claves de preferencias con nombres ligados al paquete;
- referencias al paquete dentro de scripts, documentación y CI;
- configuración de notificaciones, servicios y accesibilidad.

No hagas un reemplazo global indiscriminado de `OpenDroid`. Conserva nombres de bibliotecas externas y avisos de procedencia que legalmente deban permanecer.

## 5.3 Manifest y servicio de accesibilidad

Verifica que:

- `MainActivity` se resuelve después de la migración;
- `CardinalAccessibilityService` tiene el nombre completo correcto;
- el servicio utiliza `android.permission.BIND_ACCESSIBILITY_SERVICE` según corresponda;
- el filtro y los metadatos de accesibilidad están presentes;
- solo se declaran permisos realmente necesarios;
- ninguna capacidad se considera habilitada únicamente porque el permiso aparece en el Manifest.

## 5.4 Instalación independiente

El identificador de paquete debe permitir instalar AETHEREAL CARDINAL junto a OpenDroid sin sobrescribirlo, salvo que el usuario elija expresamente lo contrario. Verifica el `applicationId` del APK final con herramientas de Android SDK cuando estén disponibles.

**Criterio de salida de fase:** no quedan identificadores de aplicación contradictorios y el APK de debug presenta el nombre, el icono, el launcher y el package ID esperados.

# 6. Fase 3: arquitectura de ejecución y estados

## 6.1 Separación de responsabilidades

Mantén la separación lógica siguiente, usando los nombres y paquetes existentes si ya cumplen la función:

1. **Perception:** produce un snapshot explícito del estado de pantalla.
2. **Planner:** propone un plan; no ejecuta acciones ni concede permisos.
3. **PlanValidator:** comprueba estructura, límites, herramientas y parámetros.
4. **Policy/Permission Gate:** decide si la operación está permitida o requiere confirmación.
5. **ActionExecutor:** ejecuta una herramienta validada con timeout y cancelación.
6. **ResultVerifier:** determina si se logró la postcondición, no solo si la llamada terminó sin error.
7. **Mission State Reducer/Controller:** mantiene transiciones válidas de misión y paso.
8. **Persistence:** almacena misiones, pasos e historial de forma consistente.
9. **Observability:** produce eventos útiles para depuración y para la UI.

La capa de interfaz no debe contener lógica de ejecución de herramientas. `AccessibilityService` debe actuar como adaptador de capacidades Android, no como planificador ni almacén de todo el estado del agente.

## 6.2 Ciclo obligatorio

Implementa y registra de manera explícita:

`OBSERVE → PLAN → VALIDATE PLAN → VALIDATE ACTION → EXECUTE → VERIFY → UPDATE STATE → CONTINUE / REPLAN / FAIL`

No se ejecuta ninguna herramienta a partir de texto libre del LLM. Cada paso que vaya a ejecutarse debe corresponder a una herramienta registrada y pasar validaciones deterministas.

## 6.3 Concurrencia y propiedad del ciclo de vida

- Sustituye el booleano compartido `isPaused` por un mecanismo sincronizado y observable, por ejemplo un estado de control respaldado por `StateFlow`, `Mutex` y/o un gate suspendible.
- El estado del control debe ser consistente si se pulsa pausar y reanudar rápidamente o al mismo tiempo que termina una acción.
- Evita `CoroutineScope(Dispatchers.Default)` sin un propietario de ciclo de vida. Usa un scope de aplicación/servicio/ViewModel adecuado con `SupervisorJob`, inyección de scope o un mecanismo explícito de cierre.
- No retengas una `Activity` como `Context` dentro de un objeto de larga vida. Usa `applicationContext` cuando corresponda.
- Evita que dos misiones se ejecuten simultáneamente sobre la misma pantalla si el runtime no admite concurrencia de forma explícita.
- Iniciar una nueva misión no debe cancelar silenciosamente otra en curso. Rechaza la solicitud con estado `BUSY` o exige una confirmación explícita para cancelar/sustituir la misión activa.

## 6.4 Estados y transiciones válidas

Conserva los estados existentes cuando sea posible, pero define una máquina de estados clara. Mínimo para misión:

- `IDLE`;
- `PLANNING`;
- `RUNNING`;
- `PAUSED`;
- `COMPLETED`;
- `FAILED`;
- `ABORTED`.

Documenta las transiciones permitidas. Requisitos:

- `COMPLETED`, `FAILED` y `ABORTED` son terminales para ese `missionId`.
- Un estado terminal no debe volver a `RUNNING` por una llamada tardía a resume.
- `PAUSED` solo puede reanudarse si la misión no ha sido cancelada o terminada.
- `PLANNING` también puede fallar o cancelarse y debe finalizar con estado terminal correcto.
- Todos los caminos, incluidos error, timeout y cancelación, deben actualizar estado, duración y log final.
- Las transiciones deben centralizarse en una función/controlador y rechazarse o registrarse si son inválidas.

## 6.5 Cancelación y excepciones

En el job de misión:

- captura `CancellationException` solo para completar limpieza y persistencia y luego relánzala; no la conviertas en un error normal ni la tragues;
- gestiona otras excepciones de manera estructurada y asocia el error al `missionId` y `stepId`;
- utiliza `try/finally` para liberar recursos, finalizar indicadores y persistir el estado;
- verifica que cancelar detenga el trabajo pendiente, incluidos delays y peticiones cooperativas;
- si una operación del sistema no puede interrumpirse a mitad de ejecución, registra ese hecho y verifica el estado después;
- diferencia la cancelación solicitada por el usuario de un fallo interno.

## 6.6 Estado atómico

Usa actualizaciones atómicas/centralizadas de `StateFlow` (por ejemplo `update`) o un reducer serializado. No hagas modificaciones concurrentes independientes sobre copias de listas. Persistencia y UI deben recibir un estado coherente.

**Criterio de salida de fase:** no quedan caminos conocidos que dejen una misión en `RUNNING` después de terminar, que reactiven una misión terminal o que ejecuten dos misiones en conflicto.

# 7. Fase 4: planificador, validación y recuperación

## 7.1 Esquema de plan explícito

Define y valida un formato estructurado para cada paso. Como mínimo:

- ID estable de paso;
- nombre de herramienta;
- título y descripción;
- parámetros tipados o un objeto JSON validable;
- precondiciones cuando proceda;
- resultado esperado/postcondición;
- nivel de riesgo;
- regla de verificación;
- política de retry/idempotencia.

No confíes en `Map<String, String>` como única barrera de validación. Puede mantenerse para compatibilidad, pero la entrada debe validar tipos, campos requeridos, rangos, longitud y campos no admitidos antes de ejecutar.

## 7.2 Validación del plan generado por el LLM

Rechaza o repara de manera acotada un plan que contenga:

- herramientas inexistentes;
- argumentos faltantes o de tipos inválidos;
- más pasos que el límite configurado;
- ciclos evidentes o repeticiones improductivas;
- una acción peligrosa sin el consentimiento requerido;
- acciones que no se puedan verificar de ninguna forma razonable;
- referencias a elementos de una pantalla que ya no corresponda a la observación actual.

Las salidas del LLM son datos no confiables. Analiza JSON con un parser, valida el esquema y maneja JSON truncado, texto adicional, nulls y campos inesperados sin crash.

## 7.3 Ejecución, éxito y postcondición

Cambia el significado de éxito para distinguir, al menos, los siguientes resultados:

- `EXECUTION_SUCCEEDED`: la llamada a la herramienta se ejecutó técnicamente;
- `VERIFIED`: se comprobó la postcondición;
- `NOT_VERIFIED`: no hay evidencia suficiente;
- `VERIFICATION_FAILED`: la condición esperada no se alcanzó;
- `NEEDS_USER_CONFIRMATION`: la operación exige intervención del usuario;
- `EXECUTION_FAILED`: la llamada falló.

Una herramienta que devuelva `isSuccess = true` no puede, por sí sola, marcar el paso o la misión como completados. Por ejemplo, abrir la página de Wi-Fi no equivale a activar Wi-Fi.

`MissionStatus.COMPLETED` solo se permite cuando el criterio de finalización del objetivo se verifica, o cuando el plan define explícitamente un objetivo de mera ejecución que no promete más que iniciar una acción. Si solo fue posible ejecutar pero no verificar, informa `NOT_VERIFIED` y muestra qué falta comprobar; no muestres éxito garantizado.

## 7.4 Estado de los pasos y replanificación

- Usa `stepId` estable; no utilices la posición de una lista mutable como identidad del paso.
- Deriva el índice visual desde el orden del plan o mantenlo mediante una operación de reducer coherente.
- Al fallar un paso, marca su estado de forma explícita, conserva el motivo y la cantidad de intentos y enlaza el paso reemplazo al paso que lo originó.
- No marques un paso como completado únicamente porque se insertó una alternativa.
- Al modificar el plan, actualiza el total, el índice actual, el progreso y la persistencia en una sola operación coherente.
- Antes de reintentar una acción con efectos externos, vuelve a observar el estado para comprobar si la primera ejecución surtió efecto pese a devolver error.

## 7.5 Límites contra bucles y tormentas de llamadas

Implementa límites configurables. Valores iniciales propuestos para la primera versión corregida:

- máximo 30 pasos en un plan;
- máximo 3 intentos de recuperación/replanificación por paso;
- máximo 10 replanificaciones por misión;
- máximo 10 minutos por misión por defecto, configurable;
- timeout por defecto para herramientas ordinarias de 15 segundos, ajustado por herramienta;
- timeout de proveedor LLM de 45 segundos por solicitud, configurable;
- número máximo de peticiones por misión y presupuesto de tokens configurable cuando el proveedor informe su uso.

Los límites deben vivir en una configuración central, no repartidos como números mágicos por el código. Si se alcanza un límite, termina en `FAILED` o `ABORTED` con razón `BUDGET_EXCEEDED` y un evento explícito. No ocultes el límite ni sigas intentando indefinidamente.

## 7.6 Retry versus replan

- **Retry**: repetir la misma operación solo ante errores transitorios y si es seguro por idempotencia o el estado se ha revalidado.
- **Reobserve**: volver a obtener una instantánea de UI tras un cambio de pantalla, timeout o sospecha de nodo obsoleto.
- **Revalidate**: comprobar otra vez herramienta, parámetros, permisos y elemento objetivo.
- **Replan**: generar un nuevo paso cuando el paso original no pueda completarse y haya evidencia suficiente de estado actual.
- **Resume**: solo después de existir una política explícita sobre checkpoint y operaciones pendientes. No afirmes que es una reanudación durable si el checkpoint no persiste.
- **Cancel**: detener la misión de forma terminal y cooperativa.

No implementes una recuperación simulada que solo cambie el texto del log sin modificar el comportamiento del runtime.

**Criterio de salida de fase:** se puede probar con dobles de prueba que las herramientas desconocidas no se ejecutan, los planes inválidos se rechazan, los límites se cumplen y una recuperación no produce un bucle infinito.

# 8. Fase 5: percepción y accesibilidad semántica

## 8.1 Modelo de instantánea de percepción

`capturePerception()` debe devolver un resultado real y descriptivo, no un objeto ficticio. La instantánea debe incluir, cuando el sistema pueda obtenerlo:

- timestamp;
- paquete de la app activa y nombre de ventana, cuando estén disponibles;
- fuente de percepción (`ACCESSIBILITY_TREE`, `SCREENSHOT`, `HYBRID`, `UNAVAILABLE`);
- capacidades disponibles y motivo de las no disponibles;
- jerarquía de nodos normalizada;
- texto visible, content description, resource/view ID y clase;
- roles, clickable, long-clickable, editable, scrollable, focused, selected, enabled y visible;
- límites en pantalla, ruta o contexto de padres e índice entre hermanos;
- indicador de truncamiento si se alcanzó un límite.

No retengas objetos `AccessibilityNodeInfo` a través de esperas de red o del planificador. Guarda una representación inmutable y vuelve a resolver el nodo real justo antes de actuar.

## 8.2 Recorrido limitado y seguro

El recorrido del árbol tiene que estar acotado en número de nodos, profundidad, longitud textual y tiempo. Establece valores configurables iniciales, por ejemplo 1.000 nodos, profundidad 30 y texto por nodo limitado a 512 caracteres. Si se trunca, marca `truncated=true` para que el planner sepa que la percepción es parcial.

El recorrido debe:

- evitar ciclos o referencias repetidas si la API lo permite;
- limitar memoria y coste por snapshot;
- cerrar/liberar los objetos que correspondan según la API y el nivel Android;
- evitar registrar textos sensibles completos;
- distinguir nodo ausente de nodo no accesible o árbol vacío;
- tolerar cambios de ventana mientras se recorre el árbol.

## 8.3 Resolución de objetivos semánticos

Prioriza, en este orden general, identificadores de recurso inequívocos, etiquetas y descripciones accesibles, rol/clase y contexto de jerarquía. El texto por sí solo no es identidad suficiente si hay varios nodos con la misma etiqueta.

Para cada acción:

1. toma una observación reciente;
2. resuelve el candidato con reglas deterministas;
3. comprueba que existe exactamente una coincidencia suficientemente fiable;
4. verifica `visible`, `enabled` y que soporte la acción solicitada;
5. verifica que la app/ventana activa siga correspondiendo;
6. si hay ambigüedad o cambio, vuelve a observar o solicita aclaración;
7. ejecuta y después verifica el efecto.

Nunca elijas simplemente el primer nodo si hay varias coincidencias válidas. Nunca ejecutes sobre un nodo cacheado que puede haber quedado obsoleto.

Si se usan puntuaciones de confianza, documenta cómo se calculan, qué campos las sustentan y qué sucede en cada banda. La confianza no puede sustituir comprobaciones deterministas de permisos, visibilidad o habilitación.

## 8.4 Gestos, tap y scroll

- Prefiere `ACTION_CLICK` en el nodo semántico cuando el elemento sea accionable.
- Utiliza coordenadas solo como alternativa explícita y justificada, cuando el objetivo haya sido localizado con evidencia vigente y la acción sea segura.
- Valida límites contra el tamaño real de pantalla y contempla insets, barras del sistema, densidad y rotación.
- En scroll, identifica el contenedor scrollable correcto; no desplaces siempre la raíz de la ventana.
- Tras un gesto, espera de forma limitada al cambio esperado y vuelve a observar.
- Distingue `NODE_NOT_FOUND`, `AMBIGUOUS_TARGET`, `STALE_SNAPSHOT`, `NOT_VISIBLE`, `DISABLED`, `UNSUPPORTED_ACTION`, `GESTURE_REJECTED` y `POSTCONDITION_FAILED`.

## 8.5 Visión real y modo degradado

El código no puede describir como “dual-tier vision” un flujo que únicamente lee nodos de accesibilidad o produce una percepción simulada.

- Primero utiliza Accessibility APIs.
- Implementa captura de pantalla solo por APIs Android oficialmente disponibles para el nivel del dispositivo y con los permisos/capacidades requeridos; respeta las restricciones del sistema.
- Gestiona por nivel de API la compatibilidad real. Si el dispositivo no dispone de captura válida, utiliza el árbol de accesibilidad como modo degradado y señala la limitación.
- No declares la captura de pantalla exitosa hasta recibir una imagen válida.
- No introduzcas una solicitud de `MediaProjection` ni grabación de pantalla persistente sin UX, consentimiento y justificación claros.
- Si no se implementa aún visión, renombra la presentación y logs para decir `Accessibility perception` y marca `SCREENSHOT` como no disponible.

**Criterio de salida de fase:** la percepción nunca inventa una pantalla; los nodos obsoletos y ambiguos se rechazan; los límites de recorrido están probados; las acciones priorizan elementos semánticos.

# 9. Fase 6: catálogo de herramientas y ActionExecutor

## 9.1 Auditar todas las herramientas existentes

Recorre una por una todas las herramientas registradas en `ActionRegistry` y comprueba su implementación real en `ActionExecutor` y archivos relacionados. No asumas que son 27 para siempre: genera el inventario desde el código existente y registra el número real.

Para cada herramienta documenta:

- ID estable y descripción precisa;
- esquema de entrada, campos obligatorios y límites;
- resultado estructurado;
- permisos y capacidades Android necesarios;
- nivel de riesgo: lectura, reversible, efecto externo, sensible;
- precondiciones;
- postcondición verificable;
- idempotencia;
- timeout;
- retry permitido o prohibido;
- posibilidad de requerir confirmación humana;
- pruebas y limitaciones de compatibilidad.

Mantén el inventario en un archivo Markdown de catálogo. No añadas una herramienta si solo es un stub o si su nombre promete algo que la implementación no puede hacer.

## 9.2 Contrato de herramienta

Todas las herramientas deben pasar por una abstracción común que permita validar y registrar su ejecución. El diseño puede adaptarse a las clases existentes, pero debe representar al menos:

- `ToolSpec`: ID, descripción, esquema, riesgo, precondiciones, capacidades y política de retry;
- `ToolInvocation`: `missionId`, `stepId`, parámetros validados, intento, timestamp y timeout;
- `ToolExecutionResult`: resultado técnico, error tipado, duración y evidencia;
- `VerificationResult`: postcondición comprobada o no comprobada;
- `ToolError`: código estable, mensaje seguro para UI y detalle técnico redactado para log.

No permitas que una herramienta reciba sin validación un mapa arbitrario producido por el modelo.

## 9.3 Acciones con efectos externos

Una acción que pueda enviar mensajes, realizar llamadas, crear o modificar datos, cambiar ajustes sensibles o provocar una consecuencia externa debe tener un nivel de riesgo explícito. Exige confirmación humana cuando el efecto no sea fácilmente reversible o cuando no exista una autorización persistente clara. No interpretes una instrucción general como permiso para enviar contenido no revisado si el usuario no autorizó el envío.

Evita duplicados: antes de reintentar un envío, llamada o creación, vuelve a observar o consulta la aplicación/estado pertinente para determinar si la acción inicial pudo surtir efecto.

## 9.4 No prometer capacidades bloqueadas por Android

Inspecciona funciones como Wi-Fi, llamadas, mensajes y alarmas. Si Android limita una operación para una app convencional sin privilegios especiales, la herramienta debe abrir el flujo de sistema disponible o devolver un resultado explícito de capacidad restringida. No anuncies que cambió el estado cuando solo se abrió Ajustes.

Shizuku no debe asumirse instalado ni autorizado. Verifica la disponibilidad y el permiso en tiempo de ejecución para cada operación que lo requiera; proporciona una alternativa segura si no está disponible.

**Criterio de salida de fase:** cada herramienta tiene un comportamiento demostrado, parámetros validados, límites explícitos y resultados que diferencian fallo, éxito técnico y verificación.

# 10. Fase 7: proveedores LLM, fallback y secretos

## 10.1 Interfaz de proveedor

Mantén una interfaz común para los proveedores que realmente estén implementados. La configuración de UI y el catálogo no deben anunciar integración completa de OpenAI, Groq, Ollama u otros si solo se ha implementado Gemini.

Para cada proveedor real, verifica:

- configuración y validación de credenciales;
- creación de petición y formato de respuesta;
- timeout y cancelación;
- parseo estructurado de salida;
- tratamiento de códigos HTTP;
- límite de solicitudes y tokens cuando exista información;
- logging sin claves ni contenido privado innecesario;
- pruebas con respuestas válidas, inválidas y errores simulados.

## 10.2 Clasificación de errores

Distingue como mínimo:

- `NETWORK_ERROR`;
- `TIMEOUT`;
- `AUTHENTICATION_ERROR`;
- `RATE_LIMITED`;
- `SERVER_ERROR`;
- `INVALID_RESPONSE`;
- `SCHEMA_VALIDATION_ERROR`;
- `CANCELLED`;
- `PROVIDER_UNAVAILABLE`.

Registra código, etapa y si se aplicó fallback. No guardes cabeceras de autenticación ni claves.

## 10.3 Fallback determinista transparente

El fallback puede usarse si el proveedor falla, pero tiene que ser explícito:

- registra `FALLBACK_STARTED` y `FALLBACK_SELECTED` con motivo clasificado;
- etiqueta el origen de la decisión como `LLM` o `DETERMINISTIC_FALLBACK`;
- no muestres que el modelo generó un plan cuando el plan salió de reglas locales;
- si el patrón no está cubierto, devuelve `UNSUPPORTED_GOAL` o solicita reformular, en vez de inventar una secuencia;
- nunca permita que el fallback eluda `PlanValidator`, la política de permisos o `ResultVerifier`.

Para peticiones seguras de planificación, aplica reintentos limitados y backoff si procede. No repitas operaciones que puedan tener efectos externos de manera automática.

## 10.4 Protección de API keys

- Elimina claves reales del código, `BuildConfig`, recursos, logs, tests, README y control de versiones.
- `BuildConfig` y los recursos incluidos en un APK no son almacenes secretos: sus valores pueden extraerse.
- Solicita al usuario su propia clave en tiempo de ejecución o utiliza un backend seguro si el producto lo requiere.
- Guarda credenciales localmente solo si es necesario, cifradas mediante una clave protegida por Android Keystore y con un mecanismo de borrado/rotación.
- Si una biblioteca de cifrado elegida está obsoleta o no es compatible con el stack, utiliza una alternativa mantenida o implementa el cifrado mediante primitives y APIs soportadas; no inventes criptografía propia.
- Evita registrar prompts con contenido privado por defecto. Si se habilita logging de diagnóstico de contenido, debe ser una opción explícita y redactada.
- Mantén `.env.example` ficticio; `.env` y archivos de secretos deben estar ignorados por Git.

## 10.5 Compatibilidad y documentación

Actualiza Settings para reflejar exactamente los proveedores disponibles y sus requisitos. Si un proveedor no se implementa dentro de esta corrección, decláralo como `NOT_IMPLEMENTED`; no crees un adaptador vacío para que la UI lo muestre como habilitado.

**Criterio de salida de fase:** credenciales protegidas en reposo si se guardan, ninguna clave real empaquetada, errores diferenciados y fallback visible y probado.

# 11. Fase 8: persistencia, historial y observabilidad

## 11.1 Modelo persistente

El runtime necesita registrar más que un resumen final para ser depurable. Implementa almacenamiento estructurado para:

- misión: ID, objetivo, estado, tiempos de creación/inicio/fin, duración, resultado final y razón terminal;
- paso: ID estable, orden, herramienta, estado, número de intentos, parámetros redactados, resultado y verificación;
- evento de ejecución: tipo de evento, timestamp, misión, paso, severidad y campos seguros;
- llamada a herramienta: herramienta, duración, timeout, error y origen de decisión;
- checkpoint mínimo, si se va a anunciar reanudación durable.

Dado que el catálogo de dependencias ya incluye Room, evalúa e implementa Room para el historial estructurado si no existe una razón técnica concreta para no hacerlo. Usa entidades, DAOs, una única base de datos gestionada, operaciones transaccionales y migraciones de versión. No basta con incluir la dependencia.

Configura preferencias simples por separado del historial. DataStore o SharedPreferences pueden seguir utilizándose para preferencias no sensibles, pero no deben contener toda la base de misiones en un único JSON sin esquema.

## 11.2 Recuperación de errores de persistencia

- No ignores excepciones de lectura/escritura silenciosamente.
- Mantén la aplicación utilizable cuando el historial esté corrupto, pero registra un error técnico seguro e informa que el historial no se pudo cargar.
- Añade pruebas para almacenamiento vacío, registros inválidos, actualización de esquema y migración.
- Si hay historial legado guardado en JSON, migra de forma tolerante y de manera idempotente; no borres los datos legados antes de comprobar la migración.

## 11.3 Checkpoint y reanudación

No muestres `Resume` como una recuperación después de reiniciar el proceso hasta que exista un checkpoint persistente con misión, paso, intentos, plan actual, último snapshot de contexto pertinente y estado de las operaciones externas.

Si el checkpoint no está implementado, limita la función a reanudar una misión pausada en memoria y etiqueta la capacidad con exactitud. Después de reiniciar el proceso, marca la misión anterior como interrumpida o pendiente de reconciliación, no la reinicies automáticamente sin revisar efectos externos.

## 11.4 Logs operacionales

Usa eventos estructurados y consistentes, por ejemplo:

```text
[Mission] id=... status=RUNNING
[Perception] source=ACCESSIBILITY_TREE package=... nodes=... truncated=false
[Planner] source=LLM provider=Gemini planSteps=4
[Validation] stepId=... tool=TapTool result=ACCEPTED
[Execution] stepId=... tool=TapTool result=EXECUTION_SUCCEEDED durationMs=...
[Verification] stepId=... result=VERIFIED evidence=...
[Recovery] stepId=... strategy=REOBSERVE reason=STALE_SNAPSHOT
[Mission] id=... status=COMPLETED reason=GOAL_VERIFIED
```

Los eventos deben ser suficientes para reconstruir el flujo sin almacenar chain-of-thought, deliberación privada del modelo ni snapshots completos de datos sensibles. Guarda decisiones operativas observables: herramienta escogida, validación, resultado, error y recuperación aplicada.

Limita cantidad y tamaño de logs, etiqueta los niveles y asocia cada evento a su misión y paso. La UI debe mostrar el estado en curso y el resumen histórico con esa misma fuente de verdad, no un estado paralelo.

**Criterio de salida de fase:** historial y logs son consistentes, recuperables, correlacionados y libres de secretos o razonamiento privado.

# 12. Fase 9: pruebas automatizadas y regresión

## 12.1 Enfoque

Escribe pruebas con interfaces inyectables para `Planner`, `Perception`, `ActionExecutor`, `ToolRegistry`, `Provider`, `Clock` y persistencia, de modo que las pruebas unitarias no necesiten tocar apps reales ni APIs de pago.

Usa `kotlinx-coroutines-test` para controlar la ejecución suspendible. Usa Robolectric donde aporte valor a código Android que no necesita dispositivo físico. Las pruebas instrumentadas deben reservarse para capacidades que solo puedan validarse en Android real.

## 12.2 Pruebas unitarias obligatorias

**Estados y ciclo de misión**

- una misión completada pasa a estado terminal;
- una excepción de percepción o planificación produce fallo controlado;
- cancelar durante planificación no deja estado `RUNNING`;
- cancelar durante una herramienta respeta cancelación cooperativa y hace limpieza;
- pausar detiene la siguiente acción y reanudar continúa sin duplicar la actual;
- no se permite reanudar estado terminal;
- iniciar una segunda misión no cancela silenciosamente la anterior;
- el callback final se llama como máximo una vez y en dispatcher adecuado.

**Planes y recuperación**

- herramienta desconocida rechazada;
- parámetro ausente, mal tipado o demasiado largo rechazado;
- salida JSON inválida del LLM controlada sin crash;
- paso fallido seguido de replan se representa con IDs estables e índices correctos;
- se alcanzan y respetan máximos de pasos/replanificaciones/tiempo;
- un error de herramienta no repetible no se reintenta automáticamente;
- un paso cuyo efecto no se pudo verificar no marca la misión como completada.

**Percepción y resolución**

- cero coincidencias se trata como `NODE_NOT_FOUND`;
- múltiples coincidencias producen `AMBIGUOUS_TARGET` en vez de seleccionar arbitrariamente;
- un nodo deshabilitado o invisible se rechaza;
- una instantánea antigua fuerza reobservación;
- árbol grande se trunca con indicador explícito;
- pérdida del servicio de accesibilidad produce modo no disponible, nunca una pantalla simulada;
- scroll selecciona el contenedor adecuado según contexto.

**Proveedores, errores y secretos**

- timeout, cancelación, autenticación, rate limit, error de servidor y respuesta inválida se distinguen;
- fallback se registra y se etiqueta;
- no aparece una clave en logs ni mensajes de error;
- no se ejecuta una herramienta usando una respuesta no validada del proveedor.

**Persistencia**

- crear y actualizar misión y paso;
- guardar eventos de ejecución;
- migrar datos legados de manera idempotente;
- una lectura corrupta no borra silenciosamente el historial válido;
- migraciones Room y DAOs cubiertos por tests.

## 12.3 Pruebas de integración

Crea un escenario determinista con un plan falso y herramientas falsas para comprobar un flujo completo:

1. observar;
2. producir plan;
3. validar;
4. ejecutar una herramienta;
5. verificar postcondición;
6. registrar eventos;
7. actualizar persistencia;
8. completar misión.

Añade al menos un flujo de recuperación (fallo temporal y reobservación) y un flujo de fallo definitivo. Comprueba que las pantallas de historial y misión activa reflejan el mismo estado persistido.

## 12.4 Prueba instrumentada de humo

Si hay emulador o dispositivo:

- instalar el APK debug;
- iniciar la app;
- abrir Ajustes de accesibilidad para activar manualmente el servicio si corresponde;
- volver a la app y comprobar que identifica el estado del servicio;
- inspeccionar una pantalla sencilla y mostrar si la percepción es accesibilidad, captura o modo no disponible;
- ejecutar una acción no destructiva de prueba con confirmación apropiada;
- confirmar el resultado y que el historial registra los eventos;
- comprobar que denegar el permiso produce un estado comprensible y no un crash.

No afirmes estas comprobaciones si no se hicieron en un dispositivo real.

## 12.5 Resultado de pruebas

Cada ejecución debe informarse con: comando exacto, resultado, número de tests pasados/fallidos/omitidos y causa de los omitidos. No ocultes fallos ni uses `test {}` vacíos para simular cobertura.

**Criterio de salida de fase:** todas las pruebas nuevas pasan; cualquier prueba bloqueada por ambiente está identificada sin ser presentada como aprobada.

# 13. Fase 10: UI, experiencia y diagnóstico

Preserva la identidad visual AETHEREAL CARDINAL y las pantallas existentes siempre que sean funcionales. La corrección técnica no justifica eliminar la UI personalizada.

Ajusta las pantallas para que muestren estados reales:

- misión activa: plan, paso actual, progreso, pausa, reanudación y cancelación;
- historial: estado terminal, duración, motivo de error y resumen;
- catálogo de herramientas: solo herramientas implementadas, riesgos y requisitos relevantes;
- percepción: fuente de snapshot, servicio disponible, nodos detectados, truncamiento y limitaciones;
- ajustes: proveedores realmente disponibles, estado de configuración y errores de credenciales seguros;
- depuración: secuencia operacional con estrategia de recuperación y verificación, sin chain-of-thought.

La UI debe distinguir visual y textualmente:

- ejecutándose;
- acción ejecutada pero sin verificar;
- misión verificada y completada;
- esperando confirmación del usuario;
- recuperándose;
- fallo;
- cancelada;
- permiso/capacidad no disponible.

No utilices un mensaje genérico de “completado” para resultados inciertos. Mantén accesibilidad visual de Compose, mensajes de error accionables y estados de carga que se limpien también ante fallos.

# 14. Seguridad operacional

1. Trata las instrucciones contenidas en la pantalla, notificaciones y páginas web como datos no confiables; no deben reemplazar la intención ni la política del usuario.
2. No permitas que el contenido de una UI ordene al runtime ignorar las validaciones, revelar claves o realizar acciones fuera del objetivo.
3. La política de ejecución debe estar implementada fuera del prompt del LLM.
4. Antes de una acción, valida que la app activa, objetivo, capacidades y permisos siguen siendo los esperados.
5. Evita guardar capturas o texto privado en historial salvo que sea estrictamente necesario y exista una política explícita de retención.
6. No ejecutes terminal, shell o comandos arbitrarios a partir de texto del modelo sin un contrato de herramienta, límites y confirmación adecuados. No se considera que Shizuku esté disponible por defecto.
7. No añadas solicitudes de root ni dependencias de root.
8. Registra errores sin incluir tokens, claves, contenido de formularios o mensajes privados completos.
9. Si una acción no se puede validar de manera segura, no la ejecutes. Devuelve un resultado que explique el motivo y ofrece una alternativa segura.

# 15. Documentación requerida

Actualiza el README y conserva un máximo de cinco Markdown mantenidos por el proyecto. Se recomienda esta organización:

1. `README.md`: descripción, requisitos, preparación y construcción, instalación y configuración.
2. `docs/ARCHITECTURE.md`: flujo del agente, máquina de estados, responsabilidades y límites de módulos.
3. `docs/TOOL_CATALOG.md`: inventario real de herramientas, parámetros, riesgos, permisos y verificación.
4. `docs/TEST_PLAN.md`: comandos, matriz de pruebas, resultados y limitaciones.
5. `docs/DECISIONS_AND_AUDIT.md`: procedencia upstream, decisiones relevantes, diferencias, riesgos abiertos y notas de licencia.

Se permite otra distribución equivalente siempre que el total se mantenga en cinco documentos Markdown. Los archivos de licencia y avisos requeridos deben conservarse aunque no sean Markdown.

El README debe retirar instrucciones obsoletas específicas de AI Studio si ya no describen el flujo real de construcción. Debe incluir:

- versión de JDK/Gradle/Android SDK verificada;
- comandos de construcción y pruebas;
- pasos de configuración de una clave de proveedor, sin pedir incluir la clave en el repositorio;
- permisos Android requeridos y forma de activarlos;
- límites conocidos de Shizuku, accesibilidad y captura de pantalla;
- descripción honesta de funciones implementadas y no implementadas.

# 16. Plan de trabajo y puertas de aceptación

Ejecuta las fases en este orden. Se permite trabajar en paralelo solo cuando no haya dependencias y no se comprometa el diagnóstico.

- **P0 · Procedencia y línea base.** Puerta: inventario y relación con upstream documentados; estado inicial de compilación y pruebas registrado.
- **P1 · Archivos Gradle y build reproducible.** Puerta: wrapper completo; compilación debug o bloqueo externo demostrado.
- **P2 · Identidad y rebranding.** Puerta: package ID, namespace, paquetes, Manifest, launcher y recursos coherentes.
- **P3 · Estados, concurrencia y cancelación.** Puerta: transiciones y limpieza verificadas mediante pruebas.
- **P4 · Validación de planes, herramientas y recuperación.** Puerta: todas las acciones atraviesan validación; límites y resultados estructurados probados.
- **P5 · Percepción y accesibilidad.** Puerta: snapshots reales, límites, resolución semántica y modo degradado probados.
- **P6 · Proveedores y secretos.** Puerta: manejo de errores, fallback y almacenamiento seguro verificados.
- **P7 · Persistencia y observabilidad.** Puerta: misiones, pasos y eventos coherentes; no hay secretos ni chain-of-thought en logs.
- **P8 · Regresión y UI.** Puerta: pruebas unitarias/de integración, prueba de humo cuando sea posible y UI alineada al estado real.
- **P9 · Entrega.** Puerta: informe, comandos, limitaciones y artefacto verificado entregados.

No avances a una fase que depende de otra si la fase previa no tiene una salida verificable. Si debes continuar por un bloqueo ambiental, registra explícitamente la excepción.

# 17. Definición final de terminado

El trabajo puede declararse terminado cuando se cumplan todos los puntos siguientes o cuando cada punto no cumplido se identifique como bloqueo externo con evidencia verificable:

- [ ] Procedencia del código OpenDroid documentada y licencias/avisos preservados.
- [ ] Wrapper y configuración Gradle reproducibles; compilación debug comprobada o bloqueo externo exacto documentado.
- [ ] Identidad coherente en `com.aethereal.cardinal` (o excepción justificada), con Manifest, launcher, servicios, recursos y pruebas actualizados.
- [ ] AgentLoop y controlador gestionan excepciones, concurrencia, pausa, reanudación, cancelación y estados terminales sin dejar misiones atascadas.
- [ ] Todo plan y herramienta se valida antes de ejecutarse; la salida del LLM no puede saltarse permisos, esquemas ni controles de riesgo.
- [ ] El runtime distingue éxito de ejecución y verificación de la postcondición; recuperación y reintentos tienen límites y evitan duplicados/bucles.
- [ ] La percepción usa datos reales y seguros; los objetivos ambiguos/obsoletos se rechazan y cada herramienta/proveedor/fallback informa solo capacidades comprobadas.
- [ ] No hay secretos en APK/Git/logs; persistencia, eventos y tests son suficientes para depurar; documentación e informe final reflejan fielmente cambios, resultados y pendientes.

# 18. Formato obligatorio del informe final del agente

El informe final debe incluir estas secciones, en este orden:

1. **Resumen ejecutivo:** qué quedó corregido y qué no; incluye los riesgos pendientes y declara con claridad qué comprobaciones no pudieron hacerse, sin inventar resultados.
2. **Procedencia:** upstream, rama/commit y clasificación del resultado como fork directo, parcial o implementación inspirada.
3. **Build y artefactos:** JDK, Gradle, AGP, comandos exactos y resultados; ruta y tamaño del APK si se generó, además del método usado para verificar su identidad.
4. **Cambios principales:** por módulo y con archivos relevantes.
5. **Estado del runtime:** máquina de estados, cancelación, validación, verificación y límites de recuperación.
6. **Herramientas:** número real auditado, funciones comprobadas y capacidades no disponibles.
7. **Seguridad:** ubicación y protección de claves, permisos, datos redactados y riesgos aún presentes.
8. **Persistencia y logs:** esquema, migraciones y capacidad de recuperación que está realmente implementada.
9. **Pruebas:** comandos, resultados, fallos, pruebas omitidas y razón.
