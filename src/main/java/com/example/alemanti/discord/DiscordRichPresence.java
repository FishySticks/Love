package com.example.alemanti.discord;

/**
 * Discord Rich Presence integration point.
 *
 * To make Discord show Alemanti Client as a registered application on Linux,
 * create a Discord Developer Portal application and use its Application ID
 * with a supported Discord RPC library. This class deliberately keeps the
 * base project dependency-free until an Application ID/library is supplied.
 */
public final class DiscordRichPresence {
    public static final String DEVELOPER = "svbx";
    public static final String CLIENT_NAME = "Alemanti Client";

    private DiscordRichPresence() {}

    public static void start(String applicationId) {
        if (applicationId == null || applicationId.isBlank()) {
            throw new IllegalArgumentException("A registered Discord Application ID is required.");
        }
        // RPC library hookup goes here.
    }

    public static void update(String details, String state) {
        // RPC library update goes here.
    }

    public static void shutdown() {
        // RPC library shutdown goes here.
    }
}
