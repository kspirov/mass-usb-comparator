package com.github.mu.tools.interactive.model;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.Getter;

@Component
public class AnalyzerStatus {

    public static final Object ANALYZER_MODEL_MUTEX = new Object();
    @Getter
    private Map<String, AnalysedDisk> analysedDiskMap = new TreeMap<>();

    @Data
    public static class AnalysedDisk {
        private String diskName;
        private String masterName;
        private boolean rightPartitionNumber;
        private boolean isEmpty;
    }
}
