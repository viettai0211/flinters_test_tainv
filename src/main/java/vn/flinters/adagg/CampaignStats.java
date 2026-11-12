package vn.flinters.adagg;

/** Running totals for one campaign. Spend is kept in micro-dollars so sums stay exact. */
final class CampaignStats {

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
}
