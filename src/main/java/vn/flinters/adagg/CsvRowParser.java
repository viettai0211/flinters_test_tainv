package vn.flinters.adagg;

/**
 * Parses one data line of the ad CSV into a reusable {@link Row}.
 * Numbers are read straight from the line's characters so the hot loop
 * allocates only the campaign id.
 */
final class CsvRowParser {

    static final int FIELD_COUNT = 6;
    private static final int SPEND_SCALE_DIGITS = 6;
    private static final int MAX_LONG_DIGITS = 18;

    /** Mutable holder reused across lines to avoid one allocation per row. */
    static final class Row {
        String campaignId;
        long impressions;
        long clicks;
        long spendMicros;
        long conversions;
    }

    private final int[] fieldEnds = new int[FIELD_COUNT];

    void parse(String line, Row target) throws MalformedRowException {
        splitFields(line);
        target.campaignId = line.substring(0, fieldEnds[0]);
        target.impressions = requireCount(line, 2, "impressions");
        target.clicks = requireCount(line, 3, "clicks");
        target.spendMicros = requireSpend(line);
        target.conversions = requireCount(line, 5, "conversions");
    }

    private void splitFields(String line) throws MalformedRowException {
        int field = 0;
        for (int i = 0, n = line.length(); i < n; i++) {
            if (line.charAt(i) == ',') {
                if (field == FIELD_COUNT - 1) {
                    throw new MalformedRowException("expected " + FIELD_COUNT + " fields, found more");
                }
                fieldEnds[field++] = i;
            }
        }
        if (field != FIELD_COUNT - 1) {
            throw new MalformedRowException("expected " + FIELD_COUNT + " fields, found " + (field + 1));
        }
        fieldEnds[field] = line.length();
    }

    private int fieldStart(int field) {
        return field == 0 ? 0 : fieldEnds[field - 1] + 1;
    }

    private long requireCount(String line, int field, String name) throws MalformedRowException {
        long value = parseCount(line, fieldStart(field), fieldEnds[field]);
        if (value < 0) {
            throw new MalformedRowException("invalid " + name + ": '" + field(line, field) + "'");
        }
        return value;
    }

    private long requireSpend(String line) throws MalformedRowException {
        long value = parseMicros(line, fieldStart(4), fieldEnds[4]);
        if (value < 0) {
            throw new MalformedRowException("invalid spend: '" + field(line, 4) + "'");
        }
        return value;
    }

    private String field(String line, int field) {
        return line.substring(fieldStart(field), fieldEnds[field]);
    }

    /** Parses a non-negative integer in {@code s[from, to)}; returns -1 if invalid. */
    static long parseCount(String s, int from, int to) {
        int length = to - from;
        if (length == 0 || length > MAX_LONG_DIGITS) {
            return -1;
        }
        long value = 0;
        for (int i = from; i < to; i++) {
            int digit = s.charAt(i) - '0';
            if (digit < 0 || digit > 9) {
                return -1;
            }
            value = value * 10 + digit;
        }
        return value;
    }

    /**
     * Parses a non-negative decimal in {@code s[from, to)} into millionths
     * (e.g. "45.5" becomes 45500000); returns -1 if invalid or more precise than 6 decimals.
     */
    static long parseMicros(String s, int from, int to) {
        int dot = s.indexOf('.', from);
        int intEnd = dot < 0 || dot >= to ? to : dot;
        int fracStart = intEnd == to ? to : intEnd + 1;
        int intDigits = intEnd - from;
        int fracDigits = to - fracStart;
        if (intDigits + fracDigits == 0 || intDigits > MAX_LONG_DIGITS - SPEND_SCALE_DIGITS
                || fracDigits > SPEND_SCALE_DIGITS) {
            return -1;
        }
        long whole = intDigits == 0 ? 0 : parseCount(s, from, intEnd);
        long fraction = fracDigits == 0 ? 0 : parseCount(s, fracStart, to);
        if (whole < 0 || fraction < 0) {
            return -1;
        }
        for (int i = fracDigits; i < SPEND_SCALE_DIGITS; i++) {
            fraction *= 10;
        }
        return whole * 1_000_000L + fraction;
    }
}
