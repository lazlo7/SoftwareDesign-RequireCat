package net.requef.requirecat;

import org.jetbrains.annotations.NotNull;

public enum LogLevel {
    INFO("info", ConsoleColors.RESET),
    SUCCESS("success", ConsoleColors.GREEN),
    WARN("warning", ConsoleColors.YELLOW_BRIGHT),
    ERROR("error", ConsoleColors.RED);

    private final String prefix;

    LogLevel(final @NotNull String name, final @NotNull String color) {
        this.prefix = String.format("%s[%s]%s ", color, name, ConsoleColors.RESET);
    }

    /**
     * Gets the prefix of this log level.
     * A prefix is a string that should be printed before the actual log message.
     * It uses an ANSI color code to color the log level name.
     * @return The prefix of this log level.
     */
    public @NotNull String getPrefix() {
        return prefix;
    }
}
