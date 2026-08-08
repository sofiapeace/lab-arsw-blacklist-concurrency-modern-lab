# ARSW — Blacklist Concurrency Laboratory

> **Java 21 laboratory on concurrency, performance measurement, fixed thread pools, and virtual threads.**

**Course:** Arquitecturas de Software — ARSW  
**Institution:** Universidad Escuela Colombiana de Ingeniería Julio Garavito  
**Professor:** Javier Iván Toquica  
**Work mode:** Teams of three students  
**Technology:** Java 21 · Maven · JUnit 5  
**Submission deadline:** Defined in the institutional platform

---

## 1. Laboratory purpose

This laboratory evaluates the implementation and experimental comparison of three strategies for consulting blacklist providers:

1. Sequential execution.
2. Concurrent execution with a fixed-size thread pool.
3. Concurrent execution with Java 21 virtual threads.

The goal is not merely to identify the fastest implementation. Each team must produce evidence to explain:

- When concurrency improves performance.
- When task coordination introduces more overhead than benefit.
- How blocking operations affect the choice of concurrency model.
- How correctness is preserved when several tasks execute concurrently.
- What architectural trade-offs exist among performance, complexity, scalability, and maintainability.

> **Correctness comes before performance.** A benchmark is invalid when the compared strategies do not produce equivalent results.

---

## 2. Relationship with Workshop 1

Workshop 1 and Laboratory 1 use the same case, but they are different activities.

### Workshop 1

- Inspect the starter project.
- Execute the sequential implementation.
- Analyze architectural decisions, quality attributes, metrics, and trade-offs.
- Do not implement the concurrent solutions.

### Laboratory 1

- Implement the missing concurrent strategies.
- Create automated tests.
- Execute a controlled benchmark.
- Analyze experimental evidence.
- Document and defend the resulting architectural recommendation.

The decision matrix completed during the workshop is **not** a laboratory deliverable. The laboratory grade is based on implementation, correctness, measurement, analysis, and repository evidence.

---

## 3. Problem statement

A system receives an IP address and asks multiple blacklist providers whether that address has been reported.

The starter project creates 100 deterministic providers. A provider can optionally simulate a blocking I/O operation by waiting for a controlled amount of time.

The supplied sequential implementation:

- Consults all providers.
- Collects the identifiers of matching providers.
- Reports the number of consulted providers.
- Measures elapsed time.
- Classifies the IP according to an alarm threshold.

The laboratory must preserve the same functional result while changing the execution strategy.

---

## 4. Starter project

The repository includes the following relevant classes:

```text
src/
├── main/
│   └── java/edu/eci/arsw/blacklist/
│       ├── BenchmarkRunner.java
│       ├── BlackListProvider.java
│       ├── BlackListSearch.java
│       ├── FixedPoolBlackListSearch.java
│       ├── MockBlackListProvider.java
│       ├── ProviderFactory.java
│       ├── SearchResult.java
│       ├── SequentialBlackListSearch.java
│       └── VirtualThreadBlackListSearch.java
└── test/
    └── java/edu/eci/arsw/blacklist/
        └── SequentialBlackListSearchTest.java
```

### Supplied implementation

`SequentialBlackListSearch` is complete and must be used as the functional baseline.

### Pending implementations

The following classes intentionally contain `TODO` work:

- `FixedPoolBlackListSearch`
- `VirtualThreadBlackListSearch`

`BenchmarkRunner` initially executes only the sequential strategy. Each team must extend it to run the required benchmark configurations.

---

## 5. Technical requirements

Before starting, verify:

```bash
java -version
mvn -version
```

Required versions:

- JDK 21.
- Maven 3.9 or later.
- Git.
- A GitHub account.

Compile and execute the supplied baseline:

```bash
mvn clean test
mvn exec:java
```

Execute the baseline with and without simulated I/O:

```bash
mvn exec:java -Dexec.args="202.24.34.55 true"
mvn exec:java -Dexec.args="202.24.34.55 false"
```

The default IP address is:

```text
202.24.34.55
```

---

## 6. Repository setup

Each team must create its own repository from this template.

Suggested repository name:

```text
arsw-blacklist-lab-gXX
```

