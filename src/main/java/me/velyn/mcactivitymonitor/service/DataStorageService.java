package me.velyn.mcactivitymonitor.data;

import java.util.*;

import org.eclipse.microprofile.config.inject.*;

import jakarta.enterprise.context.*;

@ApplicationScoped
public class DataService {

    @ConfigProperty(name = "storage.file.servers")
    String serversFilePath;

    @ConfigProperty(name = "storage.file.activity.records")
    String activityRecordsFilePath;

    public void writeActivityRecord(ActivityRecord rec) {
        // TODO write the record by appending to a file
    }

    public List<ActivityRecord> getActivityRecordsForServer(String server) {
        // TODO read the activity-records.csv file line by line.
        //  If the line is for the specified server add it to the list, otherwise dont load it into ram
        return new ArrayList<>();
    }

    public Set<ServerRecord> getServers() {
        // TODO return all data from the servers.csv file (cached)
        return new HashSet<>();
    }

    public void writeServerRecord(ServerRecord record) {
        // TODO update the servers.csv file with the modified record
    }
}
