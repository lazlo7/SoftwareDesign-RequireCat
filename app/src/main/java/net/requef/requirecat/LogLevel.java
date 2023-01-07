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

    public @NotNull String getPrefix() {
        return prefix;
    }
}
