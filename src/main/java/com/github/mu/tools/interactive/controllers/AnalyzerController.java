package com.github.mu.tools.interactive.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.github.mu.tools.helpers.CommonShellCommandsHelper;
import com.github.mu.tools.helpers.ConfigHelpers;
import com.github.mu.tools.interactive.OperationMode;
import com.github.mu.tools.interactive.model.AnalyzerStatus;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;


@Slf4j
@Component
public class AnalyzerController implements Runnable {


    private final InteractiveModeStatus model;

    private final FoldersOpHelper helper;

    private final ConfigHelpers configHelpers;
    private final String masterPartition;
    private final String partitionBases;
    private final String copyFilter;

    private final AnalyzerStatus analyzerModel;


    private final CommonShellCommandsHelper shellCommandsHelper;

    public AnalyzerController(AnalyzerStatus analyzerModel,
                              InteractiveModeStatus model,
                              FoldersOpHelper copyFoldersHelper,
                              CommonShellCommandsHelper shellCommandsHelper,
                              ConfigHelpers configHelpers,
                              @Value("${master.partition}") String masterPartition,
                              @Value("${partition.bases}") String partitionBases,
                              @Value("${copy.filter}") String copyFilter) {

        this.analyzerModel = analyzerModel;
        this.model = model;
        this.helper = copyFoldersHelper;
        this.configHelpers = configHelpers;
        this.shellCommandsHelper = shellCommandsHelper;
        this.masterPartition = masterPartition;
        this.partitionBases = partitionBases;
        this.copyFilter = copyFilter;
    }




    /**
     * Returns a map by device name and value - map by device index and mount point
     *
     * @return Result like {sda: {1: media/..., 2: media/...}}
     */
    public Map<String, Map<String, Path>> getCurrentMountedDevices() {
        String result = shellCommandsHelper.getCurrentMountedPartitions();
        return shellCommandsHelper.getCurrentMountedDevices(result, AbstractFileOpsController.getSplitPartitionBases(partitionBases).keySet());
    }



    public void run() {

        Map<String, String> usedDevices = Collections.synchronizedMap(new HashMap<String, String>());
        Map<String, Map<String, Path>> mounted;
        Set<String> partitions = AbstractFileOpsController.getSplitPartitionBases(partitionBases).keySet();
        while (!usedDevices.isEmpty() || model.isInteractiveModeEnabled()) {
            mounted = getCurrentMountedDevices();
            ArrayList<AnalyzerStatus.AnalysedDisk> disks = new ArrayList<>();
            for (Map.Entry<String, Map<String, Path>> entry : mounted.entrySet()) {
                Path masterPartitionPath = entry.getValue().get(masterPartition);
                File masterFile = configHelpers.findMasterFile(masterPartitionPath.toFile());
                AnalyzerStatus.AnalysedDisk disk = new AnalyzerStatus.AnalysedDisk();
                disk.setDiskName(entry.getKey());
                disk.setRightPartitionNumber(partitions.size() == entry.getValue().size());
                if (masterFile != null) {
                    disk.setMasterName(masterFile.getName());
                    disk.setEmpty(false);
                } else {
                    disk.setMasterName("N/A");


                }
                boolean empty = true;
                loop:
                for (Path p: entry.getValue().values()) {
                    File[] paths = p.toFile().listFiles();
                    if (paths != null && paths.length>0) {
                        empty = false;
                        break loop;
                    }
                }
                disk.setEmpty(empty);
                disks.add(disk);
            }
            synchronized (AnalyzerStatus.ANALYZER_MODEL_MUTEX) {
                analyzerModel.getAnalysedDiskMap().clear();
                for (AnalyzerStatus.AnalysedDisk  d : disks) {
                    analyzerModel.getAnalysedDiskMap().put(d.getDiskName(), d);
                }
            }
        }

        log.info("bye.");
    }
}
