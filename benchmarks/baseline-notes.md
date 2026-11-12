# Baseline run: Task 4 (2026-11-12)

Command (run twice, back to back):

```
java -Xmx64m -jar target/aggregator.jar \
  --input requirement/recruitment/fv-sec-001-software-engineer-challenge/ad_data.csv \
  --output <tmp>
```

| Item | Value |
|---|---|
| Input | `ad_data.csv`, 1,043,304,870 bytes, 26,843,545 lines (26,843,544 data rows + header) |
| Rows skipped | 0. At this stage any malformed row stopped the run with exit 2, and both runs exited 0. |
| Campaigns | 50 |
| Elapsed (program-reported) | run 1: 6,950 ms (cold file cache), run 2: 4,928 ms |
| Heap cap | `-Xmx64m`. Both runs completed. |
| Environment | Windows 10 Pro, JDK 21.0.12.1 (HotSpot), Maven 3.8.8 |

## Observations

- This is well under the 15 s target, so no parsing optimisation is needed. Checkpoint B passes without changes.
- The top campaigns' CTRs all round to `0.0275` when shown to 4 decimals. The ranking depends on the full-precision values: CTR and CPA are computed to 34 significant digits and only rounded when written out. Tie-breaking by `campaign_id` applies only to exact ties.
