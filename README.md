# Ad Performance Aggregator

Flinters Việt Nam technical exam: **FV-SEC001 – Software Engineer Challenge**.

This is a Java command-line tool. It reads the ~1 GB `ad_data.csv` once, adds up impressions, clicks, spend and conversions per `campaign_id`, and writes two reports:

- [`results/top10_ctr.csv`](results/top10_ctr.csv): the 10 campaigns with the highest CTR (clicks / impressions)
- [`results/top10_cpa.csv`](results/top10_cpa.csv): the 10 campaigns with the lowest CPA (spend / conversions), leaving out campaigns with zero conversions

## Results on the 1 GB dataset

| Metric | Value |
|---|---|
| Input | 1,043,304,870 bytes, 26,843,544 data rows, 50 campaigns |
| Rows skipped as malformed | 0 |
| Processing time | **~7 s** (7.0–9.0 s in the program, 7.2–9.3 s wall time including JVM startup, 3 runs) |
| Peak heap | **~42 MiB** (with the heap capped at `-Xmx64m`) |
| Machine | Windows 10 Pro, 8 logical CPUs, JDK 21.0.12.1 |

The full log is in [`benchmarks/2026-11-12_193723.log`](benchmarks/2026-11-12_193723.log).

**Correctness check:** both result files were compared with an independent `awk` aggregation ([`scripts/reference.awk`](scripts/reference.awk)), which adds up spend as whole cents. Both files match exactly.

## Requirements

- JDK 21 (make sure `JAVA_HOME` points to it)
- Maven 3.8 or newer
- No runtime libraries are needed. JUnit 5 is used for tests only.

## Build and run

```bash
mvn clean package            # compiles, runs the tests, builds target/aggregator.jar

java -jar target/aggregator.jar \
  --input  path/to/ad_data.csv \
  --output results/
```

| Option | Meaning |
|---|---|
| `--input <file>` | Input CSV with the header `campaign_id,date,impressions,clicks,spend,conversions` (required) |
| `--output <dir>` | Folder for `top10_ctr.csv` and `top10_cpa.csv`. It is created if it doesn't exist. (required) |
| `--help` | Show usage |

A summary goes to stderr: rows read and skipped, campaign count, elapsed time, peak heap, and up to 5 sample reasons for skipped rows.

**Exit codes**

| Code | Meaning |
|---|---|
| `0` | Success, even if some rows were skipped |
| `1` | Invalid command-line arguments |
| `2` | The input file is missing or unreadable, empty, or has the wrong header |
| `3` | The output folder can't be created or written |

### Tests

```bash
mvn test
```

There are 67 tests. Unit tests cover parsing, validation, totals, rankings and formatting. Integration tests run `Main` on fixture CSVs and compare the output files byte for byte. The expected values for [`basic.csv`](src/test/resources/fixtures/basic.csv) were calculated by hand.

### Benchmark

```bash
scripts/benchmark.sh path/to/ad_data.csv 3   # writes benchmarks/<timestamp>.log
```

### Docker

```bash
docker build -t ad-aggregator .
docker run --rm -v "$PWD/data:/data" -v "$PWD/results:/results" \
  ad-aggregator --input /data/ad_data.csv --output /results
```

> ⚠️ Docker isn't installed on the machine this was written on, so **this image has not been built or tested**. The Dockerfile is a standard two-stage build: Maven 3.9 with JDK 21 to build, then a JRE 21 image to run.

## Design decisions

- **One streaming pass.** The file is read line by line through a `BufferedReader` with a 1 MiB buffer, and totals are kept in a `HashMap` keyed by campaign. Memory depends on the number of campaigns (50), not on the file size. That's why it runs within a 64 MB heap.
- **Hand-written parsing.** Lines are split on commas without regex or `String.split`. Numbers are read straight from the line's characters, and the parsed values go into one reused row object. The only allocation per row is the campaign id string.
- **Spend is stored as whole micro-dollars, not as a floating-point number.** Each spend value is converted to a `long` count of millionths of a dollar (e.g. `45.50` becomes `45500000`). Summing ~27M floating-point values would drift. Whole numbers keep the totals exact, as the challenge requires ("match expected values precisely").
- **Full-precision ranking, rounding only on output.** CTR and CPA are calculated as `BigDecimal` values with 34 significant digits, and they are rounded only when written out: CTR to 4 decimals, spend and CPA to 2, halves rounded up. On this dataset every top-10 CTR rounds to `0.0275`, so ranking on rounded values would give a meaningless order.
- **Fixed tie-break.** When two metric values are exactly equal, the lower `campaign_id` comes first, so the output is the same on every run.
- **Missing metrics.** CPA is `null` when a campaign has zero conversions. Those campaigns are left out of the CPA ranking and get an empty CPA cell in the CTR report. CTR is handled the same way when impressions are zero.
- **Bad rows are skipped, not fatal.** A row is skipped if it has the wrong number of fields, a blank or quoted `campaign_id`, an invalid date (for example `2025-02-30`), a negative or non-integer count, or a spend value that isn't a number or has more than 6 decimals. Skipped rows are counted and a few sample reasons are printed. A wrong header stops the run, because it means the file isn't this dataset.
- **No parallel processing.** A single pass already finishes in about 7 s, well under the 15 s target. Splitting the file across threads would add complexity for little benefit.

## Project layout

```
src/main/java/vn/flinters/adagg/
  Main.java               command-line entry point, maps errors to exit codes
  CliArgs.java            argument parsing
  CampaignAggregator.java streaming loop, header check, per-campaign totals
  CsvRowParser.java       line splitting, row validation, number parsing
  CampaignStats.java      running totals; calculates CTR and CPA
  CampaignReport.java     final metrics for one campaign
  Rankings.java           top-N by CTR and by CPA
  ReportWriter.java       CSV output formatting
  ParseStats.java         rows read and skipped, sample reasons
src/test/...              JUnit 5 tests and fixtures
scripts/                  benchmark.sh, reference.awk
benchmarks/               benchmark logs
results/                  top10_ctr.csv, top10_cpa.csv
SPEC.md, tasks/           spec and implementation plan
```

## AI assistance

This project was built with Claude Code. The raw prompts are in [`PROMPTS.md`](PROMPTS.md).