Example:

```text
arsw-blacklist-lab-g03
```

Before modifying the code:

1. Add the three team members as collaborators.
2. Clone the team repository.
3. Verify Java 21 and Maven.
4. Run `mvn clean test`.
5. Execute the sequential baseline.
6. Create issues or tasks for the work distribution.
7. Record the baseline result in this README.

Every team member must contribute meaningful commits and must understand the complete solution.

---

# Part A — Concurrent implementation

## 7. Task 1: Fixed-size thread pool

Complete:

```text
FixedPoolBlackListSearch.java
```

The implementation must:

- Implement the `BlackListSearch` interface.
- Receive the provider list and pool size through the constructor.
- Validate that the pool size is greater than zero.
- Use `ExecutorService`.
- Create the executor with `Executors.newFixedThreadPool(poolSize)`.
- Submit provider consultations as concurrent tasks.
- Wait for all submitted tasks.
- Collect each matching provider identifier exactly once.
- Return deterministic results.
- Report the correct number of consulted providers.
- Measure elapsed time with `System.nanoTime()`.
- Close the executor correctly.
- Preserve interruption when an `InterruptedException` occurs.
- Avoid unsafe shared mutable state.

The required pool sizes are:

```text
2, 4, and 8 platform threads
```

### Implementation restrictions

The following approaches do not satisfy this task:

- Replacing the implementation with `parallelStream()`.
- Using the common `ForkJoinPool`.
- Protecting the entire search method with `synchronized`.
- Delegating the search to `SequentialBlackListSearch`.
- Removing or modifying the provider latency to improve results.
- Returning hard-coded matches.

A valid design may use tasks that return their own result and then consolidate those results after calling `Future.get()`.

---

## 8. Task 2: Java 21 virtual threads

Complete:

```text
VirtualThreadBlackListSearch.java
```

The implementation must:

- Implement the `BlackListSearch` interface.
- Use `Executors.newVirtualThreadPerTaskExecutor()`.
- Create one independent task per provider.
- Wait for all tasks to finish.
- Collect each matching provider identifier exactly once.
- Return deterministic results.
- Report the correct number of consulted providers.
- Measure elapsed time with `System.nanoTime()`.
- Close the executor correctly.
- Preserve interruption and provide meaningful error handling.
- Produce a result equivalent to the sequential baseline.

The virtual-thread implementation must not create a manually sized platform-thread pool.

---

## 9. Required result contract

For the mandatory part of this laboratory, all strategies must perform a **complete scan** of the provider list.

For the same IP address and provider configuration:

```text
Sequential result = Fixed-pool result = Virtual-thread result
```

The following values must be equivalent:

- Matching provider identifiers.
- Number of matching providers.
- Trustworthiness classification.
- Number of consulted providers.

Because concurrent tasks can finish in a different order, the returned matching provider identifiers must be ordered before constructing the final `SearchResult`.

For the supplied set of 100 providers:

```text
consultedProviders = 100
```

Early termination at five matches is not part of the mandatory implementation because it changes the amount of evidence collected. It appears only as an optional extension at the end of this document.

---

# Part B — Automated verification

## 10. Task 3: Tests

Add automated tests for the concurrent implementations.

At minimum, the test suite must verify:

1. The sequential implementation is deterministic.
2. A pool of 2 threads returns the same provider identifiers as the sequential baseline.
3. A pool of 4 threads returns the same provider identifiers as the sequential baseline.
4. A pool of 8 threads returns the same provider identifiers as the sequential baseline.
5. The virtual-thread strategy returns the same provider identifiers as the sequential baseline.
6. Every mandatory strategy reports all 100 providers as consulted.
7. Matching provider identifiers contain no duplicates.
8. Matching provider identifiers are returned in ascending order.
9. Creating a fixed-pool search with a non-positive pool size fails with `IllegalArgumentException`.
10. The project passes all tests with simulated I/O disabled.

Run:

```bash
mvn clean test
```

Tests must validate behavior, not execution speed. Do not write tests that fail because one strategy took a few milliseconds more than another.

---

# Part C — Benchmark runner

## 11. Task 4: Extend `BenchmarkRunner`

