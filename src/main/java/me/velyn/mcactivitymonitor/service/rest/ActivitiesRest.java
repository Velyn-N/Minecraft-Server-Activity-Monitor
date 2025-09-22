package me.velyn.mcactivitymonitor.service.rest;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import io.quarkus.logging.*;
import jakarta.inject.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import me.velyn.mcactivitymonitor.data.*;
import me.velyn.mcactivitymonitor.service.*;
import me.velyn.mcactivitymonitor.service.DataStorageService.*;

@Path("/rest/activities")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Activities", description = "Retrieve activity history for tracked Minecraft servers")
public class ActivitiesRest {

    @Inject
    DataStorageService dataStorageService;

    @GET
    @Operation(summary = "Get activity records",
            description = "Returns activity records grouped by server. Optionally filter by server and time range, and limit the number of records per server.")
    @APIResponse(responseCode = "200", description = "Activity records grouped by server",
            content = @Content(schema = @Schema(
                    type = SchemaType.OBJECT,
                    description = "Map of server name to list of activity records",
                    implementation = Map.class,
                    additionalProperties = ActivityRecord[].class
            )))
    @APIResponse(responseCode = "400", description = "Invalid query parameter value")
    public Response getActivities(
            @Parameter(description = "Only return data for this exact server name", example = "play.example.net")
            @QueryParam("server") String server,
            @Parameter(description = "Start of time range (inclusive) in ISO_LOCAL_DATE_TIME format",
                    example = "2025-01-01T00:00:00")
            @QueryParam("from") String from,
            @Parameter(description = "End of time range (inclusive) in ISO_LOCAL_DATE_TIME format",
                    example = "2025-01-31T23:59:59")
            @QueryParam("to") String to,
            @Parameter(description = "Max number of records per server to return (0 means no limit)", example = "30")
            @QueryParam("maxDataPoints") int maxDataPoints) {

        Log.debug("Starting getActivities Endpoint");
        LocalDateTime fromDateTime = null;
        LocalDateTime toDateTime = null;

        if (from != null && !from.trim().isEmpty()) {
            try {
                fromDateTime = LocalDateTime.parse(from, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid 'from' date format. Expected ISO format: yyyy-MM-ddTHH:mm:ss")
                        .build();
            }
        }

        if (to != null && !to.trim().isEmpty()) {
            try {
                toDateTime = LocalDateTime.parse(to, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } catch (DateTimeParseException e) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Invalid 'to' date format. Expected ISO format: yyyy-MM-ddTHH:mm:ss")
                        .build();
            }
        }

        if (maxDataPoints < 0) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("maxDataPoints must be non-negative")
                    .build();
        }

        String serverFilter = (server != null && !server.trim().isEmpty()) ? server.trim() : null;

        ActivityRecordFilter filter = new ActivityRecordFilter(fromDateTime, toDateTime, serverFilter);

        Map<String, List<ActivityRecord>> records = dataStorageService.getActivityRecords(filter)
                .stream()
                .sorted(Comparator.comparing(ar -> ar.recordCreationTime))
                .collect(Collectors.groupingBy(ar -> ar.server));

        Log.debugf("Found %d activity records", records.size());

        if (maxDataPoints > 0) {
            Map<String, List<ActivityRecord>> truncatedRecords = new HashMap<>();
            for (var entry : records.entrySet()) {
                if (entry.getValue().size() <= maxDataPoints) {
                    truncatedRecords.put(entry.getKey(), entry.getValue());
                    continue;
                }
                truncatedRecords.put(entry.getKey(), entry.getValue().subList(0, maxDataPoints));
            }
            records = truncatedRecords;
        }

        Log.debug("End getActivities Endpoint");
        return Response.ok(records).build();
    }
}
