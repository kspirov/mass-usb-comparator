package com.github.mu.tools.interactive.model;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@Getter
@Setter
@Slf4j
public class InteractiveModeStatus {

    private static final int MAX_PREVIOUS_MEDIA_ID_SIZE = 20;

    private static final int MAX_ERROR_MEDIA_ID_SIZE = 8;

    private volatile String operationTypeDisplayName = "copy operation";
    private volatile long startTimeMillis;
    private volatile boolean interactiveModeEnabled;
    private volatile int maxErrorSize;
    private volatile AtomicInteger successfulPartitionCommand = new AtomicInteger();
    private volatile AtomicInteger successfulDiskCommand = new AtomicInteger();
    private volatile List<String> baseFolders;
    private volatile ConcurrentHashMap<String, CopyWorkerStatus> currentWorkers = new ConcurrentHashMap<>();
    private volatile LinkedHashSet<String> errors = new LinkedHashSet<>();
    private volatile LinkedHashSet<String> successfulId = new LinkedHashSet<>();
    private volatile LinkedHashSet<String> errorId = new LinkedHashSet<>();

    public void addError(String error) {
        log.error("Added error {} ", error);
        if (maxErrorSize>0) {
            errors.add(error);
            if (errors.size() > maxErrorSize) {
                Iterator<String> i = errors.iterator();
                i.next();
                i.remove();
            }
        }
    }

    public void addSuccessfulId(String success) {
        log.info("Success in {}", success);
        successfulId.add(success);
        if (successfulId.size() > MAX_PREVIOUS_MEDIA_ID_SIZE) {
            Iterator<String> i = successfulId.iterator();
            i.next();
            i.remove();
        }
    }

    public void addErrorId(String error) {
        log.error("Error in {}", error);
        errorId.add(error);
        if (errorId.size() > MAX_ERROR_MEDIA_ID_SIZE) {
            Iterator<String> i = errorId.iterator();
            i.next();
            i.remove();
        }
    }

    @Data
    @Builder
    public static class CopyWorkerStatus {
        private transient String sourceDevice;
        private transient String operation;
        private transient String operationArguments;
    }


}