Modify `BenchmarkRunner` so that it can select the execution strategy from command-line arguments.

Use the following command contract:

```text
<strategy> <ipAddress> <simulateIo> <warmups> <measuredRuns> [poolSize]
```

Accepted strategy values:

```text
SEQUENTIAL
FIXED
VIRTUAL
```

Examples:

```bash
mvn exec:java -Dexec.args="SEQUENTIAL 202.24.34.55 true 2 5"
```

```bash
mvn exec:java -Dexec.args="FIXED 202.24.34.55 true 2 5 4"
```

```bash
mvn exec:java -Dexec.args="VIRTUAL 202.24.34.55 true 2 5"
```

The runner must:

- Validate the arguments.
- Instantiate the selected strategy.
- Execute the requested warm-up runs without including them in the results.
- Execute the requested measured runs.
- Verify that every measured run produces the expected functional result.
- Calculate minimum, maximum, and average elapsed time.
- Print the selected configuration.
- Print individual measured times.
- Print a summary suitable for copying into `results.csv`.

Recommended output fields:

```text
scenario,strategy,pool_size,run,elapsed_ms,matches,consulted_providers
```

Example row:

```text
IO,FIXED,4,1,2845.327,7,100
```

Do not use IDE timestamps or manually measured wall-clock time. Use the elapsed duration returned by the search implementation.

---

# Part D — Experimental comparison

## 12. Task 5: Benchmark methodology

Use the same computer for all measurements.

Before measuring:

- Close unnecessary applications.
- Connect the computer to power when possible.
- Avoid changing the source code between compared runs.
- Run `mvn clean test`.
- Record the execution environment.
- Use two warm-up executions.
- Use five measured executions.

Required experiment matrix:

| Scenario | Strategy | Threads or tasks |
|---|---|---:|
| Local, no simulated I/O | Sequential | 1 |
| Local, no simulated I/O | Fixed pool | 2 |
| Local, no simulated I/O | Fixed pool | 4 |
| Local, no simulated I/O | Fixed pool | 8 |
| Local, no simulated I/O | Virtual threads | 100 tasks |
| Simulated blocking I/O | Sequential | 1 |
| Simulated blocking I/O | Fixed pool | 2 |
| Simulated blocking I/O | Fixed pool | 4 |
| Simulated blocking I/O | Fixed pool | 8 |
| Simulated blocking I/O | Virtual threads | 100 tasks |

### Important interpretation

The scenario without simulated I/O performs a small local calculation. It is useful for observing coordination overhead, but it is not a complete representation of every CPU-bound workload.

The scenario with simulated I/O represents blocking calls such as network, database, or external-service requests.

Do not invent expected times. Performance depends on the execution environment.

---

## 13. Metrics

For every configuration, report:

- Average elapsed time in milliseconds.
- Minimum elapsed time.
- Maximum elapsed time.
- Number of matches.
- Number of consulted providers.
- Speedup relative to the sequential strategy in the same scenario.

Calculate speedup as:

```text
Speedup = sequential average time / strategy average time
```

Interpretation examples:

- `1.00`: no improvement relative to sequential execution.
- Greater than `1.00`: faster than the sequential baseline.
- Less than `1.00`: slower than the sequential baseline.

Do not compare a strategy executed with simulated I/O against a baseline executed without simulated I/O.

---

## 14. Required results table

Complete this table with actual measurements:

| Scenario | Strategy | Pool size | Average ms | Minimum ms | Maximum ms | Speedup | Matches | Consulted |
|---|---|---:|---:|---:|---:|---:|---:|---:|
| No simulated I/O | Sequential | — | 0.007 | 0.005 | 0.012 | 1.00 | 7 | 100 |
| No simulated I/O | Fixed pool | 2 | 0.262 | 0.189 | 0.416 | 0.03 | 7 | 100 |
| No simulated I/O | Fixed pool | 4 | 0.333 | 0.320 | 0.365 | 0.02 | 7 | 100 |
| No simulated I/O | Fixed pool | 8 | 0.443 | 0.406 | 0.478 | 0.02 | 7 | 100 |
| No simulated I/O | Virtual threads | — | 0.390 | 0.242 | 0.474 | 0.02 | 7 | 100 |
| Simulated I/O | Sequential | — | 11260.235 | 11227.221 | 11303.817 | 1.00 | 7 | 100 |
| Simulated I/O | Fixed pool | 2 | 5664.929 | 5651.239 | 5679.442 | 1.99 | 7 | 100 |
| Simulated I/O | Fixed pool | 4 | 2900.718 | 2892.418 | 2912.849 | 3.88 | 7 | 100 |
| Simulated I/O | Fixed pool | 8 | 1511.382 | 1509.027 | 1516.314 | 7.45 | 7 | 100 |
| Simulated I/O | Virtual threads | — | 198.096 | 197.913 | 198.499 | 56.84 | 7 | 100 |

