# Spec: Ad Performance Aggregator (FV-SEC001)

Source requirement: `requirement/recruitment/fv-sec-001-software-engineer-challenge/README.md`

## Assumptions

1. **Java 21 (LTS) + Maven 3.8+.** No runtime dependencies; JUnit 5 for tests only.
2. **The input matches the published schema and has a header row**: `campaign_id,date,impressions,clicks,spend,conversions`. Fields are not quoted and contain no commas. If a quoted field does show up, the row is treated as malformed and skipped (see Error Handling).
3. **`spend` is summed exactly using fixed-point arithmetic**: a `long` in micro-dollars (×10⁶), never `double`. This keeps totals exact, because the README says results must "match expected values precisely". Overflow headroom: about 27M rows × $10⁴ × 10⁶ ≈ 2.7×10¹⁷, well below `Long.MAX_VALUE` (9.2×10¹⁸).
4. **Output formatting**:
   - `total_spend`: 2 decimals
   - `CTR`: 4 decimals
   - `CPA`: 2 decimals
   - Rounding is `HALF_UP`, computed with `BigDecimal` only for the final ~50 campaigns, so the hot loop stays fast.
5. **Ranking tie-break**: when two campaigns have the same metric, sort by `campaign_id` ascending, so the output is deterministic.
6. **Edge cases in the metrics**:
   - `CTR` is `null` when `total_impressions = 0`, and those campaigns are left out of the CTR ranking.
   - `CPA` is `null` when `total_conversions = 0`. Those campaigns are left out of the CPA ranking, and the CPA cell is empty in the CTR output.
7. **The 1GB dataset is never committed.** It stays where it is under `requirement/` (or is passed via `--input`). Only the result CSVs are committed.
8. **Fewer than 10 eligible campaigns** means the output contains only as many rows as exist. This is not an error.

## Objective

Build a console app that streams a large ad-performance CSV (~1GB, about 26.8M rows) in a single pass. It aggregates the metrics per `campaign_id` and writes two files:

- `top10_ctr.csv`: the 10 campaigns with the highest CTR
- `top10_cpa.csv`: the 10 campaigns with the lowest CPA, excluding campaigns with zero conversions

**Who uses it:** the Flinters reviewers. They judge correctness, clean code, memory efficiency, error handling, tests and documentation.

**Metrics per campaign:**

| Metric | Definition |
|---|---|
| `total_impressions` | sum of `impressions` |
| `total_clicks` | sum of `clicks` |
| `total_spend` | sum of `spend` |
| `total_conversions` | sum of `conversions` |
| `CTR` | total_clicks / total_impressions |
| `CPA` | total_spend / total_conversions (`null` when conversions = 0) |

**Output format (both files):**

```
campaign_id,total_impressions,total_clicks,total_spend,total_conversions,CTR,CPA
CMP042,125000,6250,12500.50,625,0.0500,20.00
```

## Tech Stack

- Java 21, Maven 3.8+ (`maven-jar-plugin` builds the executable jar `target/aggregator.jar`)
- JUnit 5 (test scope only)
- Docker: a multi-stage build, with `maven:3.9-eclipse-temurin-21` for building and `eclipse-temurin:21-jre` for running

## Commands

```
Build:      mvn -q clean package
Test:       mvn -q test
Run:        java -jar target/aggregator.jar --input requirement/recruitment/fv-sec-001-software-engineer-challenge/ad_data.csv --output results/
Help:       java -jar target/aggregator.jar --help
Docker:     docker build -t ad-aggregator . 
            docker run --rm -v "$PWD/data:/data" -v "$PWD/results:/results" ad-aggregator --input /data/ad_data.csv --output /results
Benchmark:  ./scripts/benchmark.sh <path-to-ad_data.csv>   (writes benchmarks/<date>.log)
```

**CLI contract:**

- `--input <file>` is required.
- `--output <dir>` is required. The directory is created if it does not exist.
- Exit codes:
  - `0`: success, including when some rows were skipped
  - `1`: bad arguments or usage
  - `2`: the input file is missing or unreadable, or the header is invalid
  - `3`: an I/O failure while writing the output
