package vn.flinters.adagg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReportWriterTest {

    private static final String HEADER =
            "campaign_id,total_impressions,total_clicks,total_spend,total_conversions,CTR,CPA\n";

    @Test
    void writesHeaderAndFormattedRow(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("out.csv");
        CampaignReport report = new CampaignReport("CMP042", 125000, 6250,
                new BigDecimal("12500.5"), 625, new BigDecimal("0.05"), new BigDecimal("20.0008"));

        ReportWriter.write(file, List.of(report));

        assertEquals(HEADER + "CMP042,125000,6250,12500.50,625,0.0500,20.00\n",
                Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void roundsHalfUp(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("out.csv");
        CampaignReport report = new CampaignReport("C", 20000, 1,
                new BigDecimal("15.005"), 8, new BigDecimal("0.00005"), new BigDecimal("1.875"));

        ReportWriter.write(file, List.of(report));

        assertEquals(HEADER + "C,20000,1,15.01,8,0.0001,1.88\n", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void writesEmptyCellsForNullMetrics(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("out.csv");
        CampaignReport report = new CampaignReport("C", 0, 0, BigDecimal.ONE, 0, null, null);

        ReportWriter.write(file, List.of(report));

        assertEquals(HEADER + "C,0,0,1.00,0,,\n", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void writesHeaderOnlyForNoReports(@TempDir Path tmp) throws Exception {
        Path file = tmp.resolve("out.csv");

        ReportWriter.write(file, List.of());

        assertEquals(HEADER, Files.readString(file, StandardCharsets.UTF_8));
    }
}