Also include the raw measurements in:

```text
results/results.csv
```

Suggested repository location:

```text
results/
├── results.csv
└── environment.md
```

---

# Part E — Analysis and architectural recommendation

## 15. Task 6: Required analysis

Answer every question with evidence from the experiment.

### 15.1 Correctness

1. How did the team verify that the three strategies produce equivalent results?

> Con dos mecanismos: una suite de 9 pruebas JUnit que compara los IDs, el número de proveedores consultados y la clasificación de confianza de cada estrategia (pools de 2, 4 y 8 y virtual threads) contra la línea base secuencial; y (b) el `BenchmarkRunner`, que verifica en **cada corrida medida** que el resultado sea idéntico al secuencial y aborta si no lo es. Las 50 mediciones registradas reportan los mismos 7 matches y 100 consultados.

2. Why can concurrent tasks return matches in a different order?

> Porque el planificador (del SO para hilos de plataforma, de la JVM para virtual threads) decide en qué orden se ejecutan y terminan las tareas, y eso varía entre corridas. El proveedor 90 puede responder antes que el 13. Por eso el orden de llegada es no determinista, y ordenamos los IDs ascendentemente antes de construir el `SearchResult`.

3. What mechanism or design prevented lost or duplicated matches?

> Diseño sin estado mutable compartido: cada tarea consulta **un** proveedor y devuelve su propio resultado (el ID si hay match, `null` si no) a través de un `Future`. El hilo principal consolida los resultados con `Future.get()`, así que ningún hilo escribe en una colección compartida. Cada proveedor se envía exactamente una vez, luego no puede haber duplicados ni pérdidas: la condición de carrera se elimina por diseño en lugar de mitigarse con locks.

4. Why should performance not be compared before proving functional equivalence?

> Porque un benchmark entre estrategias que calculan cosas distintas no compara nada. Lo vivimos en el laboratorio: la primera versión concurrente tenía un cortocircuito y devolvía 50 matches donde el secuencial encontraba 77 — era "más rápida" precisamente porque hacía menos trabajo. La correctitud define qué se está midiendo; sin ella el tiempo es un número sin significado.

### 15.2 Fixed thread pool

5. What changed when the pool increased from 2 to 4 threads?

> Con I/O simulado el tiempo promedio bajó de 5664.9 ms a 2900.7 ms (speedup de 1.99 a 3.88): casi exactamente la mitad, porque el trabajo dominante es espera bloqueante y se reparte entre el doble de hilos. Sin I/O ocurrió lo contrario: el promedio *subió* de 0.262 ms a 0.333 ms, porque no hay espera que repartir y solo se agregó más coordinación.

6. What changed when the pool increased from 4 to 8 threads?

> Mismo patrón: con I/O el promedio bajó de 2900.7 ms a 1511.4 ms (speedup 7.45), de nuevo cerca de la mitad. Sin I/O volvió a empeorar (0.333 ms → 0.443 ms). Más hilos solo ayudan cuando hay bloqueo que solapar.

7. Was the improvement proportional to the number of threads? Explain.

> Con I/O fue casi proporcional pero siempre un poco por debajo del ideal: 1.99x con 2 hilos, 3.88x con 4, 7.45x con 8. La diferencia frente al ideal (2x, 4x, 8x) viene del desbalanceo de carga —las latencias por proveedor van de 20 a 200 ms, así que un hilo puede quedarse con tareas más lentas mientras otros terminan— más el costo fijo de crear el pool y consolidar resultados. Es el comportamiento que predice la ley de Amdahl cuando existe una fracción no paralelizable.

