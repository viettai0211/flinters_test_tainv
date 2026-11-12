package vn.flinters.adagg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryType;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** CLI entry point; the only place that maps failures to exit codes. */
public final class Main {

    static final int EXIT_OK = 0;
    static final int EXIT_USAGE = 1;
    static final int EXIT_INPUT_ERROR = 2;
    static final int EXIT_OUTPUT_ERROR = 3;

    static final String CTR_FILE = "top10_ctr.csv";
    static final String CPA_FILE = "top10_cpa.csv";
    private static final int TOP_N = 10;
    private static final int READ_BUFFER_CHARS = 1 << 20;
    private static final double BYTES_PER_MIB = 1024.0 * 1024.0;

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(run(args, System.out, System.err));
    }

    static int run(String[] rawArgs, PrintStream out, PrintStream err) {
        if (CliArgs.isHelpRequested(rawArgs)) {
            out.print(CliArgs.USAGE);
            return EXIT_OK;
        }
        CliArgs args;
        try {
            args = CliArgs.parse(rawArgs);
        } catch (UsageException e) {
            err.println("Error: " + e.getMessage());
            err.print(CliArgs.USAGE);
            return EXIT_USAGE;
        }
        Path input = args.input();
        if (!Files.isRegularFile(input) || !Files.isReadable(input)) {
            err.println("Error: input file not found or not readable: " + input);
            return EXIT_INPUT_ERROR;
        }

        long startNanos = System.nanoTime();
        CampaignAggregator aggregator = new CampaignAggregator();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(Files.newInputStream(input), StandardCharsets.UTF_8), READ_BUFFER_CHARS)) {
            aggregator.aggregate(reader);
        } catch (IOException e) {
            err.println("Error: failed to read " + input + ": " + e.getMessage());
            return EXIT_INPUT_ERROR;
        } catch (InvalidHeaderException e) {
            err.println("Error: invalid header in " + input + ": " + e.getMessage());
            return EXIT_INPUT_ERROR;
        }

        List<CampaignReport> reports = toReports(aggregator.results());
        try {
            Files.createDirectories(args.output());
            ReportWriter.write(args.output().resolve(CTR_FILE), Rankings.topByCtr(reports, TOP_N));
            ReportWriter.write(args.output().resolve(CPA_FILE), Rankings.topByCpa(reports, TOP_N));
        } catch (IOException e) {
            err.println("Error: failed to write results to " + args.output() + ": " + e.getMessage());
            return EXIT_OUTPUT_ERROR;
        }

        long elapsedMillis = (System.nanoTime() - startNanos) / 1_000_000;
        printSummary(err, aggregator.parseStats(), reports.size(), elapsedMillis);
        return EXIT_OK;
    }

    private static void printSummary(PrintStream err, ParseStats stats, int campaigns, long elapsedMillis) {
        err.printf(Locale.ROOT, "Rows read: %d, skipped: %d, campaigns: %d, elapsed: %d ms, peak heap: %.1f MiB%n",
                stats.rowsRead(), stats.rowsSkipped(), campaigns, elapsedMillis, peakHeapBytes() / BYTES_PER_MIB);
        for (String sample : stats.samples()) {
            err.println("  skipped " + sample);
        }
    }

    /** Sum of each heap pool's peak usage: an upper bound on the heap used at any one moment. */
    private static double peakHeapBytes() {
        return ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == MemoryType.HEAP)
                .mapToLong(pool -> pool.getPeakUsage().getUsed())
                .sum();
    }

    private static List<CampaignReport> toReports(Map<String, CampaignStats> stats) {
        return stats.entrySet().stream()
                .map(entry -> entry.getValue().toReport(entry.getKey()))
                .toList();
    }
}
