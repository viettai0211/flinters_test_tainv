package vn.flinters.adagg;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CliArgsTest {

    @Test
    void parsesInputAndOutput() throws Exception {
        CliArgs args = CliArgs.parse(new String[] {"--input", "data.csv", "--output", "results/"});

        assertEquals(Path.of("data.csv"), args.input());
        assertEquals(Path.of("results/"), args.output());
    }

    @Test
    void acceptsFlagsInAnyOrder() throws Exception {
        CliArgs args = CliArgs.parse(new String[] {"--output", "out", "--input", "in.csv"});

        assertEquals(Path.of("in.csv"), args.input());
        assertEquals(Path.of("out"), args.output());
    }

    @Test
    void rejectsMissingInput() {
        UsageException e = assertThrows(UsageException.class,
                () -> CliArgs.parse(new String[] {"--output", "out"}));

        assertTrue(e.getMessage().contains("--input"));
    }

    @Test
    void rejectsMissingOutput() {
        UsageException e = assertThrows(UsageException.class,
                () -> CliArgs.parse(new String[] {"--input", "in.csv"}));

        assertTrue(e.getMessage().contains("--output"));
    }

    @Test
    void rejectsFlagWithoutValue() {
        assertThrows(UsageException.class,
                () -> CliArgs.parse(new String[] {"--output", "out", "--input"}));
    }

    @Test
    void rejectsUnknownFlag() {
        UsageException e = assertThrows(UsageException.class,
                () -> CliArgs.parse(new String[] {"--input", "a", "--output", "b", "--verbose"}));

        assertTrue(e.getMessage().contains("--verbose"));
    }

    @Test
    void rejectsDuplicateFlag() {
        assertThrows(UsageException.class,
                () -> CliArgs.parse(new String[] {"--input", "a", "--input", "b", "--output", "c"}));
    }

    @Test
    void detectsHelpFlag() {
        assertTrue(CliArgs.isHelpRequested(new String[] {"--input", "a", "--help"}));
        assertTrue(CliArgs.isHelpRequested(new String[] {"-h"}));
        assertFalse(CliArgs.isHelpRequested(new String[] {"--input", "a", "--output", "b"}));
    }
}