8. What costs are introduced by task creation, scheduling, context switching, and result consolidation?

> El escenario sin I/O los aísla perfectamente: el trabajo real toma 0.007 ms en secuencial, pero cualquier variante concurrente tarda entre 0.26 y 0.44 ms — entre 35 y 60 veces más lento. Ese sobrecosto es crear el executor y sus hilos, encolar 100 tareas, los cambios de contexto y recorrer 100 futures. Con I/O ese mismo costo (~0.3 ms) es despreciable frente a los ~11 000 ms de espera que se ahorran.

9. What would happen if the pool size were much larger than the available platform threads?

> Los hilos de plataforma son hilos del SO (~1-2 MB de pila cada uno). Un pool mucho mayor que los 10 procesadores lógicos de la máquina no agrega paralelismo real para trabajo de CPU: agrega memoria consumida y más cambios de contexto. Para trabajo bloqueante sí permitiría más esperas simultáneas (con 100 hilos el escenario I/O se acercaría al tiempo del proveedor más lento), pero a un costo por hilo alto — que es exactamente el problema que los virtual threads resuelven baratos.

### 15.3 Virtual threads

10. In which scenario did virtual threads provide the clearest benefit?

> En el escenario con I/O bloqueante, de forma contundente: 198.1 ms promedio contra 11 260.2 ms del secuencial — un speedup de 56.84x, y 7.6 veces más rápido que el mejor pool fijo (8 hilos). El tiempo total quedó pegado al proveedor más lento (~200 ms), que es el límite teórico cuando las 100 consultas se lanzan a la vez.

11. Why are virtual threads especially relevant for blocking operations?

> Porque cuando un virtual thread se bloquea (en este caso en `Thread.sleep`, en producción en una llamada de red o base de datos), la JVM lo *desmonta* del hilo de plataforma que lo transporta y ese hilo queda libre para ejecutar otro virtual thread. Bloquear deja de desperdiciar un recurso caro del SO. Eso permite tener las 100 esperas en curso simultáneamente con apenas ~10 hilos de plataforma, algo inviable con un pool fijo de tamaño razonable.

12. Why do virtual threads not make local CPU work automatically faster?

> Porque no crean capacidad de cómputo: los virtual threads se ejecutan sobre los mismos ~10 núcleos de la máquina. Solo cambian la economía del *bloqueo*. Nuestros datos lo muestran: sin I/O, los virtual threads (0.390 ms) fueron ~55 veces más lentos que el secuencial (0.007 ms), igual de penalizados por el overhead de coordinación que el pool fijo.

13. What trade-offs remain even when virtual threads are lightweight?

> Siguen existiendo: (a) el costo de coordinación y consolidación de resultados, visible en el escenario sin I/O; (b) el orden no determinista de terminación, que obliga a ordenar resultados; (c) la depuración y el monitoreo de miles de hilos efímeros es menos madura que la de pools clásicos; (d) el código que usa `synchronized` sobre operaciones bloqueantes puede *anclar* (pinning) el virtual thread a su hilo de plataforma y degradar el beneficio; y (e) no sirven para limitar concurrencia — con 100 tareas simultáneas un servicio externo real podría recibirnos como un ataque; un pool fijo actúa además como regulador (throttle).

### 15.4 Architectural decision

14. Which strategy would the team recommend for a system dominated by blocking external calls?

> Virtual threads. Es el caso de uso para el que fueron diseñados y donde la evidencia es más clara (speedup de 56.84x). Además el código resultante es el más simple: una tarea por consulta, sin decidir un tamaño de pool.

15. Which strategy would the team recommend for a small local workload?

> Ejecución secuencial. Sin I/O, cualquier forma de concurrencia fue decenas de veces más lenta que el secuencial (0.007 ms vs 0.26–0.44 ms). Para trabajo local pequeño, la solución más simple es también la más rápida y la más fácil de mantener.

16. Under what conditions would a fixed pool still be preferable?

