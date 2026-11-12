package vn.flinters.adagg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    private int run(String... args) {
        return Main.run(args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8));
    }

    @Test
    void noArgumentsPrintsUsageAndExitsWithUsageError() {
        int code = run();

        assertEquals(Main.EXIT_USAGE, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("Usage:"));
    }

    @Test
    void helpPrintsUsageAndExitsSuccessfully() {
        int code = run("--help");

        assertEquals(Main.EXIT_OK, code);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("Usage:"));
    }

    @Test
    void missingInputFileExitsWithInputError(@TempDir Path tmp) {
        Path missing = tmp.resolve("nope.csv");

        int code = run("--input", missing.toString(), "--output", tmp.resolve("out").toString());

        assertEquals(Main.EXIT_INPUT_ERROR, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("nope.csv"));
    }

    @Test
    void directoryAsInputExitsWithInputError(@TempDir Path tmp) {
        int code = run("--input", tmp.toString(), "--output", tmp.resolve("out").toString());

        assertEquals(Main.EXIT_INPUT_ERROR, code);
    }

    @Test
    void writesBothReportsMatchingExpectedFixtures(@TempDir Path tmp) throws Exception {
        Path outputDir = tmp.resolve("nested/results");

        int code = run("--input", fixture("basic.csv").toString(), "--output", outputDir.toString());

        assertEquals(Main.EXIT_OK, code, err.toString(StandardCharsets.UTF_8));
        assertEquals(Files.readString(fixture("basic_expected_ctr.csv")),
                Files.readString(outputDir.resolve("top10_ctr.csv")));
        assertEquals(Files.readString(fixture("basic_expected_cpa.csv")),
                Files.readString(outputDir.resolve("top10_cpa.csv")));
    }

    private static Path fixture(String name) throws URISyntaxException {
        return Path.of(MainTest.class.getResource("/fixtures/" + name).toURI());
    }
}
