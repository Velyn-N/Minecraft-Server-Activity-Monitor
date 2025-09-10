package me.velyn.mcactivitymonitor.service;

import java.time.*;
import java.util.*;

import org.eclipse.microprofile.config.inject.*;

import io.quarkus.logging.*;
import io.quarkus.scheduler.*;
import jakarta.enterprise.context.*;
import jakarta.inject.*;
import me.velyn.mcactivitymonitor.data.*;

@ApplicationScoped
public class SchedulerService {
    public static final String SCHEDULER_NAME = "SERVER_CHECK_SCHEDULER";

    @Inject
    DataStorageService dataStorageService;

    @Inject
    DataProcessingService dataProcessingService;

    @ConfigProperty(name = "scheduler.server.check.delay", defaultValue = "5")
    int serverCheckDelay;

    @Scheduled(identity = SCHEDULER_NAME,
            cron = "${scheduler.server.check.cron}",
            concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    public void scheduledServerCheck() {
        Set<ServerRecord> servers = dataStorageService.getServers();

        LocalDateTime checkDelay = LocalDateTime.now().minusMinutes(serverCheckDelay);
        List<ServerRecord> checkServers = servers.stream()
                .filter(sr -> checkDelay.isAfter(sr.lastFetchTime))
                .sorted(Comparator.comparing(sr -> sr.lastFetchTime))
                .toList();

        // The API rate limits at 5 requests per second per IP
        checkServers = checkServers.subList(0, Math.min(checkServers.size(), 5));

        Log.infof("Checking %d servers", checkServers.size());
        dataStorageService.writeActivityRecords(dataProcessingService.checkServers(checkServers));
    }
}