- A summary goes to stderr: rows read, rows skipped, campaign count, elapsed time.

## Project Structure

```
pom.xml
Dockerfile
README.md                          → setup, run, libraries, timing, peak memory, decisions
PROMPTS.md                         → raw AI prompts (maintained by the user, do not edit)
SPEC.md                            → this file
src/main/java/vn/flinters/adagg/
  Main.java                        → CLI entry: parse args, wire components, map errors to exit codes
  CliArgs.java                     → argument parsing + validation
  CsvRowParser.java                → splits one line into fields; parses numbers without allocating where it can
  CampaignAggregator.java          → HashMap<String, CampaignStats>; accumulates rows
  CampaignStats.java               → mutable totals (long impressions/clicks/conversions, long spendMicros)
  CampaignReport.java              → immutable result record with CTR/CPA (nullable)
  Rankings.java                    → top-N by CTR desc / CPA asc, with tie-break
  ReportWriter.java                → CSV output and formatting
  ParseStats.java                  → counters for rows read/skipped, plus sample errors
src/test/java/vn/flinters/adagg/   → unit + integration tests (one test class per production class)
src/test/resources/fixtures/       → small hand-crafted CSVs with known expected outputs
scripts/benchmark.sh               → runs the jar under a measurement tool, records time + peak RSS
benchmarks/                        → committed benchmark logs
results/                           → committed top10_ctr.csv, top10_cpa.csv from the real dataset
```

## Processing Design

1. **Read**: `BufferedReader` over `Files.newBufferedReader(path, UTF_8)` with a 1 MiB buffer, reading line by line. Memory stays O(number of campaigns), not O(file size).
2. **Header**: the first line must match the expected columns, in order. Otherwise the program stops with exit code 2.
3. **Parse**: a hand-rolled split on `,` (no regex, no `String.split`). Integers are parsed with `Long.parseLong` on substrings, and `spend` with a custom decimal-to-micros parser.
4. **Validate each row**:
   - exactly 6 fields
   - `campaign_id` is not blank
   - `date` matches `YYYY-MM-DD`
   - numeric fields are non-negative
   - `spend` has at most 6 decimals
   
   Invalid rows are skipped and counted in `ParseStats`, and the first 5 are kept as samples (line number plus reason).
5. **Aggregate**: `map.computeIfAbsent(id, CampaignStats::new).add(...)`.
6. **Report**: convert the stats into `CampaignReport`, then sort to get the top 10 for each list. With only about 50 campaigns, a full sort is fine and simpler than a heap.
7. **Write**: two CSV files with a header line, `\n` line endings and UTF-8.

## Code Style

- Standard Java conventions, 4-space indent, final fields where possible, records for immutable data.
- Small classes with one job each. No static mutable state. No dead or commented-out code. That last rule is an explicit reviewer expectation.
- Javadoc only where the reason isn't obvious (e.g. why spend is stored in micros).

```java
public final class CampaignStats {
    private long impressions;
    private long clicks;
    private long spendMicros;
    private long conversions;

    void add(long impressions, long clicks, long spendMicros, long conversions) {
        this.impressions += impressions;
        this.clicks += clicks;
        this.spendMicros += spendMicros;
        this.conversions += conversions;
    }

    CampaignReport toReport(String campaignId) {
        BigDecimal spend = BigDecimal.valueOf(spendMicros, 6);
        BigDecimal ctr = impressions == 0 ? null
                : BigDecimal.valueOf(clicks).divide(BigDecimal.valueOf(impressions), MathContext.DECIMAL64);
        BigDecimal cpa = conversions == 0 ? null
                : spend.divide(BigDecimal.valueOf(conversions), MathContext.DECIMAL64);
        return new CampaignReport(campaignId, impressions, clicks, spend, conversions, ctr, cpa);
    }
}
```

## Testing Strategy

