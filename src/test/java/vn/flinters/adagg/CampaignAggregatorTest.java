package vn.flinters.adagg;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CampaignAggregatorTest {

    private static final String HEADER = "campaign_id,date,impressions,clicks,spend,conversions";

    private static Map<String, CampaignStats> aggregate(String csv) throws Exception {
        CampaignAggregator aggregator = new CampaignAggregator();
        aggregator.aggregate(new BufferedReader(new StringReader(csv)));
        return aggregator.results();
    }

    @Test
    void sumsReadmeExamplePerCampaign() throws Exception {
        String csv = HEADER + "\n"
                + "CMP001,2025-01-01,12000,300,45.50,12\n"
                + "CMP002,2025-01-01,8000,120,28.00,4\n"
                + "CMP001,2025-01-02,14000,340,48.20,15\n"
                + "CMP003,2025-01-01,5000,60,15.00,3\n"
                + "CMP002,2025-01-02,8500,150,31.00,5\n";

        Map<String, CampaignStats> results = aggregate(csv);

        assertEquals(3, results.size());
        CampaignStats cmp001 = results.get("CMP001");
        assertEquals(26000, cmp001.impressions());
        assertEquals(640, cmp001.clicks());
        assertEquals(93_700_000L, cmp001.spendMicros());
        assertEquals(27, cmp001.conversions());
        CampaignStats cmp002 = results.get("CMP002");
        assertEquals(16500, cmp002.impressions());
        assertEquals(270, cmp002.clicks());
        assertEquals(59_000_000L, cmp002.spendMicros());
        assertEquals(9, cmp002.conversions());
        assertEquals(5000, results.get("CMP003").impressions());
    }

    @Test
    void acceptsWindowsLineEndings() throws Exception {
        String csv = HEADER + "\r\n"
                + "CMP001,2025-01-01,100,10,1.25,1\r\n"
                + "CMP001,2025-01-02,100,10,1.25,1\r\n";

        Map<String, CampaignStats> results = aggregate(csv);

        assertEquals(2, results.get("CMP001").conversions());
        assertEquals(2_500_000L, results.get("CMP001").spendMicros());
    }

    @Test
    void sumsSpendExactlyWhereDoubleWouldDrift() throws Exception {
        StringBuilder csv = new StringBuilder(HEADER).append('\n');
        for (int i = 0; i < 10; i++) {
            csv.append("CMP001,2025-01-01,1,1,0.10,1\n");
        }

        assertEquals(1_000_000L, aggregate(csv.toString()).get("CMP001").spendMicros());
    }
}
