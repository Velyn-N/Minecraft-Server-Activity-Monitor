package me.velyn.mcactivitymonitor.service.rest;

import java.time.*;
import java.util.*;

import jakarta.inject.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import me.velyn.mcactivitymonitor.data.*;
import me.velyn.mcactivitymonitor.service.*;

@Path("/rest/server")
public class ServerRest {

    @Inject
    DataStorageService dataStorageService;

    @GET
    public Response getServers() {
        return Response.ok(dataStorageService.getServers()).build();
    }

    @POST
    public Response addServer(String server) {
        ServerRecord record = new ServerRecord();
        record.server = server;
        record.lastFetchTime = LocalDate.of(1970, 1, 1).atStartOfDay();
        dataStorageService.writeServerRecord(record);
        return Response.ok().build();
    }
    
    @DELETE
    @Path("/{server}")
    public Response deleteServer(@PathParam("server") String server) {
        boolean removed = dataStorageService.deleteServer(server);
        if (removed) return Response.noContent().build();
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
