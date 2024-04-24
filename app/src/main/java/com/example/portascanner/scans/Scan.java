package com.example.portascanner.scans;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Scan {
    public String title;
    public String desc;
    public long unixTimestamp;
    public ScanResults scanResults;

    public static String getFormattedTimestamp(long unixTimestamp) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        return LocalDateTime.ofEpochSecond(unixTimestamp, 0, ZoneOffset.UTC).format(dateFormatter);
    }

    public static String getLocalisedFormattedTimestamp(long unixTimestamp) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT);
        return LocalDateTime.ofEpochSecond(unixTimestamp, 0, ZoneOffset.UTC).format(dateFormatter);
    }
}
