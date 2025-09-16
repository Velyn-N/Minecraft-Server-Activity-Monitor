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

    @Inject
    DataStorageService dataStorageService;

    public String formatDateTime(LocalDateTime date) {
        return date.format(DATE_TIME_FORMATTER);
    }

    public Set<ServerRecord> getServers() {
        return dataStorageService.getServers();
    }

    public List<ActivityRecord> getActivities(String server) {
        return dataStorageService.getActivityRecords(new DataStorageService.ActivityRecordFilter(null, null, server));
    }

    public List<ActivityRecord> getRecentActivities(String server, int maxPoints) {
        List<ActivityRecord> list = getActivities(server)
                .stream()
                .sorted(Comparator.comparing(ar -> ar.recordCreationTime))
                .collect(Collectors.toList());
        if (maxPoints > 0 && list.size() > maxPoints) {
            return list.subList(list.size() - maxPoints, list.size());
        }
        return list;
    }

    public int getMaxPlayerCount(String server, int maxPoints) {
        return getRecentActivities(server, maxPoints)
                .stream()
                .filter(ar -> ar != null && ar.online)
                .mapToInt(ar -> ar.playerCount)
                .max()
                .orElse(0);
    }

    public static class ActivitySegment {
        public double start;
        public double end;
        public String startLabel;
        public String endLabel;
        public int startValue;
        public int endValue;

        public ActivitySegment() {}
    }

    public List<ActivitySegment> getRecentActivitySegments(String server, int maxPoints) {
        List<ActivityRecord> recs = getRecentActivities(server, maxPoints);
        int max = getMaxPlayerCount(server, maxPoints);
        if (max <= 0 || recs.size() < 2) {
            return Collections.emptyList();
        }
        List<ActivitySegment> segments = new ArrayList<>();
        ActivityRecord prev = recs.get(0);
        for (int i = 1; i < recs.size(); i++) {
            ActivityRecord curr = recs.get(i);
            double s = (prev != null && prev.online) ? ((double) prev.playerCount / (double) max) : 0d;
            double e = (curr != null && curr.online) ? ((double) curr.playerCount / (double) max) : 0d;
            ActivitySegment seg = new ActivitySegment();
            seg.start = s;
            seg.end = e;
            seg.startLabel = prev != null && prev.recordCreationTime != null ? formatDateTime(prev.recordCreationTime) : "n/a";
            seg.endLabel = curr != null && curr.recordCreationTime != null ? formatDateTime(curr.recordCreationTime) : "n/a";
            seg.startValue = (prev != null && prev.online) ? prev.playerCount : 0;
            seg.endValue = (curr != null && curr.online) ? curr.playerCount : 0;
            segments.add(seg);
            prev = curr;
        }
        return segments;
    }
}
