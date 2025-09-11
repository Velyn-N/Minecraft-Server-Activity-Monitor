package me.velyn.mcactivitymonitor.service.rest;

import jakarta.inject.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import me.velyn.mcactivitymonitor.service.*;

@Path("/rest/servers")
@Produces(MediaType.APPLICATION_JSON)
public class ServersRest {

    @Inject
    DataStorageService dataStorageService;

    @GET
    public Response getServers() {
        return Response.ok(dataStorageService.getServers()).build();
    }
}
