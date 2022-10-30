package com.github.mu.tools.interactive.model;

import java.util.LinkedHashSet;
import java.util.LinkedList;
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

    private static final int MAX_ERROR_SIZE = 3;
    private static final int MAX_PREVIOUS_MEDIA_ID_SIZE = 8;

    private static final int MAX_ERROR_MEDIA_ID_SIZE = 8;

    private volatile String operationTypeDisplayName = "copy operation";
    private volatile long startTimeMillis;
    private volatile boolean interactiveModeEnabled;
    private volatile AtomicInteger successfulPartitionCommand = new AtomicInteger();
    private volatile AtomicInteger successfulDiskCommand = new AtomicInteger();
    private volatile String baseFolder;
    private volatile ConcurrentHashMap<String, CopyWorkerStatus> currentWorkers = new ConcurrentHashMap<>();
    private volatile LinkedList<String> errors = new LinkedList<>();
    private volatile LinkedHashSet<String> successfulId = new LinkedHashSet<>();
    private volatile LinkedHashSet<String> errorId = new LinkedHashSet<>();

    public void addError(String error) {
        errors.addLast(error);
        if (errors.size() > MAX_ERROR_SIZE) {
            errors.removeFirst();
        }
    }

    public void addSuccessfulId(String success) {
        log.info("Success in {}", success);
        successfulId.add(success);
        if (successfulId.size() > MAX_PREVIOUS_MEDIA_ID_SIZE) {
            successfulId.iterator().remove();
        }
    }

    public void addErrorId(String error) {
        log.error("Error in {}", error);
        errorId.add(error);
        if (errorId.size() > MAX_ERROR_MEDIA_ID_SIZE) {
            errorId.iterator().remove();
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