> Cuando se necesita **limitar** la concurrencia, no maximizarla: proveedores externos con límite de peticiones simultáneas o cuotas, conexiones a base de datos acotadas por un pool, control de presión sobre servicios aguas abajo, o trabajo CPU-bound donde lo óptimo es un pool del tamaño del número de núcleos. También en bases de código con mucho `synchronized` bloqueante, donde los virtual threads sufren pinning.

17. What evidence from the measurements supports the recommendation?

> La tabla de la sección 14: con I/O, virtual threads promedió 198.1 ms contra 11 260.2 ms del secuencial y 1511.4 ms del mejor pool fijo; el speedup del pool creció casi linealmente con los hilos (1.99x → 3.88x → 7.45x) pero siempre acotado por el tamaño del pool, mientras los virtual threads alcanzaron el límite físico del problema (el proveedor más lento, ~200 ms). Sin I/O, el secuencial ganó por más de un orden de magnitud a todas las variantes concurrentes. Las 50 mediciones produjeron resultados funcionales idénticos (7 matches, 100 consultados), así que la comparación es válida.

18. What limitations prevent generalizing the conclusion to every production system?

> La latencia es simulada con `Thread.sleep`, que es perfectamente paralelizable; una red real tiene límites de ancho de banda, pools de conexiones, timeouts y fallos. El trabajo "local" del mock es trivial, no representa cargas CPU-bound reales. Se midió en una sola máquina (Apple M5, 10 núcleos) con 100 proveedores y 5 corridas; otros tamaños de problema, hardware o JVMs pueden desplazar los puntos de equilibrio. Y no medimos consumo de memoria ni comportamiento bajo carga sostenida, solo latencia de una búsqueda a la vez.

Answers such as “virtual threads are better” or “more threads are faster” are insufficient without conditions and evidence.

---

## 16. Architectural conclusion

Write a team conclusion of 150 to 250 words.

The conclusion must include:

- The dominant workload characteristic.
- The measured evidence.
- The recommended strategy.
- The conditions under which the recommendation is valid.
- At least one trade-off.
- At least one limitation of the experiment.

### Team conclusion

> La carga de trabajo de este problema está dominada por operaciones bloqueantes: cada consulta a un proveedor espera entre 20 y 200 ms simulando una llamada de red, de modo que el tiempo secuencial (11 260 ms en promedio) es casi en su totalidad espera, no cómputo. Bajo esa característica, la evidencia medida es concluyente: el pool fijo escaló de forma casi lineal con el número de hilos (speedup de 1.99x con 2, 3.88x con 4 y 7.45x con 8), mientras que los hilos virtuales, al lanzar las 100 consultas simultáneamente, redujeron el tiempo a 198 ms —un speedup de 56.84x— quedando acotados por el proveedor más lento. Recomendamos por tanto hilos virtuales para sistemas dominados por llamadas externas bloqueantes, siempre que los servicios consultados toleren esa concurrencia simultánea; cuando existe un límite de peticiones concurrentes o la carga es CPU-bound, un pool fijo dimensionado según ese límite sigue siendo preferible porque actúa como regulador. El trade-off principal es que la concurrencia no es gratuita: en el escenario sin I/O toda variante concurrente fue más de 35 veces más lenta que la ejecución secuencial (0.26–0.44 ms contra 0.007 ms) por el costo de coordinación. La principal limitación del experimento es que la latencia simulada con `Thread.sleep` es perfectamente paralelizable y no captura límites reales de red, conexiones o fallos, y que se midió en una sola máquina con un único tamaño de problema.

---

## 17. Individual conclusions

Each student must add an individual conclusion of 80 to 120 words.

### Student 1

**Name:** Sofia Garcia

> Al implementar las estrategias concurrentes aprendí que la correctitud es anterior al rendimiento. Mi primera versión detenía la búsqueda al alcanzar el umbral de alarmas y parecía funcionar, pero rompía la equivalencia con la línea base: devolvía 50 coincidencias donde el secuencial encontraba 77. Entendí que un resultado "más rápido" que calcula algo distinto no es una optimización sino un error. También comprobé el valor de diseñar sin estado mutable compartido: al pasar de acumular en una lista compartida a consolidar resultados con `Future.get()`, la condición de carrera desapareció por diseño y no por parches con estructuras atómicas. La concurrencia exige verificar, no suponer.

