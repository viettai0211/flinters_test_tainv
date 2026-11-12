# Tasks: Ad Performance Aggregator

Plan: [`tasks/plan.md`](plan.md) · Spec: [`SPEC.md`](../SPEC.md)
Package root: `src/main/java/vn/flinters/adagg/` (tests mirror it under `src/test/java/...`)

---

## Phase 1: Working end-to-end path

### Task 1: Maven project skeleton + CLI argument handling
**Description:** Create the Maven project (Java 21, JUnit 5, executable jar named `aggregator.jar`). The Maven Wrapper was dropped: its plugin is not cached and `mvnw` downloads Maven at runtime, so the build uses `mvn` directly. Add `CliArgs`, which parses `--input`, `--output` and `--help`. Add a minimal `Main` that validates the arguments and exits 1 on bad usage or 2 when the input file is missing.

**Acceptance criteria:**
- [x] `mvn clean package` produces `target/aggregator.jar`
- [x] Missing or unknown flags print the usage text and exit 1. `--help` prints the usage text and exits 0.
- [x] A missing input file prints a message to stderr and exits 2

**Verification:**
- [x] `mvn test` passes (`CliArgsTest`)
- [x] Manual: `java -jar target/aggregator.jar` exits 1; `--input nope.csv --output out` exits 2

**Dependencies:** None
**Files:** `pom.xml`, `.gitignore` (also ignores `.idea/` and `requirement/`, per the user), `.gitattributes`, `Main.java`, `CliArgs.java`, `UsageException.java`, `CliArgsTest.java`, `MainTest.java`
**Precondition:** `java -version` reports 21 (set `JAVA_HOME` to the JDK 21 folder)
**Scope:** M

---

### Task 2: Parse rows and add them up per campaign
**Description:** `CsvRowParser` splits a well-formed line into fields without regex and converts `spend` to micro-dollars. `CampaignStats` keeps the running totals. `CampaignAggregator` reads lines from a `BufferedReader`, skips the header, and adds each row to the map.

**Acceptance criteria:**
- [x] Spend values such as `45.50`, `0`, `.5` and `12.` convert to the correct micro-dollar values
- [x] Aggregating the README's 5-row example gives the right totals for CMP001/002/003
- [x] Both `\n` and `\r\n` line endings are accepted

**Verification:**
- [x] `mvn test` passes (`CsvRowParserTest`, `CampaignAggregatorTest`)

**Dependencies:** Task 1
**Files:** `CsvRowParser.java`, `MalformedRowException.java`, `CampaignStats.java`, `CampaignAggregator.java`, matching tests
**Scope:** M

---

### Task 3: Rank campaigns, write the reports, connect it all in `Main`
**Description:** `CampaignReport` is a record with CTR and CPA, both of which can be null. `Rankings` returns the top N by CTR (highest first) and by CPA (lowest first), excluding campaigns where the metric is null and breaking ties by `campaign_id`. `ReportWriter` writes the CSV with the spec's header and formatting: spend 2 decimals, CTR 4, CPA 2, rounding halves up, and an empty cell when CPA is null. `Main` connects the parts and prints a summary to stderr.

**Acceptance criteria:**
- [ ] Ordering, tie-break and exclusions are covered by `RankingsTest`, including the case with fewer than 10 campaigns
- [ ] The exact formatting is tested, including the case where 0.00005 rounds to 0.0001 and the empty CPA cell
- [ ] An integration test runs `Main` on `fixtures/basic.csv` (about 15 campaigns) and compares both outputs byte for byte with `fixtures/basic_expected_{ctr,cpa}.csv`

**Verification:**
- [ ] `mvn test` passes
- [ ] Manual: run the jar on the fixture and inspect the two files

**Dependencies:** Task 2
**Files:** `CampaignReport.java`, `Rankings.java`, `ReportWriter.java`, `Main.java`, tests + `src/test/resources/fixtures/basic*.csv`
**Scope:** M

### ✅ Checkpoint A: Small fixture works end-to-end
- [ ] `mvn clean package` and `mvn test` pass
- [ ] Review with the human

---

## Phase 2: Real-data baseline

### Task 4: First run on the 1GB file and a timing baseline
**Description:** Run the jar on the real `ad_data.csv` with `-Xmx64m` and record the time and summary. If the run takes longer than 15s, apply the parsing fix agreed in plan.md (parse numbers straight from the line text) and re-measure. Do not add parallel processing without approval.

**Acceptance criteria:**
- [ ] The run finishes with `-Xmx64m` and writes both files
- [ ] Rows read, rows skipped, campaign count and wall time are written down in `benchmarks/baseline-notes.md`
- [ ] Either the time is ≤ 15s, or the shortfall and the proposed fix are reported to the human

**Verification:**
- [ ] Manual: `java -Xmx64m -jar target/aggregator.jar --input requirement/recruitment/fv-sec-001-software-engineer-challenge/ad_data.csv --output results/`

**Dependencies:** Task 3
**Files:** `benchmarks/baseline-notes.md` (and `CsvRowParser.java` if optimised)
**Scope:** S

