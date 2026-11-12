package vn.flinters.adagg;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Streams CSV lines once and accumulates totals per campaign; memory grows with campaigns, not rows. */
final class CampaignAggregator {

    private final Map<String, CampaignStats> stats = new HashMap<>();
    private final CsvRowParser parser = new CsvRowParser();
    private final CsvRowParser.Row row = new CsvRowParser.Row();

    void aggregate(BufferedReader reader) throws IOException, MalformedRowException {
        reader.readLine();
        String line;
        while ((line = reader.readLine()) != null) {
            parser.parse(line, row);
            stats.computeIfAbsent(row.campaignId, id -> new CampaignStats())
                    .add(row.impressions, row.clicks, row.spendMicros, row.conversions);
        }
    }

    Map<String, CampaignStats> results() {
        return stats;
    }
}
