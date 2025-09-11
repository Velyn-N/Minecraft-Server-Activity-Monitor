package me.velyn.mcactivitymonitor.service.rest;

import java.time.*;
import java.util.*;

import jakarta.inject.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import me.velyn.mcactivitymonitor.data.*;
import me.velyn.mcactivitymonitor.service.*;

@Path("/rest/server")
@Produces(MediaType.APPLICATION_JSON)
public class ServerRest {

    @Inject
    DataStorageService dataStorageService;

    @POST
    public Response addServer(String server) {
        dataStorageService.addServer(server);
        return Response.ok().build();
    }
    
    @DELETE
    @Path("/{server}")
    public Response deleteServer(@PathParam("server") String server) {
        boolean removed = dataStorageService.deleteServer(server);
        if (removed) return Response.noContent().build();
        return Response.status(Response.Status.NOT_FOUND).build();
    }

    @GET
    @Path("/{server}/activities")
    public Response getActivities(@PathParam("server") String server) {
        boolean hasServerFilter = server != null && !server.trim().isBlank();

        if (!hasServerFilter) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        return Response.ok(dataStorageService.getActivityRecordsForServer(server)).build();
    }
}
