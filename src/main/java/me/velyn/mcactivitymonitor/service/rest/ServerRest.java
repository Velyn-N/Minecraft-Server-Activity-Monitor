package me.velyn.mcactivitymonitor.service.rest;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.inject.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import me.velyn.mcactivitymonitor.service.*;

@Path("/rest/server")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.TEXT_PLAIN)
@Tag(name = "Server", description = "Add or remove a tracked server")
public class ServerRest {

    @Inject
    DataStorageService dataStorageService;

    @POST
    @Operation(summary = "Add a server", description = "Adds a server (hostname) to the tracked set.")
    @RequestBody(required = true,
            content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(example = "play.example.net")))
    @APIResponse(responseCode = "200", description = "Server added")
    public Response addServer(String server) {
        dataStorageService.addServer(server);
        return Response.ok().build();
    }
    
    @DELETE
    @Operation(summary = "Delete a server", description = "Removes the server (hostname) from the tracked set.")
    @RequestBody(required = true,
            content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(example = "play.example.net")))
    @APIResponse(responseCode = "204", description = "Server deleted")
    @APIResponse(responseCode = "404", description = "Server not found")
    public Response deleteServer(String server) {
        boolean removed = dataStorageService.deleteServer(server);
        if (removed) return Response.noContent().build();
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
