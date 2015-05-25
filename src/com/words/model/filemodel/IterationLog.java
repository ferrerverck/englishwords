package com.words.model.filemodel;

import com.words.controller.utils.DateTimeUtils;
import com.words.main.EnglishWords;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Properties;

public class IterationLog {
    
    public static final String LOG_FILE_NAME = "iterations";
    
    private final Path logFile;
    private final Properties props;
    
    public static void main(String[] args) {
        IterationLog il = new IterationLog(EnglishWords.PROJECT_DIRECTORY);
        
        String today = DateTimeUtils.todayAsString();
        System.out.println("0 = " + il.getIterationsForDays(0, today));
        System.out.println("1 = " + il.getIterationsForDays(1, today));
        System.out.println("2 = " + il.getIterationsForDays(2, today));
        System.out.println("7 = " + il.getIterationsForDays(7, today));
    }
    
    IterationLog(Path projectDir) {
        props = new Properties();
        
        logFile = projectDir.resolve(LOG_FILE_NAME);
        
        try (BufferedReader br = Files.newBufferedReader(logFile,
            StandardCharsets.UTF_8)) {
            props.load(br);
        } catch (IOException e) { }
    }
    
    synchronized int getIterations(String key) {
        try {
            return Integer.parseInt(props.getProperty(key, "0"));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
    
    synchronized void setIterations(String key, int iter) {
        props.put(key, "" + iter);
        
        try (BufferedWriter bw = Files.newBufferedWriter(logFile,
            StandardCharsets.UTF_8)) {
            props.store(bw, "Format: {date}={iterations}");
        } catch (IOException e) { }
    }
    
    // contract is the same as with model interface
    synchronized int getIterationsForDays(int n, String todayAsString) {
        if (n <= 0) return 0;
        // deadline is not included
        LocalDate today = DateTimeUtils.getCurrentLocalDate();
        LocalDate deadline = today.minusDays(n);
        
        return props.entrySet().stream().filter(entry -> {
            String key = entry.getKey().toString();
            if (!DateTimeUtils.isValidDate(key)) return false;
            
            LocalDate date = DateTimeUtils.parseDate(key);
            return date.isAfter(deadline) && date.isBefore(today);
        }).mapToInt(entry -> {
            try {
                return Integer.parseInt(entry.getValue().toString());
            } catch (NumberFormatException ex) {
                return 0;
            }
        }).sum();
    }
    
    void destroy() {
        try {
            Files.deleteIfExists(logFile);
        } catch (IOException ioe) { }
    }
}
