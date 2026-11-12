package vn.flinters.adagg;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Streams CSV lines once and accumulates totals per campaign; memory grows with campaigns, not rows.
 * Malformed rows are skipped and recorded in {@link ParseStats} instead of aborting the run.
 */
final class CampaignAggregator {

    static final String EXPECTED_HEADER = "campaign_id,date,impressions,clicks,spend,conversions";
    private static final String UTF8_BOM = "﻿";

    private final Map<String, CampaignStats> stats = new HashMap<>();
    private final ParseStats parseStats = new ParseStats();
    private final CsvRowParser parser = new CsvRowParser();
    private final CsvRowParser.Row row = new CsvRowParser.Row();

    void aggregate(BufferedReader reader) throws IOException, InvalidHeaderException {
        requireHeader(reader.readLine());
        long lineNumber = 1;
        String line;
        while ((line = reader.readLine()) != null) {
            lineNumber++;
            parseStats.recordRow();
            try {
                parser.parse(line, row);
            } catch (MalformedRowException e) {
                parseStats.recordSkip(lineNumber, e.getMessage());
                continue;
            }
            stats.computeIfAbsent(row.campaignId, id -> new CampaignStats())
                    .add(row.impressions, row.clicks, row.spendMicros, row.conversions);
        }
    }

    private static void requireHeader(String header) throws InvalidHeaderException {
        if (header == null) {
            throw new InvalidHeaderException("file is empty, expected header: " + EXPECTED_HEADER);
        }
        String withoutBom = header.startsWith(UTF8_BOM) ? header.substring(1) : header;
        if (!withoutBom.equals(EXPECTED_HEADER)) {
            throw new InvalidHeaderException(
                    "unexpected header '" + header + "', expected: " + EXPECTED_HEADER);
        }
    }

    Map<String, CampaignStats> results() {
        return stats;
    }

    ParseStats parseStats() {
        return parseStats;
    }
}
