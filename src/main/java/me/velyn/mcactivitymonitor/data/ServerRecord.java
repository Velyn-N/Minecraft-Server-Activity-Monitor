package me.velyn.mcactivitymonitor.data;

import java.time.*;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ServerRecord", description = "A tracked server entry")
public class ServerRecord {
    @Schema(description = "Server hostname", example = "play.example.net")
    public String server;

    @Schema(description = "Last time data was fetched for this server", example = "2025-01-10T12:34:56")
    public LocalDateTime lastFetchTime;
}
