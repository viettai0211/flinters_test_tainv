package vn.flinters.adagg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Counts data rows read and skipped, keeping the first few skip reasons for diagnostics. */
final class ParseStats {

    static final int MAX_SAMPLES = 5;

    private long rowsRead;
    private long rowsSkipped;
    private final List<String> samples = new ArrayList<>(MAX_SAMPLES);

    void recordRow() {
        rowsRead++;
    }

    void recordSkip(long lineNumber, String reason) {
        rowsSkipped++;
        if (samples.size() < MAX_SAMPLES) {
            samples.add("line " + lineNumber + ": " + reason);
        }
    }

    long rowsRead() {
        return rowsRead;
    }

    long rowsSkipped() {
        return rowsSkipped;
    }

    List<String> samples() {
        return Collections.unmodifiableList(samples);
    }
}
