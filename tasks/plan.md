# Implementation Plan: Ad Performance Aggregator (FV-SEC001)

Spec: [`SPEC.md`](../SPEC.md) (approved 2026-11-12; `results/` at the repo root)
Task checklist: [`tasks/todo.md`](todo.md)

## Overview

Build a Java 21 / Maven command-line app that reads `ad_data.csv` (~1GB, ~26.8M rows) once, line by line. It adds up the metrics for each `campaign_id` and writes `results/top10_ctr.csv` and `results/top10_cpa.csv`.

Work happens in thin slices:
- **Early:** get a working end-to-end path on a small fixture file, then run it on the real file. This tells us the processing time before we invest in polish.
- **Next:** make it robust (bad rows, edge cases, exit codes).
- **Last:** deliverables (benchmarks, Docker, README).

## Architecture Decisions

- **Single pass, constant memory**: `BufferedReader` with a 1 MiB buffer feeding a `HashMap<String, CampaignStats>`. Memory grows with the number of campaigns (~50), not the file size.
- **Money in micro-dollars**: spend is summed as a `long` scaled by 10⁶. `BigDecimal` is only used at report time, for about 50 records. Totals are exact and the main loop stays cheap.
- **Hand-rolled line splitting**: no regex and no `String.split`. This is the main way to save CPU time. Building a substring for each field is acceptable as a first version. Only optimise further if the first real run misses 15 seconds.
- **Full sort, not a heap**: there are only about 50 campaigns, so sorting is simpler and the extra cost is negligible.
- **Exit codes**: `Main` is the only class that maps exceptions to exit codes (0/1/2/3). The other classes throw typed exceptions and never call `System.exit`. This keeps them testable.
- **No runtime dependencies**: JUnit 5 for tests only. Build with the system `mvn` (3.8+). The Maven Wrapper was dropped because its plugin was not cached and the wrapper downloads Maven at runtime anyway.

## Dependency Graph

```
pom.xml
   └── CliArgs ──────────────────────────────┐
   └── CsvRowParser ─┐                       │
       CampaignStats ┴─ CampaignAggregator ─┐│
                        CampaignReport ─────┤│
                        Rankings ───────────┤│
                        ReportWriter ───────┴┴── Main (end-to-end)
                                                    └── real-data run → results/
                                                          ├── benchmark script + logs
                                                          ├── Dockerfile
                                                          └── README
```

## Task List

### Phase 1: Working end-to-end path
- [x] Task 1: Maven project skeleton + CLI argument handling
- [x] Task 2: Parse rows and add them up per campaign
- [x] Task 3: Rank campaigns, write the reports, connect it all in `Main`

### Checkpoint A: Small fixture works end-to-end
- [x] `mvn clean package` and `mvn test` pass
- [x] `java -jar target/aggregator.jar --input <fixture> --output <tmp>` writes both files, matching the expected output byte for byte

### Phase 2: Real-data baseline (find out about speed early)
- [x] Task 4: First run on the 1GB file and a timing baseline

### Checkpoint B: Real data
- [x] The run finishes, timing is recorded, any speed fix is agreed with the human before continuing

### Phase 3: Robustness
- [x] Task 5: Row validation and skipped-row reporting
- [x] Task 6: Header and file edge cases, output errors, rounding

### Checkpoint C: Robust
- [x] Every case in the spec's Error Handling table has a passing test

### Phase 4: Deliverables
- [x] Task 7: Check results against an independent calculation, commit `results/`
- [x] Task 8: Benchmark script and logs (time and peak memory, `-Xmx64m`)
- [x] Task 9: Dockerfile
- [x] Task 10: README

### Checkpoint D: Complete
- [ ] Every item in the spec's Success Criteria is ticked
- [ ] Human review before submission

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| JDK 21 / Maven not installed on the dev machine | High | Task 1 checks with `java -version`. If it's missing, ask the human to install it or build through Docker. Resolved: JDK 21 installed; set `JAVA_HOME` to it when running Maven. |
| The 1GB run misses the 15s target | Med | Task 4 measures early. If it's slow, first parse numbers straight from the line text without building substrings, then try a larger buffer. Parallel processing needs the human's approval (spec: Ask first). |
| Rounding or format differs from what reviewers expect | High | Pin exact formatting in `ReportWriter` tests, including halfway rounding cases. Compare against an independent calculation (Task 7). |
| Floating-point drift in spend totals | High | Micro-dollar `long` totals; spec forbids `double` totals. |
| Real data contains rows the sample didn't show | Med | Skip-and-count design. Task 4 prints the skipped count and sample errors, which shows what the real data contains. |
| The reference check over the 1GB file is slow or blocked | Low | Run it once, with the human's approval (the earlier full-file scan was declined). The alternative is a second, independent Java/JShell calculation. |
| Windows vs. Linux line endings in fixtures | Low | `.gitattributes` pins test fixtures to LF. The parser accepts `\r\n`. |

## Parallelization

The task list assumes one agent working in order. If parallel work is wanted: once Task 1 is done, Task 2 (`CsvRowParser`/`CampaignStats`) and the `Rankings`/`ReportWriter` part of Task 3 depend only on the `CampaignReport` record, so they can be built in parallel. Tasks 8, 9 and 10 can run in parallel once Task 7 is done.

## Open Questions

- Is the JDK installed? Task 1 checks.
- 15s target and measuring peak memory with `time -v` in Docker: proposed defaults, which stand unless you object.
