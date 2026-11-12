package vn.flinters.adagg;

import java.math.BigDecimal;
import java.math.MathContext;

/** Running totals for one campaign. Spend is kept in micro-dollars so sums stay exact. */
final class CampaignStats {

    private static final int MICROS_SCALE = 6;

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

    long impressions() {
        return impressions;
    }

    long clicks() {
        return clicks;
    }

    long spendMicros() {
        return spendMicros;
    }

    long conversions() {
        return conversions;
    }

    /** Ratios keep 34 significant digits so rankings compare effectively exact values; rounding happens on output. */
    CampaignReport toReport(String campaignId) {
        BigDecimal spend = BigDecimal.valueOf(spendMicros, MICROS_SCALE);
        BigDecimal ctr = impressions == 0 ? null
                : BigDecimal.valueOf(clicks).divide(BigDecimal.valueOf(impressions), MathContext.DECIMAL128);
        BigDecimal cpa = conversions == 0 ? null
                : spend.divide(BigDecimal.valueOf(conversions), MathContext.DECIMAL128);
        return new CampaignReport(campaignId, impressions, clicks, spend, conversions, ctr, cpa);
    }
}
