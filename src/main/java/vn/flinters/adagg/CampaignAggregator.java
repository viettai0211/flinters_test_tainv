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

    private final Map<String, CampaignStats> stats = new HashMap<>();
    private final ParseStats parseStats = new ParseStats();
    private final CsvRowParser parser = new CsvRowParser();
    private final CsvRowParser.Row row = new CsvRowParser.Row();

    void aggregate(BufferedReader reader) throws IOException {
        reader.readLine();
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

    Map<String, CampaignStats> results() {
        return stats;
    }

    ParseStats parseStats() {
        return parseStats;
    }
}
