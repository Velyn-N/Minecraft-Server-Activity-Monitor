package me.velyn.mcactivitymonitor.service;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import me.velyn.mcactivitymonitor.data.ActivityRecord;
import me.velyn.mcactivitymonitor.data.ServerRecord;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@ApplicationScoped
public class DataStorageService {
    private static final DateTimeFormatter ISO_DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final char SEPARATOR = ';';

    @ConfigProperty(name = "storage.file.servers")
    String serversFilePath;

    @ConfigProperty(name = "storage.file.activity.records")
    String activityRecordsFilePath;

    private volatile Set<ServerRecord> serversCache = null;
    private volatile long serversCacheMtime = -1;

    // ---------------------
    // Activity records
    // ---------------------

    public long getActivityRecordsCount() {
        Path path = Paths.get(activityRecordsFilePath);
        if (!Files.exists(path)) return 0;
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return lines.count();
        } catch (IOException e) {
            Log.error("Failed to read activity records", e);
            return 0;
        }
    }

    public long getDistinctDaysCount() {
        Path path = Paths.get(activityRecordsFilePath);
        if (!Files.exists(path)) return 0;
        try (Stream<String> lines = Files.lines(path, StandardCharsets.UTF_8)) {
            return lines.map(ln -> parseActivityRecordLine(ln, null))
                    .filter(Objects::nonNull)
                    .map(ar -> ar.dataRetrievalTime.toLocalDate())
                    .distinct()
                    .count();
        } catch (IOException e) {
            Log.error("Failed to read activity records", e);
            return 0;
        }
    }

    public void writeActivityRecords(List<ActivityRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Path path = Paths.get(activityRecordsFilePath);
        ensureParentDirExists(path);

        boolean exists = Files.exists(path);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            CSVFormat format = csvFormatActivity(exists);
            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (ActivityRecord r : records) {
                    printer.printRecord(
                            r.recordCreationTime != null ? ISO_DATE_TIME_FORMAT.format(r.recordCreationTime) : "",
                            r.dataRetrievalTime != null ? ISO_DATE_TIME_FORMAT.format(r.dataRetrievalTime) : "",
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

    public ActivityRecord getLastActivityRecord(String server) {
        if (server == null) return null;

        Path path = Paths.get(activityRecordsFilePath);
        if (!Files.exists(path)) return null;

        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
            long fileLength = file.length();
            if (fileLength == 0) return null;

            StringBuilder lineBuilder = new StringBuilder();
            long position = fileLength - 1;

            // Read file backwards line by line
            while (position >= 0) {
                file.seek(position);
                char ch = (char) file.readByte();

                if (ch == '\n' || ch == '\r') {
                    if (!lineBuilder.isEmpty()) {
                        String line = lineBuilder.reverse().toString();
                        ActivityRecord record = parseActivityRecordLine(line, server);
                        if (record != null) {
                            return record;
                        }
                        lineBuilder.setLength(0);
                    }
                } else {
                    lineBuilder.append(ch);
                }
                position--;
            }

            // Handle the first line (no newline at the beginning)
            if (!lineBuilder.isEmpty()) {
                String line = lineBuilder.reverse().toString();
                ActivityRecord record = parseActivityRecordLine(line, server);
                if (record != null) {
                    return record;
                }
            }

        } catch (IOException e) {
            Log.error("Failed to read last activity record for server: " + server, e);
        }

        return null;
    }

    private ActivityRecord parseActivityRecordLine(String line, String targetServer) {
        if (line == null || line.trim().isEmpty()) return null;

        if (line.startsWith("recordCreationTime")) return null;

        try {
            String[] parts = line.split(String.valueOf(SEPARATOR));
            if (parts.length != 5) return null;

            String srv = parts[3]; // server is at index 3

            if (targetServer != null && !Objects.equals(srv, targetServer)) {
                return null;
            }

            return buildActivityRecord(parts[0], parts[1], parts[2], srv, parts[4]);
        } catch (Exception e) {
            Log.debug("Failed to parse activity record line: " + line, e);
            return null;
        }
    }

    public record ActivityRecordFilter(LocalDateTime from, LocalDateTime to, String server) {}

    public List<ActivityRecord> getActivityRecords(ActivityRecordFilter filter) {
        boolean hasFilterServer = filter != null && filter.server() != null;
        boolean hasFilterFrom = filter != null && filter.from() != null;
        boolean hasFilterTo = filter != null && filter.to() != null;

        List<ActivityRecord> result = new ArrayList<>();
        Path path = Paths.get(activityRecordsFilePath);
        if (!Files.exists(path)) return result;
        CSVFormat format = csvFormatActivity(true);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, format)) {
            for (CSVRecord rec : parser) {
                String srv = rec.get("server");

                if (hasFilterServer && !Objects.equals(srv, filter.server())) {
                    continue;
                }

                ActivityRecord ar = buildActivityRecord(
                        rec.get("recordCreationTime"),
                        rec.get("dataRetrievalTime"),
                        rec.get("online"),
                        srv,
                        rec.get("playerCount")
                );

                if (hasFilterFrom && ar.recordCreationTime != null && ar.recordCreationTime.isBefore(filter.from())) {
                    continue;
                }
                if (hasFilterTo && ar.recordCreationTime != null && ar.recordCreationTime.isAfter(filter.to())) {
                    continue;
                }

                result.add(ar);
            }
        } catch (IOException e) {
            Log.error("Failed to read activity records", e);
        }
        result.sort(Comparator.comparing(ar -> ar.recordCreationTime));
        return result;
    }

    // ---------------------
    // Servers
    // ---------------------

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
        CSVFormat format = csvFormatServers(true);
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVParser.parse(reader, format)) {
            for (CSVRecord rec : parser) {
                ServerRecord sr = new ServerRecord();
                sr.server = rec.get("server");
                String lft = rec.get("lastFetchTime");
                sr.lastFetchTime = (lft == null || lft.isEmpty()) ? LocalDateTime.MIN : LocalDateTime.parse(lft, ISO_DATE_TIME_FORMAT);
                set.add(sr);
            }
        } catch (IOException e) {
            Log.error("Failed to read servers", e);
        }
        serversCache = set;
        serversCacheMtime = mtime;
        return set;
    }

    public synchronized void addServer(String server) {
        ServerRecord record = new ServerRecord();
        record.server = server;
        record.lastFetchTime = LocalDateTime.MIN;
        writeServerRecord(record);
    }

    private void writeServers(Set<ServerRecord> all) {
        Path path = Paths.get(serversFilePath);
        ensureParentDirExists(path);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            CSVFormat format = csvFormatServers(false);
            try (CSVPrinter printer = new CSVPrinter(writer, format)) {
                for (ServerRecord sr : all) {
                    printer.printRecord(
                            sr.server != null ? sr.server : "",
                            sr.lastFetchTime != null ? ISO_DATE_TIME_FORMAT.format(sr.lastFetchTime) : ""
                    );
                }
            }
            try { serversCacheMtime = Files.getLastModifiedTime(path).toMillis(); } catch (IOException ignored) {}
            serversCache = all;
        } catch (IOException e) {
            Log.error("Failed to write servers", e);
        }
    }

    public synchronized void writeServerRecord(ServerRecord record) {
        Set<ServerRecord> all = new HashSet<>(getServers());
        all.removeIf(sr -> Objects.equals(sr.server, record.server));
        all.add(record);
        writeServers(all);
    }

    public synchronized boolean deleteServer(String server) {
        if (server == null) return false;
        Set<ServerRecord> all = new HashSet<>(getServers());
        boolean removed = all.removeIf(sr -> Objects.equals(sr.server, server));
        if (!removed) return false;
        writeServers(all);
        return true;
    }

    // ---------------------
    // Helpers (parsing, CSV, IO)
    // ---------------------

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        return "1".equals(value) || Boolean.parseBoolean(value);
    }

    private static LocalDateTime parseLocalDateTime(String value) {
        if (value == null || value.isEmpty()) return null;
        return LocalDateTime.parse(value, ISO_DATE_TIME_FORMAT);
    }

    private static int parseIntSafe(String value) {
        if (value == null || value.isEmpty()) return 0;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static CSVFormat csvFormatActivity(boolean skipHeader) {
        return CSVFormat.DEFAULT.builder()
                .setHeader("recordCreationTime", "dataRetrievalTime", "online", "server", "playerCount")
                .setSkipHeaderRecord(skipHeader)
                .setDelimiter(SEPARATOR)
                .get();
    }

    private static CSVFormat csvFormatServers(boolean skipHeader) {
        return CSVFormat.DEFAULT.builder()
                .setHeader("server", "lastFetchTime")
                .setSkipHeaderRecord(skipHeader)
                .setDelimiter(SEPARATOR)
                .get();
    }

    private static void ensureParentDirExists(Path path) {
        try {
            Path parent = path.getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception ignored) {}
    }

    private static ActivityRecord buildActivityRecord(String rct, String drt, String onl, String srv, String plc) {
        ActivityRecord ar = new ActivityRecord();
        ar.recordCreationTime = parseLocalDateTime(rct);
        ar.dataRetrievalTime = parseLocalDateTime(drt);
        ar.online = parseBoolean(onl);
        ar.server = srv;
        ar.playerCount = parseIntSafe(plc);
        return ar;
    }

    // ---------------------
    // Backup
    // ---------------------

    public void copyFilesToBakFiles() {
        Path activitySrc = Paths.get(activityRecordsFilePath);
        Path activityBak = Paths.get(activityRecordsFilePath + ".bak");
        try {
            Files.copy(activitySrc, activityBak, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.error("Failed to copy activity-records.csv file", e);
        }

        Path serversSrc = Paths.get(serversFilePath);
        Path serversBak = Paths.get(serversFilePath + ".bak");
        try {
            Files.copy(serversSrc, serversBak, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Log.error("Failed to copy servers.csv file", e);
        }
    }
}
