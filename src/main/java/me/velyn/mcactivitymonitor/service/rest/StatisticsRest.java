package me.velyn.mcactivitymonitor.service.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.velyn.mcactivitymonitor.service.DataStorageService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/rest/statistics")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Statistics", description = "Retrieve a short statistical overview of the application's current state. Useful for dashboards.")
public class StatisticsRest {

    @Inject
    DataStorageService dataStorageService;

    @GET
    @Operation(summary = "Get application statistics",
            description = "Returns a compact overview including the number of tracked servers and the total number of recorded activity entries.")
    @APIResponse(responseCode = "200", description = "Statistics payload",
            content = @Content(schema = @Schema(implementation = Statistics.class)))
    public Response getStatistics() {
        return Response.ok(new Statistics(
                dataStorageService.getServers().size(),
                dataStorageService.getActivityRecordsCount()
        )).build();
    }

    @Schema(name = "Statistics", description = "High-level application counters")
    public record Statistics(
            @Schema(description = "Number of tracked servers", examples = "3")
            int serverCount,
            @Schema(description = "Total number of recorded activity entries", examples = "12456")
            long recordedActivitiesCount
    ) {}
}
