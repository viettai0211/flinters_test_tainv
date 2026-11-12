package vn.flinters.adagg;

import java.math.BigDecimal;

/**
 * Final aggregated metrics for one campaign. {@code ctr} is null when there were no
 * impressions and {@code cpa} is null when there were no conversions.
 */
record CampaignReport(
        String campaignId,
        long totalImpressions,
        long totalClicks,
        BigDecimal totalSpend,
        long totalConversions,
        BigDecimal ctr,
        BigDecimal cpa) {
}
