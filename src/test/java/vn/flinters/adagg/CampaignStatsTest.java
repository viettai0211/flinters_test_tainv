package vn.flinters.adagg;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CampaignStatsTest {

    @Test
    void reportComputesCtrAndCpa() {
        CampaignStats stats = new CampaignStats();
        stats.add(1000, 50, 10_000_000L, 4);

        CampaignReport report = stats.toReport("CMP001");

        assertEquals("CMP001", report.campaignId());
        assertEquals(0, new BigDecimal("0.05").compareTo(report.ctr()));
        assertEquals(0, new BigDecimal("2.5").compareTo(report.cpa()));
        assertEquals(0, new BigDecimal("10").compareTo(report.totalSpend()));
    }

    @Test
    void cpaIsNullWhenNoConversions() {
        CampaignStats stats = new CampaignStats();
        stats.add(1000, 50, 10_000_000L, 0);

        assertNull(stats.toReport("CMP001").cpa());
    }

    @Test
    void ctrIsNullWhenNoImpressions() {
        CampaignStats stats = new CampaignStats();
        stats.add(0, 0, 1_000_000L, 1);

        assertNull(stats.toReport("CMP001").ctr());
    }
}
