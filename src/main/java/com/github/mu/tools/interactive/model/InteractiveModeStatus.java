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

@Component
@Getter
@Setter
public class InteractiveModeStatus {

    private static final int MAX_ERROR_SIZE = 3;
    private static final int MAX_PREVIOUS_SIZE = 1;

    private volatile String operationTypeDisplayName = "copy operation";
    private volatile long startTimeMillis;
    private volatile boolean interactiveModeEnabled;
    private volatile AtomicInteger successfulPartitionCommand = new AtomicInteger();
    private volatile AtomicInteger successfulDiskCommand = new AtomicInteger();
    private volatile String baseFolder;
    private volatile ConcurrentHashMap<String, CopyWorkerStatus> currentWorkers = new ConcurrentHashMap<>();
    private volatile LinkedList<String> errors = new LinkedList<>();
    private volatile LinkedHashSet<String> successfulId = new LinkedHashSet<>();


    public void addError(String error) {
        errors.addLast(error);
        if (errors.size() > MAX_ERROR_SIZE) {
            errors.removeFirst();
        }
    }

    public void addSuccessfulId(String error) {
        successfulId.add(error);
        if (successfulId.size() > MAX_PREVIOUS_SIZE) {
            successfulId.iterator().remove();
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