- **Framework:** JUnit 5, run with `mvn test`.
- **Unit tests:**
  - `CsvRowParser`: valid rows, wrong field count, non-numeric values, negatives, bad dates, decimal edge cases (`0`, `.5`, `12.`, more than 6 decimals)
  - `CampaignStats`: accumulation, CTR/CPA when impressions or conversions are zero
  - `Rankings`: ordering, tie-break, exclusions, fewer than 10 campaigns
  - `ReportWriter`: exact formatting and rounding (`HALF_UP` cases such as 0.00005 → 0.0001)
  - `CliArgs`: missing or unknown flags
- **Integration tests:** run `Main` against the fixture CSVs in a temp dir, then byte-compare the output with the expected CSVs. Cover:
  - an empty file (header only)
  - a missing file, which should give exit 2
  - a file with mixed malformed rows
  - a file without a trailing newline
- **Reference check (manual, once):** independently verify the real-dataset results, e.g. with a one-off `awk` aggregation, before committing `results/`. Record the result in the README.
- **Coverage target:** at least 80% line coverage on non-`Main` classes. This is a guideline, not enforced by a plugin unless you ask for one.

## Error Handling

| Situation | Behavior |
|---|---|
| Missing or unreadable input | stderr message, exit 2 |
| Wrong or missing header | stderr message, exit 2 |
| Malformed row | skip, count, keep a sample; continue |
| Empty data (header only) | write both files with the header only, exit 0 |
| Output dir can't be created or written | stderr message, exit 3 |
| Bad CLI args | usage text, exit 1 |

## Boundaries

- **Always:**
  - stream the input; never load the whole file into memory
  - use fixed-point math for spend
  - run `mvn test` before every commit
  - keep output deterministic
  - update this spec when a decision changes
- **Ask first:**
  - adding any runtime dependency (e.g. a CSV library)
  - switching to parallel processing
  - changing output format or rounding rules
  - committing anything under `requirement/`
  - editing `PROMPTS.md`
- **Never:**
  - commit `ad_data.csv` or the zip
  - use `double` to accumulate money
  - leave dead or commented-out code
  - delete or skip failing tests to get to green
  - rewrite the user's raw prompts in `PROMPTS.md`

## Success Criteria

- [x] `mvn clean package` produces `target/aggregator.jar`, and `mvn test` passes (67 tests, also verified from a clean clone).
- [x] Running on the real 1GB file writes `results/top10_ctr.csv` and `results/top10_cpa.csv`. Both have the exact header and column order shown above, and at most 10 rows each.
- [x] Totals match an independent reference aggregation exactly (`scripts/reference.awk`). CTR has 4 decimals, CPA and spend have 2.
- [x] The CPA list contains no campaign with zero conversions (the dataset has none).
- [x] Peak heap stays small and flat: the program completes with `-Xmx64m` (~42 MiB peak heap).
- [x] Processing the 1GB file takes ≤ 15s wall time on the dev machine (7.2–9.3 s).
- [x] Malformed rows are skipped and reported, never crash the run, and the skipped count is printed.
- [ ] The Dockerfile builds and runs end-to-end and produces the same output. **Not verified:** Docker isn't installed on the dev machine. The README says so.
- [x] `benchmarks/` contains at least one log with wall time and peak memory.
- [x] The README covers setup, running, libraries, processing time, peak memory, and the non-obvious decisions (fixed-point spend, tie-break, null handling, single-pass design).

## Open Questions

1. ~~Is the Maven Wrapper needed?~~ **Resolved:** no `mvnw`. Build with the system Maven 3.8+ and `JAVA_HOME` pointing to JDK 21.
2. Is a 15s target for the 1GB file acceptable, or do you have a different bar?
3. ~~Where should `results/` live?~~ **Resolved:** at the repo root.
4. ~~How should peak memory be measured?~~ **Resolved (2026-11-12):** Docker isn't installed on the dev machine.
   - Peak memory is measured inside the JVM (peak heap usage from the memory beans) plus a run with `-Xmx64m`.
   - The Dockerfile is still written, and the README marks it as not built or verified on this machine.
5. ~~Is JDK 21 installed?~~ **Resolved:** the machine had JDK 11. The user is installing JDK 21, and the target stays Java 21.
