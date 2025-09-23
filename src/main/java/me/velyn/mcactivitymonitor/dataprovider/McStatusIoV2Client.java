package me.velyn.mcactivitymonitor.dataprovider;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * A REST Client for the mcstatus.io API
 */
@RegisterRestClient(baseUri = "https://api.mcstatus.io/v2")
public interface McStatusIoV2Client {

    @GET
    @Path("/status/java/{address}")
    JavaStatus getStatusJava(
            @PathParam("address") String address,
            @QueryParam("query") boolean query
    );

    class JavaStatus {
        public boolean online;
        public long retrieved_at;
        public Players players;

        public static class Players {
            public int online;
        }
    }
}
