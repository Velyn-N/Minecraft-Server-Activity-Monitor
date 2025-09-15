package me.velyn.mcactivitymonitor.service.rest;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import io.quarkus.logging.*;
import jakarta.inject.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import me.velyn.mcactivitymonitor.data.*;
import me.velyn.mcactivitymonitor.service.*;
import me.velyn.mcactivitymonitor.service.DataStorageService.*;

@Path("/rest/activities")
public class ActivitiesRest {

    @Inject
    DataStorageService dataStorageService;

    @GET
    public Response getActivities(
            @QueryParam("server") String server,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
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
