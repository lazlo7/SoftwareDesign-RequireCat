package net.requef.requirecat;

import org.jetbrains.annotations.NotNull;

public class Logger {
    private LogLevel logLevel = LogLevel.INFO;

    public void setLogLevel(final @NotNull LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    private boolean shouldLog(final @NotNull LogLevel logLevel) {
        return logLevel.ordinal() >= this.logLevel.ordinal();
    }

    private void printLog(final @NotNull LogLevel level, final @NotNull String message, final @NotNull Object... args) {
        System.out.printf("%s%s%n", level.getPrefix(), String.format(message, args));
    }

    public void log(final @NotNull LogLevel level, final @NotNull String message, final @NotNull Object... args) {
        if (shouldLog(level)) {
            printLog(level, message, args);
        }
    }

    public void info(final @NotNull String message, final @NotNull Object... args) {
        log(LogLevel.INFO, message, args);
    }

    public void success(final @NotNull String message, final @NotNull Object... args) {
        log(LogLevel.SUCCESS, message, args);
    }

    public void warn(final @NotNull String message, final @NotNull Object... args) {
        log(LogLevel.WARN, message, args);
    }

    public void error(final @NotNull String message, final @NotNull Object... args) {
        log(LogLevel.ERROR, message, args);
    }
}
