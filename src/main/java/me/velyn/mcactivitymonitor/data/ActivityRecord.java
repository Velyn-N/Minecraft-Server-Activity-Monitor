package me.velyn.mcactivitymonitor.data;

import java.time.*;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ActivityRecord", description = "A single observation of a server's status at a point in time")
public class ActivityRecord {
    @Schema(description = "Time when this record was created", example = "2025-01-10T12:34:56")
    public LocalDateTime recordCreationTime;

    @Schema(description = "Time when data was fetched from the upstream provider", example = "2025-01-10T12:34:55")
    public LocalDateTime dataRetrievalTime;

    @Schema(description = "Whether the server was reachable at that time", example = "true")
    public boolean online;

    @Schema(description = "Server hostname", example = "play.example.net")
    public String server;

    @Schema(description = "Number of players online at that time", example = "12")
    public int playerCount;
}
