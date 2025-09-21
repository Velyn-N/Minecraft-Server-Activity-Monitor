package me.velyn.mcactivitymonitor.ui;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import jakarta.enterprise.context.*;
import jakarta.inject.*;
import me.velyn.mcactivitymonitor.data.*;
import me.velyn.mcactivitymonitor.service.*;
import me.velyn.mcactivitymonitor.service.DataStorageService.*;

@Named("quteService")
@ApplicationScoped
public class QuteService {
    public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Inject
    DataStorageService dataStorageService;

    public String formatDateTime(LocalDateTime date) {
        return date.format(DATE_TIME_FORMATTER);
    }

    public String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    public Set<ServerRecord> getServers() {
        return dataStorageService.getServers();
    }

    public List<ActivityRecord> getActivities(String server, int lastXDays) {
        ActivityRecordFilter filter = new ActivityRecordFilter(null, null, server);
        if (lastXDays >= 0) {
            filter = new ActivityRecordFilter(LocalDate.now().minusDays(lastXDays).atStartOfDay(), null, server);
        }
        return dataStorageService.getActivityRecords(filter);
    }

    public ActivityRecord getLastRecordedActivity(String server) {
        return dataStorageService.getLastActivityRecord(server);
    }

    public Map<LocalDate, List<ActivityRecord>> groupByDate(List<ActivityRecord> records) {
        return records.stream()
                .collect(Collectors.groupingBy(
                        ar -> ar.recordCreationTime.toLocalDate(),
                        LinkedHashMap::new,
                        Collectors.toList()));
    }

    public List<ActivityRecord> accumulateByHour(List<ActivityRecord> records) {
        List<ActivityRecord> accumulates = new ArrayList<>();

        LocalDateTime currHour = null;
        List<ActivityRecord> workingSet = new ArrayList<>();

        for (var record : records) {
            LocalDateTime recordHour = atStartOfHour(record.dataRetrievalTime);

            if (currHour == null) {
                currHour = recordHour;
            }

            if (recordHour.isEqual(currHour)) {
                workingSet.add(record);
            } else {
                accumulates.add(processHourGroup(workingSet, currHour));

                currHour = recordHour;
                workingSet = new ArrayList<>();
                workingSet.add(record);
            }
        }

        accumulates.add(processHourGroup(workingSet, currHour));

        return accumulates;
    }

    private ActivityRecord processHourGroup(List<ActivityRecord> workingSet, LocalDateTime hour) {
        if (workingSet.isEmpty()) return null;
        
        double averagePlayers = workingSet.stream()
                .mapToInt(ar -> ar.playerCount)
                .average().orElse(0.0);
        
        boolean wasOnline = workingSet.stream().allMatch(ar -> ar.online);
        
        ActivityRecord acc = new ActivityRecord();
        acc.recordCreationTime = hour;
        acc.dataRetrievalTime = hour;
        acc.online = wasOnline;
        acc.server = workingSet.getFirst().server;
        acc.playerCount = (int) Math.round(averagePlayers);
        
        return acc;
    }
    
    private LocalDateTime atStartOfHour(LocalDateTime ldt) {
        return LocalDateTime.of(ldt.toLocalDate(), LocalTime.of(ldt.getHour(), 0));
    }
}
