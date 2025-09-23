package me.velyn.mcactivitymonitor.ui;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import me.velyn.mcactivitymonitor.data.ActivityRecord;
import me.velyn.mcactivitymonitor.data.ServerRecord;
import me.velyn.mcactivitymonitor.service.DataStorageService;
import me.velyn.mcactivitymonitor.service.DataStorageService.ActivityRecordFilter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

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


        long onlineCount = workingSet.stream().filter(ar -> ar.online).count();

        ActivityRecord acc = new ActivityRecord();
        acc.recordCreationTime = hour;
        acc.dataRetrievalTime = hour;
        acc.online = onlineCount > workingSet.size() / 2;
        acc.server = workingSet.getFirst().server;
        acc.playerCount = (int) Math.round(averagePlayers);
        
        return acc;
    }
    
    private LocalDateTime atStartOfHour(LocalDateTime ldt) {
        return LocalDateTime.of(ldt.toLocalDate(), LocalTime.of(ldt.getHour(), 0));
    }
}
