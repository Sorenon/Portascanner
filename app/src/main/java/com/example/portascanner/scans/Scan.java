package com.example.portascanner.scans;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public class Scan {
    public String title;
    public String desc;
    public long unixTimestamp;
    public ScanData scanData;

    public static String getFormattedTimestamp(long unixTimestamp) {
        return LocalDateTime.ofEpochSecond(unixTimestamp, 0, ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
