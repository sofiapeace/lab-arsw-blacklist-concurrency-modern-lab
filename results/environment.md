# Benchmark execution environment

| Item                   | Value                      |
|------------------------|----------------------------|
| Operating system       | macOS 26.5.2 (build 25F84) |
| CPU model              | Apple M5                   |
| Logical processors     | 10                         |
| RAM                    | 24 GB                      |
| JDK vendor and version | Homebrew OpenJDK 21.0.11   |
| Maven version          | Apache Maven 3.9.16        |
| Measurement date       | 2026-08-07                 |

## Methodology

- Same machine for all measurements, connected to power, with unnecessary applications closed.
- `JAVA_HOME` pinned to OpenJDK 21 (`/opt/homebrew/opt/openjdk@21`).
- 2 warm-up executions (discarded) and 5 measured executions per configuration.
- 10 configurations: {no simulated I/O, simulated I/O} × {sequential, fixed pool 2/4/8, virtual threads}.
- 100 providers; simulated I/O latency per provider ranges from 20 to 200 ms (deterministic per provider id).
- Elapsed time taken from the `Duration` returned by each search implementation (measured internally with `System.nanoTime()`), never from wall-clock or IDE timestamps.
- Every measured run is verified against the sequential baseline before being recorded.
- Reproducible with: `./run-benchmark.sh` (writes `results/results.csv`).
