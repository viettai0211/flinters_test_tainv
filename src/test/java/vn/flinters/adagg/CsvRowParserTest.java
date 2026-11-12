package vn.flinters.adagg;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CsvRowParserTest {

    private final CsvRowParser parser = new CsvRowParser();
    private final CsvRowParser.Row row = new CsvRowParser.Row();

    @Test
    void parsesWellFormedLine() throws Exception {
        parser.parse("CMP001,2025-01-01,12000,300,45.50,12", row);

        assertEquals("CMP001", row.campaignId);
        assertEquals(12000, row.impressions);
        assertEquals(300, row.clicks);
        assertEquals(45_500_000L, row.spendMicros);
        assertEquals(12, row.conversions);
    }

    @ParameterizedTest
    @CsvSource({
            "45.50, 45500000",
            "0, 0",
            "0.0, 0",
            ".5, 500000",
            "12., 12000000",
            "1394.62, 1394620000",
            "0.000001, 1",
            "123456.789012, 123456789012"
    })
    void convertsSpendToMicros(String spend, long expectedMicros) {
        assertEquals(expectedMicros, CsvRowParser.parseMicros(spend, 0, spend.length()));
    }

    @Test
    void reusesTargetRowAcrossCalls() throws Exception {
        parser.parse("CMP001,2025-01-01,1,2,3.00,4", row);
        parser.parse("CMP002,2025-01-02,10,20,30.00,40", row);

        assertEquals("CMP002", row.campaignId);
        assertEquals(10, row.impressions);
        assertEquals(40, row.conversions);
    }

    @Test
    void rejectsLineWithTooFewFields() {
        assertThrows(MalformedRowException.class,
                () -> parser.parse("CMP001,2025-01-01,12000,300,45.50", row));
    }

    @Test
    void rejectsLineWithTooManyFields() {
        assertThrows(MalformedRowException.class,
                () -> parser.parse("CMP001,2025-01-01,12000,300,45.50,12,extra", row));
    }

    @ParameterizedTest(name = "rejects \"{0}\"")
    @CsvSource(delimiter = '|', value = {
            "CMP001,2025-01-01,abc,300,45.50,12       | impressions",
            "CMP001,2025-01-01,-5,300,45.50,12        | impressions",
            "CMP001,2025-01-01,12000,,45.50,12        | clicks",
            "CMP001,2025-01-01,12000,3.5,45.50,12     | clicks",
            "CMP001,2025-01-01,12000,300,-1.00,12     | spend",
            "CMP001,2025-01-01,12000,300,4.5.0,12     | spend",
            "CMP001,2025-01-01,12000,300,.,12         | spend",
            "CMP001,2025-01-01,12000,300,1.1234567,12 | spend",
            "CMP001,2025-01-01,12000,300,45.50,1e3    | conversions",
            "',2025-01-01,12000,300,45.50,12'         | campaign_id",
            "'   ,2025-01-01,12000,300,45.50,12'      | campaign_id",
            "\"CMP001\",2025-01-01,12000,300,45.50,12 | campaign_id",
            "CMP001,2025/01/01,12000,300,45.50,12     | date",
            "CMP001,2025-1-01,12000,300,45.50,12      | date",
            "CMP001,2025-13-01,12000,300,45.50,12     | date",
            "CMP001,2025-02-30,12000,300,45.50,12     | date",
            "CMP001,,12000,300,45.50,12               | date"
    })
    void rejectsInvalidFieldWithReasonNamingTheField(String line, String field) {
        MalformedRowException e = assertThrows(MalformedRowException.class, () -> parser.parse(line, row));

        assertTrue(e.getMessage().contains(field), e.getMessage());
    }

    @Test
    void acceptsLeapDay() throws Exception {
        parser.parse("CMP001,2024-02-29,1,1,1.00,1", row);

        assertEquals("CMP001", row.campaignId);
    }
}
