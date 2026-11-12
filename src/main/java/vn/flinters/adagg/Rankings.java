package vn.flinters.adagg;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/** Top-N selections. Ties are broken by campaign id so output is deterministic. */
final class Rankings {

    private static final Comparator<CampaignReport> BY_ID = Comparator.comparing(CampaignReport::campaignId);

    private Rankings() {
    }

    static List<CampaignReport> topByCtr(Collection<CampaignReport> reports, int limit) {
        return top(reports, CampaignReport::ctr, Comparator.comparing(CampaignReport::ctr).reversed(), limit);
    }

    static List<CampaignReport> topByCpa(Collection<CampaignReport> reports, int limit) {
        return top(reports, CampaignReport::cpa, Comparator.comparing(CampaignReport::cpa), limit);
    }

    private static List<CampaignReport> top(Collection<CampaignReport> reports,
                                            Function<CampaignReport, ?> metric,
                                            Comparator<CampaignReport> order,
                                            int limit) {
        return reports.stream()
                .filter(report -> Objects.nonNull(metric.apply(report)))
                .sorted(order.thenComparing(BY_ID))
                .limit(limit)
                .toList();
    }
}
