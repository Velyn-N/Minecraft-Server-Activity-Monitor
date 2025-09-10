package me.velyn.mcactivitymonitor.service;

import java.time.*;
import java.util.*;

import org.eclipse.microprofile.rest.client.inject.*;

import io.quarkus.logging.*;
import jakarta.enterprise.context.*;
import me.velyn.mcactivitymonitor.data.*;
import me.velyn.mcactivitymonitor.dataprovider.*;

@ApplicationScoped
public class DataProcessingService {

    @RestClient
    McStatusIoV2Client mcStatusIoV2Client;

    public List<ActivityRecord> checkServers(List<ServerRecord> servers) {
        return servers.stream()
                      .map(sr -> checkServer(sr.server))
                      .filter(Optional::isPresent)
                      .map(Optional::get)
                      .toList();
    }

    public Optional<ActivityRecord> checkServer(String server) {
        Log.infof("Checking server '%s'", server);
        McStatusIoV2Client.JavaStatus status;
        try {
            status = mcStatusIoV2Client.getStatusJava(server, false);
        } catch (Exception e) {
            Log.errorf("Error checking server '%s'", server, e);
            return Optional.empty();
        }

        ActivityRecord rec = new ActivityRecord();
        rec.recordCreationTime = LocalDateTime.now();
        rec.dataRetrievalTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(status.retrieved_at), ZoneId.systemDefault());
        rec.online = status.online;
        rec.server = server;
        if (status.online) {
            rec.playerCount = status.players.online;
            Log.infof("Server '%s' is online with %d players", server, rec.playerCount);
        } else {
            Log.infof("Server '%s' is offline", server);
        }
        return Optional.of(rec);
    }
}