### Student 2

**Name:** Jose Lancheros

> Lo que más me marcó fue medir el costo real de la coordinación. Antes del laboratorio asumía que más hilos significaban más velocidad; los datos mostraron lo contrario para trabajo local: sin I/O simulado, toda variante concurrente fue más de 35 veces más lenta que la secuencial (0.26–0.44 ms contra 0.007 ms). En cambio, con latencia bloqueante los hilos virtuales redujeron 11.3 segundos a 198 ms, un speedup de 56.84x, acotado por el proveedor más lento. Concluyo que la decisión arquitectónica correcta depende de caracterizar la carga bloqueante o de cómputo y de medir con una metodología reproducible: warm-ups, corridas repetidas y verificación de equivalencia en cada ejecución.

---

# Part F — Submission

## 18. Required deliverables

The repository must contain:

- Functional sequential baseline.
- Functional fixed-thread-pool implementation.
- Functional virtual-thread implementation.
- Extended `BenchmarkRunner`.
- Automated tests.
- `results/results.csv`.
- `results/environment.md`.
- Completed results table.
- Answers to all analysis questions.
- Team architectural conclusion.
- Three individual conclusions.
- AI-use declaration.
- Meaningful Git history from all team members.

The repository must compile from a clean clone:

```bash
mvn clean test
```

---

## 19. Execution environment

Complete:

| Item | Value |
|---|---|
| Operating system | macOS 26.5.2 (build 25F84) |
| CPU model | Apple M5 |
| Logical processors | 10 |
| RAM | 24 GB |
| JDK vendor and version | Homebrew OpenJDK 21.0.11 |
| Maven version | Apache Maven 3.9.16 |
| Measurement date | 2026-08-07 |

See [results/environment.md](results/environment.md) for the full methodology. Raw measurements are in [results/results.csv](results/results.csv), reproducible with `./run-benchmark.sh`.

---

## 20. Team members and contribution evidence

This team has two members.

| Student | GitHub username | Main contribution | Relevant commits                                                       |
|---|---|---|------------------------------------------------------------------------|
| Sofia | sofiapeace | Implementación inicial de las estrategias concurrentes y primeras pruebas automatizadas | `22ccbd5`, `02f10c0`                                                   |
| Jose | Lanch3ros | Corrección del contrato de escaneo completo, suite de pruebas de equivalencia, benchmark runner, mediciones y documentación | fix complete-scan contract, extend benchmark runner, benchmark results |

Each student must have at least two meaningful commits.

Examples of meaningful commits:

```text
Implement fixed thread pool search
Add virtual-thread search strategy
Add equivalence and ordering tests
Extend benchmark runner and CSV output
Document benchmark analysis and trade-offs
```

Formatting-only changes, name changes, or typo corrections do not count as sufficient contribution evidence.

---

## 21. Final submission tag

After verifying the final version:

```bash
git status
mvn clean test
git tag -a lab-1-final -m "Laboratory 1 final submission"
git push origin lab-1-final
```

Submit the repository URL and confirm that the `lab-1-final` tag is available remotely.

---

# Part G — Grading rubric

## 22. Rubric

| Criterion | Weight | Maximum grade |
|---|---:|---:|
| Correctness and equivalence of results | 20% | 1.00 |
| Fixed-pool and virtual-thread implementations | 20% | 1.00 |
| Benchmark methodology and reproducibility | 25% | 1.25 |
| Analysis and architectural trade-offs | 25% | 1.25 |
| Repository, documentation, and individual traceability | 10% | 0.50 |
| **Total** | **100%** | **5.00** |

### 22.1 Correctness and equivalence — 1.00

Full credit requires:

- All strategies return equivalent matches.
- All mandatory strategies consult 100 providers.
- Results contain no duplicates.
- Results are deterministic and ordered.
- Automated tests pass.

### 22.2 Concurrent implementations — 1.00

Full credit requires:

- Correct use of a fixed `ExecutorService`.
- Correct use of Java 21 virtual threads.
- Proper executor lifecycle.
- Appropriate exception and interruption handling.
- No unsafe global state.
- No sequential delegation disguised as concurrency.

