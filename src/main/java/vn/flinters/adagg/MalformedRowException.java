package vn.flinters.adagg;

/**
 * A data row that cannot be parsed. Thrown once per bad row, so the stack trace
 * is suppressed to keep skipping cheap.
 */
public final class MalformedRowException extends Exception {

    public MalformedRowException(String reason) {
        super(reason, null, false, false);
    }
}
