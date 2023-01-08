package net.requef.requirecat;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ErrorOr<T> {
    private final T value;
    private final String error;

    private ErrorOr(final T value, final String error) {
        this.value = value;
        this.error = error;
    }

    /**
     * Returns the value.
     * @throws NullPointerException if there was an error.
     *
     * @return the value
     */
    public @NotNull T getValue() {
        return Objects.requireNonNull(value);
    }

    /**
     * Returns the error string.
     * @throws NullPointerException if there were no errors.
     *
     * @return the error string
     */
    public @NotNull String getError() {
        return Objects.requireNonNull(error);
    }

    /**
     * Returns true if there was an error.
     * @return true if there was an error
     */
    public boolean isError() {
        return error != null;
    }

    /**
     * Constructs a new ErrorOr object with the given value (no error occurred).
     * @param value The value to use
     * @return the new ErrorOr object
     * @param <T> The type of the value
     */
    public static <T> @NotNull ErrorOr<T> ok(final @NotNull T value) {
        return new ErrorOr<>(value, null);
    }

    /**
     * Constructs a new ErrorOr object with the given error string (an error occurred).
     * @param error The error string to use
     * @param args The arguments to use for string formatting in error (optional)
     * @return the new ErrorOr object
     * @param <T> The type of the value
     */
    public static <T> @NotNull ErrorOr<T> error(final @NotNull String error, final @NotNull Object... args) {
        return new ErrorOr<>(null, String.format(error, args));
    }
}