### ✅ Checkpoint B: Real data
- [ ] Timing is acceptable, or a fix is agreed with the human

---

## Phase 3: Robustness

### Task 5: Row validation and skipped-row reporting
**Description:** Validate every row:
- exactly 6 fields
- `campaign_id` is not blank
- `date` is in `YYYY-MM-DD` format
- numbers are non-negative integers
- `spend` has at most 6 decimals

Invalid rows are skipped. `ParseStats` counts the skipped rows and keeps the first 5 samples (line number plus reason), and `Main` prints them to stderr.

**Acceptance criteria:**
- [ ] Each invalid-row case has a unit test showing it is rejected with a reason
- [ ] An integration test on `fixtures/malformed.csv` produces the same output as a version of the file with only the valid rows, prints the correct skipped count, and exits 0

**Verification:**
- [ ] `mvn test` passes

**Dependencies:** Task 3
**Files:** `CsvRowParser.java`, `ParseStats.java`, `CampaignAggregator.java`, `Main.java`, tests + fixture
**Scope:** M

---

### Task 6: Header and file edge cases, output errors, rounding
**Description:** Cover the rest of the spec's Error Handling table:
- a wrong or missing header exits 2
- a file with only a header writes two header-only files and exits 0
- a file with no trailing newline is processed normally
- an output directory that can't be written exits 3
- zero impressions gives a null CTR (excluded from the CTR list)
- zero conversions gives a null CPA

**Acceptance criteria:**
- [ ] Each case above has an integration test in a temporary directory
- [ ] `.gitattributes` pins `src/test/resources/**` to LF line endings

**Verification:**
- [ ] `mvn test` passes

**Dependencies:** Task 5
**Files:** `Main.java`, `CampaignAggregator.java`, `ReportWriter.java`, `MainIntegrationTest.java`, fixtures, `.gitattributes`
**Scope:** M

### ✅ Checkpoint C: Robust
- [ ] Every row in the spec's Error Handling table has a passing test
- [ ] No dead or commented-out code (`git grep -n "^\s*//.*;"` comes back empty)

---

## Phase 4: Deliverables

### Task 7: Check results against an independent calculation, commit `results/`
**Description:** Re-run on the real file, then compare the totals for every campaign in both output files against an independent calculation. The method is `awk`, or a separate JShell script if `awk` is declined, and it needs the human's OK because it scans the 1GB file. Commit `results/top10_ctr.csv` and `results/top10_cpa.csv`.

**Acceptance criteria:**
- [ ] Every total, CTR and CPA in both files matches the reference after rounding
- [ ] No campaign with zero conversions appears in `top10_cpa.csv`

**Verification:**
- [ ] Manual: a diff of the reference output against `results/` is empty

**Dependencies:** Checkpoint C
**Files:** `results/top10_ctr.csv`, `results/top10_cpa.csv`
**Scope:** S

---

### Task 8: Benchmark script and logs
**Description:** `scripts/benchmark.sh <csv>` builds the jar, runs it with `-Xmx64m`, and writes the wall time, peak heap and the program summary to `benchmarks/<date>.log`. Docker isn't available, so peak heap is measured inside the JVM (from the memory beans) and printed in the summary on stderr.

**Acceptance criteria:**
- [ ] At least one committed log contains the wall time and peak memory for the 1GB file

**Verification:**
- [ ] Manual: run the script and check the log

**Dependencies:** Task 7
**Files:** `scripts/benchmark.sh`, `benchmarks/*.log`
**Scope:** S

---

### Task 9: Dockerfile
**Description:** A multi-stage build: `maven:3.9-eclipse-temurin-21` builds the jar, and `eclipse-temurin:21-jre` runs it with `ENTRYPOINT ["java","-jar","/app/aggregator.jar"]`. Add a `.dockerignore` that excludes the CSV, zip, `target/` and `requirement/`.

**Acceptance criteria:**
- [ ] The Dockerfile follows the spec's Commands section
- [ ] The README says clearly that the image was not built or verified here (no Docker on the dev machine)

**Verification:**
- [ ] Manual review only (Docker isn't installed)

**Dependencies:** Task 7
**Files:** `Dockerfile`, `.dockerignore`
**Scope:** S

---

### Task 10: README
**Description:** Rewrite `README.md` to cover:
- setup (JDK 21, Maven 3.8+, Docker)
- how to run
- libraries used
- processing time and peak memory (taken from the Task 8 logs)
- design decisions: micro-dollar spend, tie-break, null handling, single pass, skip-and-count, exit codes
- how to run the tests

**Acceptance criteria:**
- [ ] Every item the challenge requires in the README is present
- [ ] The numbers match the committed benchmark log

**Verification:**
- [ ] Manual: follow the README from a clean clone; the commands work as written

**Dependencies:** Tasks 8, 9
**Files:** `README.md`
**Scope:** S

### ✅ Checkpoint D: Complete
- [ ] Every item in the spec's Success Criteria is ticked
- [ ] `PROMPTS.md` was left untouched by the agent
- [ ] Human review before submission
