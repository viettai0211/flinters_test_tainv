package vn.flinters.adagg;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RankingsTest {

    private static CampaignReport report(String id, String ctr, String cpa) {
        return new CampaignReport(id, 0, 0, BigDecimal.ZERO, 0,
                ctr == null ? null : new BigDecimal(ctr),
                cpa == null ? null : new BigDecimal(cpa));
    }

    private static List<String> ids(List<CampaignReport> reports) {
        return reports.stream().map(CampaignReport::campaignId).toList();
    }

    @Test
    void ordersByCtrDescendingAndExcludesNullCtr() {
        List<CampaignReport> reports = List.of(
                report("A", "0.01", "1"), report("B", "0.05", "1"),
                report("C", null, "1"), report("D", "0.03", "1"));

        assertEquals(List.of("B", "D", "A"), ids(Rankings.topByCtr(reports, 10)));
    }

    @Test
    void ordersByCpaAscendingAndExcludesNullCpa() {
        List<CampaignReport> reports = List.of(
                report("A", "0.01", "9"), report("B", "0.05", null),
                report("C", "0.02", "2"), report("D", "0.03", "5"));

        assertEquals(List.of("C", "D", "A"), ids(Rankings.topByCpa(reports, 10)));
    }

    @Test
    void breaksTiesByCampaignIdAscending() {
        List<CampaignReport> reports = List.of(
                report("Z", "0.05", "2"), report("M", "0.05", "2"), report("A", "0.05", "2"));

        assertEquals(List.of("A", "M", "Z"), ids(Rankings.topByCtr(reports, 10)));
        assertEquals(List.of("A", "M", "Z"), ids(Rankings.topByCpa(reports, 10)));
    }

    @Test
    void limitsToRequestedCount() {
        List<CampaignReport> reports = List.of(
                report("A", "0.01", "1"), report("B", "0.02", "2"), report("C", "0.03", "3"));

        assertEquals(List.of("C", "B"), ids(Rankings.topByCtr(reports, 2)));
        assertEquals(List.of("A", "B"), ids(Rankings.topByCpa(reports, 2)));
    }
}
