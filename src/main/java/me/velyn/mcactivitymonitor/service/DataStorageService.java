package me.velyn.mcactivitymonitor.service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.commons.csv.*;
import org.eclipse.microprofile.config.inject.*;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.*;
import me.velyn.mcactivitymonitor.data.*;

@ApplicationScoped
public class DataStorageService {

    @ConfigProperty(name = "storage.file.servers")
    String serversFilePath;

    @ConfigProperty(name = "storage.file.activity.records")
    String activityRecordsFilePath;

    private static final char SEP = ';';
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private volatile Set<ServerRecord> serversCache = null;
    private volatile long serversCacheMtime = -1;

    public void writeActivityRecords(List<ActivityRecord> records) {
        if (records == null || records.isEmpty()) return;
        Path path = Paths.get(activityRecordsFilePath);
        try {
            Files.createDirectories(path.getParent());
        } catch (Exception ignored) {}

        boolean exists = Files.exists(path);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("recordCreationTime", "dataRetrievalTime", "online", "server", "playerCount")
                    .setDelimiter(SEP)
                    .setSkipHeaderRecord(exists)
                    .get();
            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (ActivityRecord r : records) {
                    printer.printRecord(
                            r.recordCreationTime != null ? DATE_FMT.format(r.recordCreationTime) : "",
                            r.dataRetrievalTime != null ? DATE_FMT.format(r.dataRetrievalTime) : "",
                            r.online,
                            r.server != null ? r.server : "",
                            r.playerCount
                    );
                }
            }
        } catch (IOException e) {
            Log.error("Failed to write activity records", e);
        }
    }

    public List<ActivityRecord> getActivityRecordsForServer(String server) {
        List<ActivityRecord> result = new ArrayList<>();
        Path path = Paths.get(activityRecordsFilePath);
        if (!Files.exists(path)) return result;
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("recordCreationTime", "dataRetrievalTime", "online", "server", "playerCount")
                .setSkipHeaderRecord(true)
                .setDelimiter(SEP)
                .get();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, format)) {
            for (CSVRecord rec : parser) {
                String srv = rec.get("server");
                if (!Objects.equals(srv, server)) continue;
                ActivityRecord ar = new ActivityRecord();
                String rct = rec.get("recordCreationTime");
                String drt = rec.get("dataRetrievalTime");
                String onl = rec.get("online");
                String plc = rec.get("playerCount");
                ar.recordCreationTime = rct == null || rct.isEmpty() ? null : LocalDateTime.parse(rct, DATE_FMT);
                ar.dataRetrievalTime = drt == null || drt.isEmpty() ? null : LocalDateTime.parse(drt, DATE_FMT);
                ar.online = onl != null && (onl.equalsIgnoreCase("true") || onl.equals("1"));
                ar.server = srv;
                try {
                    ar.playerCount = plc == null || plc.isEmpty() ? 0 : Integer.parseInt(plc);
                } catch (NumberFormatException ex) {
                    ar.playerCount = 0;
                }
                result.add(ar);
            }
        } catch (IOException e) {
            Log.error("Failed to read activity records", e);
        }
        return result;
    }

    public synchronized Set<ServerRecord> getServers() {
        Path path = Paths.get(serversFilePath);
        long mtime = -1;
        if (Files.exists(path)) {
            try { mtime = Files.getLastModifiedTime(path).toMillis(); } catch (IOException ignored) {}
        }
        if (serversCache != null && mtime == serversCacheMtime) {
            return serversCache;
        }
        Set<ServerRecord> set = new HashSet<>();
        if (!Files.exists(path)) {
            serversCache = set;
            serversCacheMtime = mtime;
            return set;
        }
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader("server", "lastFetchTime")
                .setSkipHeaderRecord(true)
                .setDelimiter(SEP)
                .get();
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, format)) {
            for (CSVRecord rec : parser) {
                ServerRecord sr = new ServerRecord();
                sr.server = rec.get("server");
                String lft = rec.get("lastFetchTime");
                sr.lastFetchTime = (lft == null || lft.isEmpty()) ? LocalDateTime.MIN : LocalDateTime.parse(lft, DATE_FMT);
                set.add(sr);
            }
        } catch (IOException e) {
            Log.error("Failed to read servers", e);
        }
        serversCache = set;
        serversCacheMtime = mtime;
        return set;
    }

    public synchronized void writeServerRecord(ServerRecord record) {
        Set<ServerRecord> all = new HashSet<>(getServers());
        all.removeIf(sr -> Objects.equals(sr.server, record.server));
        all.add(record);
        Path path = Paths.get(serversFilePath);
        try {
            Files.createDirectories(path.getParent());
        } catch (Exception ignored) {}
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("server", "lastFetchTime")
                    .setDelimiter(SEP)
                    .get();
            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (ServerRecord sr : all) {
                    printer.printRecord(
                            sr.server != null ? sr.server : "",
                            sr.lastFetchTime != null ? DATE_FMT.format(sr.lastFetchTime) : ""
                    );
                }
            }
            try { serversCacheMtime = Files.getLastModifiedTime(path).toMillis(); } catch (IOException ignored) {}
            serversCache = all;
        } catch (IOException e) {
            Log.error("Failed to write servers", e);
        }
    }
    
    public synchronized boolean deleteServer(String server) {
        if (server == null) return false;
        Set<ServerRecord> all = new HashSet<>(getServers());
        boolean removed = all.removeIf(sr -> Objects.equals(sr.server, server));
        if (!removed) return false;
        Path path = Paths.get(serversFilePath);
        try {
            Files.createDirectories(path.getParent());
        } catch (Exception ignored) {}
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            CSVFormat format = CSVFormat.DEFAULT.builder()
                    .setHeader("server", "lastFetchTime")
                    .setDelimiter(SEP)
                    .get();
            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (ServerRecord sr : all) {
                    printer.printRecord(
                            sr.server != null ? sr.server : "",
                            sr.lastFetchTime != null ? DATE_FMT.format(sr.lastFetchTime) : ""
                    );
                }
            }
            try { serversCacheMtime = Files.getLastModifiedTime(path).toMillis(); } catch (IOException ignored) {}
            serversCache = all;
        } catch (IOException e) {
            Log.error("Failed to write servers", e);
        }
        return true;
    }
}
