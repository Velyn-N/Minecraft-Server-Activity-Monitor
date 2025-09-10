package me.velyn.mcactivitymonitor.data;

import java.time.*;

public class ActivityRecord {
    public LocalDateTime recordCreationTime;
    public LocalDateTime dataRetrievalTime;
    public boolean online;
    public String server;
    public int playerCount;
}
