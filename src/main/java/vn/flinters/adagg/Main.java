package vn.flinters.adagg;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** CLI entry point; the only place that maps failures to exit codes. */
public final class Main {

    static final int EXIT_OK = 0;
    static final int EXIT_USAGE = 1;
    static final int EXIT_INPUT_ERROR = 2;
    static final int EXIT_OUTPUT_ERROR = 3;

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
        return EXIT_OK;
    }
}
