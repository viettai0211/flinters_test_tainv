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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest {

    private static final String HEADER = "campaign_id,date,impressions,clicks,spend,conversions";

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

    @Test
    void skipsMalformedRowsAndReportsThem(@TempDir Path tmp) throws Exception {
        int code = run("--input", fixture("malformed.csv").toString(), "--output", tmp.toString());

        assertEquals(Main.EXIT_OK, code, err.toString(StandardCharsets.UTF_8));
        assertEquals(Files.readString(fixture("basic_expected_ctr.csv")),
                Files.readString(tmp.resolve("top10_ctr.csv")));
        assertEquals(Files.readString(fixture("basic_expected_cpa.csv")),
                Files.readString(tmp.resolve("top10_cpa.csv")));
        String summary = err.toString(StandardCharsets.UTF_8);
        assertTrue(summary.contains("Rows read: 22, skipped: 7"), summary);
        assertTrue(summary.contains("line 6: invalid impressions"), summary);
    }

    @Test
    void summaryReportsPeakHeap(@TempDir Path tmp) throws Exception {
        int code = run("--input", fixture("basic.csv").toString(), "--output", tmp.toString());

        assertEquals(Main.EXIT_OK, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).matches("(?s).*peak heap: \\d+(\\.\\d)? MiB.*"),
                err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void emptyFileExitsWithInputError(@TempDir Path tmp) throws Exception {
        Path input = Files.writeString(tmp.resolve("empty.csv"), "");

        int code = run("--input", input.toString(), "--output", tmp.resolve("out").toString());

        assertEquals(Main.EXIT_INPUT_ERROR, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("header"));
    }

    @Test
    void wrongHeaderExitsWithInputError(@TempDir Path tmp) throws Exception {
        Path input = Files.writeString(tmp.resolve("bad.csv"),
                "campaign,date,impressions,clicks,spend,conversions\nCMP001,2025-01-01,1,1,1.00,1\n");

        int code = run("--input", input.toString(), "--output", tmp.resolve("out").toString());

        assertEquals(Main.EXIT_INPUT_ERROR, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("header"));
        assertFalse(Files.exists(tmp.resolve("out")));
    }

    @Test
    void headerWithUtf8BomIsAccepted(@TempDir Path tmp) throws Exception {
        Path input = Files.writeString(tmp.resolve("bom.csv"),
                "﻿" + HEADER + "\nCMP001,2025-01-01,100,5,1.00,1\n");

        int code = run("--input", input.toString(), "--output", tmp.toString());

        assertEquals(Main.EXIT_OK, code, err.toString(StandardCharsets.UTF_8));
    }

    @Test
    void headerOnlyFileWritesHeaderOnlyReports(@TempDir Path tmp) throws Exception {
        Path input = Files.writeString(tmp.resolve("header.csv"), HEADER + "\n");
        Path out = tmp.resolve("out");

        int code = run("--input", input.toString(), "--output", out.toString());

        assertEquals(Main.EXIT_OK, code);
        assertEquals(ReportWriter.HEADER + "\n", Files.readString(out.resolve(Main.CTR_FILE)));
        assertEquals(ReportWriter.HEADER + "\n", Files.readString(out.resolve(Main.CPA_FILE)));
    }

    @Test
    void lastRowWithoutTrailingNewlineIsCounted(@TempDir Path tmp) throws Exception {
        Path input = Files.writeString(tmp.resolve("no-newline.csv"),
                HEADER + "\nCMP001,2025-01-01,100,5,1.00,1\nCMP001,2025-01-02,100,5,1.00,1");

        int code = run("--input", input.toString(), "--output", tmp.toString());

        assertEquals(Main.EXIT_OK, code);
        assertEquals(ReportWriter.HEADER + "\nCMP001,200,10,2.00,2,0.0500,1.00\n",
                Files.readString(tmp.resolve(Main.CTR_FILE)));
    }

    @Test
    void unwritableOutputExitsWithOutputError(@TempDir Path tmp) throws Exception {
        Path input = Files.writeString(tmp.resolve("in.csv"), HEADER + "\nCMP001,2025-01-01,1,1,1.00,1\n");
        Path outputIsAFile = Files.writeString(tmp.resolve("not-a-dir"), "x");

        int code = run("--input", input.toString(), "--output", outputIsAFile.toString());

        assertEquals(Main.EXIT_OUTPUT_ERROR, code);
        assertTrue(err.toString(StandardCharsets.UTF_8).contains("not-a-dir"));
    }

    private static Path fixture(String name) throws URISyntaxException {
        return Path.of(MainTest.class.getResource("/fixtures/" + name).toURI());
    }
}
