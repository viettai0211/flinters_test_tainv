package vn.flinters.adagg;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Writes campaign reports as CSV: spend and CPA to 2 decimals, CTR to 4, rounding half up. */
final class ReportWriter {

    static final String HEADER =
            "campaign_id,total_impressions,total_clicks,total_spend,total_conversions,CTR,CPA";

    private ReportWriter() {
    }

    static void write(Path file, List<CampaignReport> reports) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.write('\n');
            for (CampaignReport report : reports) {
                writer.write(formatRow(report));
                writer.write('\n');
            }
        }
    }

    private static String formatRow(CampaignReport report) {
        return String.join(",",
                report.campaignId(),
                Long.toString(report.totalImpressions()),
                Long.toString(report.totalClicks()),
                format(report.totalSpend(), 2),
                Long.toString(report.totalConversions()),
                format(report.ctr(), 6),
                format(report.cpa(), 2));
    }

    private static String format(BigDecimal value, int scale) {
        return value == null ? "" : value.setScale(scale, RoundingMode.HALF_UP).toPlainString();
    }
}
