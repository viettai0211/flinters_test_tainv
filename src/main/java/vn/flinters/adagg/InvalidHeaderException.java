package vn.flinters.adagg;

/** The input's first line is missing or does not match the expected CSV columns. */
public final class InvalidHeaderException extends Exception {

    public InvalidHeaderException(String message) {
        super(message);
    }
}
