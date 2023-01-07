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

    public @NotNull T getValue() {
        return Objects.requireNonNull(value);
    }

    public @NotNull String getError() {
        return Objects.requireNonNull(error);
    }

    public boolean isError() {
        return error != null;
    }

    public static <T> @NotNull ErrorOr<T> ok(final @NotNull T value) {
        return new ErrorOr<>(value, null);
    }

    public static <T> @NotNull ErrorOr<T> error(final @NotNull String error, final @NotNull Object... args) {
        return new ErrorOr<>(null, String.format(error, args));
    }
}
