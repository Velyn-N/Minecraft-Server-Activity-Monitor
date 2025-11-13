package me.velyn.mcactivitymonitor.service;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import me.velyn.mcactivitymonitor.data.ServerRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
        LocalDateTime now = LocalDateTime.now();
        Set<ServerRecord> servers = dataStorageService.getServers();

        LocalDateTime checkDelay = now.minusMinutes(serverCheckDelay);
        List<ServerRecord> checkServers = servers.stream()
                .filter(sr -> checkDelay.isAfter(sr.lastFetchTime))
                .sorted(Comparator.comparing(sr -> sr.lastFetchTime))
                .toList();

        // The API rate limits at 5 requests per second per IP
        checkServers = checkServers.subList(0, Math.min(checkServers.size(), 5));

        for (ServerRecord sr : checkServers) {
            sr.lastFetchTime = now;
            dataStorageService.writeServerRecord(sr);
        }

        Log.infof("Checking %d servers", checkServers.size());
        dataStorageService.writeActivityRecords(dataProcessingService.checkServers(checkServers));
    }

    @Scheduled(cron = "30 0 0 * * ?")
    public void backupFiles() {
        Log.info("Backing up files");
        dataStorageService.copyFilesToBakFiles();
    }
}
