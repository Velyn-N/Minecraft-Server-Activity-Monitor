package me.velyn.mcactivitymonitor.ui;

import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.stream.*;

import jakarta.enterprise.context.*;
import jakarta.inject.*;
import me.velyn.mcactivitymonitor.data.*;
import me.velyn.mcactivitymonitor.service.*;

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

    public List<ActivityRecord> getActivities(String server) {
        return dataStorageService.getActivityRecords(new DataStorageService.ActivityRecordFilter(null, null, server));
    }

    public Map<LocalDate, List<ActivityRecord>> groupByDate(List<ActivityRecord> records) {
        return records.stream()
                      .collect(Collectors.groupingBy(
                              ar -> ar.recordCreationTime.toLocalDate(),
                              LinkedHashMap::new,
                              Collectors.toList()));
    }
}
