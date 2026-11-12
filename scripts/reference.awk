# Independent reference aggregation: totals per campaign, spend summed as integer cents.
# Output: campaign_id,impressions,clicks,spend,conversions,ctr,cpa (ratios to 12 decimals, for ranking).
BEGIN { FS = "," }
NR == 1 { next }
{
    n = split($5, parts, ".")
    frac = (n > 1) ? parts[2] : ""
    if (length(frac) > 2) { bad_precision++ }
    while (length(frac) < 2) frac = frac "0"
    cents = parts[1] * 100 + frac
    imp[$1] += $3; clk[$1] += $4; spend[$1] += cents; conv[$1] += $6
    rows++
}
END {
    for (id in imp) {
        ctr = imp[id] > 0 ? sprintf("%.12f", clk[id] / imp[id]) : ""
        cpa = conv[id] > 0 ? sprintf("%.12f", spend[id] / 100 / conv[id]) : ""
        printf "%s,%d,%d,%d.%02d,%d,%s,%s\n", id, imp[id], clk[id], int(spend[id] / 100), spend[id] % 100, conv[id], ctr, cpa
    }
    printf "rows=%d bad_precision=%d\n", rows, bad_precision > "/dev/stderr"
}
