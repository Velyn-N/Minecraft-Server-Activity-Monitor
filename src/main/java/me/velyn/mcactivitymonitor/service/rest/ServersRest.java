package me.velyn.mcactivitymonitor.service.rest;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import me.velyn.mcactivitymonitor.data.ServerRecord;
import me.velyn.mcactivitymonitor.service.DataStorageService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.Set;

@Path("/rest/servers")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Servers", description = "Manage the list of tracked Minecraft servers")
public class ServersRest {

    @Inject
    DataStorageService dataStorageService;

    @GET
    @Operation(summary = "List servers", description = "Returns the set of tracked servers.")
    @APIResponse(responseCode = "200", description = "Set of servers",
            content = @Content(schema = @Schema(implementation = ServerRecord[].class)))
    public Response getServers() {
        Set<ServerRecord> servers = dataStorageService.getServers();
        return Response.ok(servers).build();
    }
}
