package vn.flinters.adagg;

import java.nio.file.Path;
import java.util.Arrays;

/** Validated command-line arguments. */
public record CliArgs(Path input, Path output) {

    static final String USAGE = """
            Usage: java -jar aggregator.jar --input <file.csv> --output <directory>

              --input   CSV file with columns campaign_id,date,impressions,clicks,spend,conversions
              --output  directory for top10_ctr.csv and top10_cpa.csv (created if missing)
              --help    show this message
            """;

    static boolean isHelpRequested(String[] args) {
        return Arrays.stream(args).anyMatch(arg -> arg.equals("--help") || arg.equals("-h"));
    }

    static CliArgs parse(String[] args) throws UsageException {
        String input = null;
        String output = null;
        for (int i = 0; i < args.length; i += 2) {
            String flag = args[i];
            if (!flag.equals("--input") && !flag.equals("--output")) {
                throw new UsageException("Unknown argument: " + flag);
            }
            if (i + 1 >= args.length) {
                throw new UsageException("Missing value for " + flag);
            }
            String value = args[i + 1];
            if (flag.equals("--input")) {
                input = requireUnset(input, flag, value);
            } else {
                output = requireUnset(output, flag, value);
            }
        }
        if (input == null) {
            throw new UsageException("Missing required argument --input");
        }
        if (output == null) {
            throw new UsageException("Missing required argument --output");
        }
        return new CliArgs(Path.of(input), Path.of(output));
    }

    private static String requireUnset(String current, String flag, String value) throws UsageException {
        if (current != null) {
            throw new UsageException("Duplicate argument: " + flag);
        }
        return value;
    }
}