### 22.3 Benchmark methodology — 1.25

Full credit requires:

- All ten mandatory configurations.
- Two warm-ups and five measured executions.
- Same environment and baseline per scenario.
- Raw data and summary metrics.
- Reproducible commands.
- Correct speedup calculations.

### 22.4 Analysis and trade-offs — 1.25

Full credit requires:

- Evidence-based interpretation.
- Correct distinction between blocking and local work.
- Analysis of pool size.
- Analysis of virtual threads.
- Architectural recommendation with conditions.
- Explicit limitations and trade-offs.

### 22.5 Repository and traceability — 0.50

Full credit requires:

- Clear documentation.
- Clean repository structure.
- Meaningful contributions from all students.
- Complete AI-use declaration.
- Final submission tag.
- Successful execution from a clean clone.

---

## 23. Oral verification

Any team member may be selected to:

- Explain a section of the concurrent implementation.
- Describe how race conditions were avoided.
- Explain a benchmark result.
- Reproduce a command.
- Justify the architectural recommendation.
- Explain code produced or modified with AI assistance.

The individual grade may be adjusted when a student cannot demonstrate understanding or contribution.

---

## 24. Use of artificial intelligence

AI tools may be used as support, but every student must understand and defend the submitted work.

Complete the following table:

| Tool | Purpose | Main prompts or activities | Validation performed | Changes made by the team |
|---|---|---|---|---|
| Claude Code | Apoyo en diagnóstico, corrección de código, benchmark y documentación | Explicación del enunciado y del estado del repositorio; diagnóstico del fallo de equivalencia (cortocircuito vs. escaneo completo); reescritura de `FixedPoolBlackListSearch` y `VirtualThreadBlackListSearch` con el patrón de consolidación por `Future`; ampliación de la suite de pruebas; extensión de `BenchmarkRunner`; script de benchmark y llenado de tablas con los datos medidos | `mvn clean test` (9 pruebas en verde); verificación automática de equivalencia contra la línea base secuencial en cada una de las 50 corridas medidas; revisión manual del código por el equipo antes de cada commit | El equipo revisó y commiteó cada cambio, ejecutó las mediciones en su propia máquina y redactó las conclusiones individuales y la revisión final de la conclusión grupal |

Requirements:

- Do not submit code that the team cannot explain.
- Validate generated code through tests and review.
- Record relevant AI assistance.
- Do not use AI output as a replacement for experimental evidence.
- Plagiarism or duplicated repository content is subject to the course academic-integrity rules.

---

# Optional extensions

These extensions do not replace any mandatory requirement.

## A. Early termination

Create a separate strategy that stops after finding five matches.

Analyze:

- Whether the final classification remains valid.
- Whether the complete evidence list is preserved.
- How pending tasks are cancelled.
- How many providers are actually consulted.
- What happens to tasks already running.
- How early termination changes comparability with the complete-scan benchmark.

Do not replace the mandatory complete-scan strategies with this extension.

## B. Five-minute cache

Add a cache with a five-minute TTL.

Analyze:

- Cache key.
- Thread safety.
- Expiration.
- Stale information.
- Cache hit ratio.
- Effect on elapsed time.
- Effect on correctness and freshness.

---

# Final checklist

Before submission, verify:

- [X] The project uses Java 21.
- [X] `mvn clean test` passes.
- [X] Fixed pools of 2, 4, and 8 threads work.
- [X] The virtual-thread strategy works.
- [X] All mandatory strategies return equivalent results.
- [X] Results are ordered and contain no duplicates.
- [X] The benchmark runner supports the required arguments.
- [X] Two warm-ups and five measured runs were executed.
- [X] All ten required configurations were measured.
- [X] `results/results.csv` contains raw measurements.
- [X] The environment is documented.
- [X] The results table is complete.
- [X] All analysis questions are answered.
- [X] The team conclusion is complete.
- [X] Every student added an individual conclusion.
- [X] Every student has meaningful commits.
- [X] AI use is declared.
- [X] The `lab-1-final` tag was pushed.
- [X] The repository URL was submitted in the institutional platform.
