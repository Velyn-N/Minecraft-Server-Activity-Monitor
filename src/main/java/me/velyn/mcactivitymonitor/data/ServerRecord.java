package me.velyn.mcactivitymonitor.data;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(name = "ServerRecord", description = "A tracked server entry")
public class ServerRecord {
    @Schema(description = "Server hostname", examples = "play.example.net")
    public String server;

    @Schema(description = "Last time data was fetched for this server", examples = "2025-01-10T12:34:56")
    public LocalDateTime lastFetchTime;
}
