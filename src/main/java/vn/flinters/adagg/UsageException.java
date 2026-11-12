package vn.flinters.adagg;

/** Thrown when command-line arguments are missing or invalid. */
public final class UsageException extends Exception {

    public UsageException(String message) {
        super(message);
    }
}
