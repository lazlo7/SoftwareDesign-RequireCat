package net.requef.requirecat;

import org.jetbrains.annotations.NotNull;

public class Logger {
    private LogLevel logLevel = LogLevel.INFO;

    /**
     * Sets the log level.
     * All messages with a lower level will be ignored.
     *
     * @param logLevel The new log level.
     */
    public void setLogLevel(final @NotNull LogLevel logLevel) {
        this.logLevel = logLevel;
    }

    private boolean shouldLog(final @NotNull LogLevel logLevel) {
        return logLevel.ordinal() >= this.logLevel.ordinal();
    }

    private void printLog(final @NotNull LogLevel level, final @NotNull String message, final @NotNull Object... args) {
        System.out.printf("%s%s%n", level.getPrefix(), String.format(message, args));
    }

    /**
     * Logs a message (prints it to the stdout).
     * If the log level is too low, the message will be ignored.
     * @param level The log level.
     * @param message The message to log.
     * @param args The arguments for string formatting.
     */
    public void log(final @NotNull LogLevel level, final @NotNull String message, final @NotNull Object... args) {
        if (shouldLog(level)) {
            printLog(level, message, args);
        }
    }

    /**
     * Logs a message with the INFO log level.
     * @param message The message to log.
     * @param args The arguments for string formatting.
     */
    public void info(final @NotNull String message, final @NotNull Object... args) {
        log(LogLevel.INFO, message, args);
    }

    /**
     * Logs a message with the SUCCESS log level.
     * @param message The message to log.
     * @param args The arguments for string formatting.
     */
    public void success(final @NotNull String message, final @NotNull Object... args) {
        log(LogLevel.SUCCESS, message, args);
    }

    /**
     * Logs a message with the WARN log level.
     * @param message The message to log.
     * @param args The arguments for string formatting.
     */
    public void warn(final @NotNull String message, final @NotNull Object... args) {
        log(LogLevel.WARN, message, args);
    }

    /**
     * Logs a message with the ERROR log level.
     * @param message The message to log.
     * @param args The arguments for string formatting.
     */
    public void error(final @NotNull String message, final @NotNull Object... args) {
        log(LogLevel.ERROR, message, args);
    }
}
