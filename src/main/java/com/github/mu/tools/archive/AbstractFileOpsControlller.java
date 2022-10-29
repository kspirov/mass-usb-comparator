package com.github.mu.tools.archive;

import static com.github.mu.tools.archive.ArchivingConstants.DEV_FOLDER_START;
import static com.github.mu.tools.archive.ArchivingConstants.UMOUNT_CMD;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.mu.tools.helpers.ConfigHelpers;
import com.google.common.annotations.VisibleForTesting;

import lombok.extern.slf4j.Slf4j;


@Slf4j
public abstract class AbstractFileOpsControlller implements Runnable {


    final ThreadPoolExecutor workerTaskPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(20);

    final InteractiveModeStatus model;

    final FoldersOpHelper helper;

    final ConfigHelpers configHelpers;
    final String masterPartition;
    final String partitionBases;
    final String masterFileStart;

    final String masterFileEnd;
    final String masterFileFilter;
    final String copyFilter;

    public AbstractFileOpsControlller(InteractiveModeStatus model,
                                      FoldersOpHelper copyFoldersHelper,
                                      ConfigHelpers configHelpers,
                                      @Value("${master.partition}") String masterPartition,
                                      @Value("${partition.bases}") String partitionBases,
                                      @Value("${master.file.start}") String masterFileStart,
                                      @Value("${master.file.end}") String masterFileEnd,
                                      @Value("${master.file.filter}") String masterFileFilter,
                                      @Value("${copy.filter}") String copyFilter) {

        this.model = model;
        this.helper = copyFoldersHelper;
        this.configHelpers = configHelpers;
        this.masterPartition = masterPartition;
        this.partitionBases = partitionBases;
        this.masterFileStart = masterFileStart;
        this.masterFileEnd = masterFileEnd;
        this.masterFileFilter = masterFileFilter;
        this.copyFilter = copyFilter;
    }


    String findDestinationFolderForPartition(String masterPath, String partitionIndex) {
        log.info("Find destination folder for masterName{} partitionIndex {} ", masterPath, partitionIndex);
        String masterName = new File(masterPath).getName();
        Map<String, String> bases = getPartitionBases();
        String pattern = bases.get(partitionIndex);
        String[] tokens = pattern.split("/");
        StringBuilder result = new StringBuilder();
        result.append(model.getBaseFolder());
        for (String token : tokens) {
            result.append("/");
            token = token.trim();
            if (token.startsWith("{") && token.endsWith("}")) {
                int ndx = Integer.parseInt(token.substring(1, token.length() - 1));
                if (ndx < 0) {
                    result.append(masterName, 0, masterName.length() + ndx);
                } else {
                    result.append(masterName, 0, ndx);
                }
            } else {
                result.append(token);
            }
        }
        return result.toString();
    }


    /**
     * Returns a map by device name and value - map by device index and mount point
     *
     * @return Result like {sda: {1: media/..., 2: media/...}}
     */
    public Map<String, Map<String, Path>> getCurrentMountedDevices() {
        String line = ArchivingConstants.LIST_PARTITIONS_CMD;
        CommandLine cmdLine = CommandLine.parse(line);
        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(baos, null));
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        executor.setExitValue(0);
        try {
            executor.execute(cmdLine);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String result = baos.toString();
        log.info("List partitions {} ",result);
        return getCurrentMountedDevicesPvt(result);

    }

    public void unmountDevice(InteractiveModeStatus.CopyWorkerStatus model, String displayName, String masterName)
            throws IOException {
        model.setOperation("Unmounting device");
        model.setSourceDevice(displayName);
        model.setOperationArguments(masterName);
        String osDeviceName = DEV_FOLDER_START + masterName;
        CommandLine cmdLine = CommandLine.parse(String.format(UMOUNT_CMD, osDeviceName));
        DefaultExecutor executor = new DefaultExecutor();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        executor.setStreamHandler(new PumpStreamHandler(baos, null));
        ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
        executor.setWatchdog(watchdog);
        executor.setExitValue(0);
        executor.execute(cmdLine);
    }


    Map<String, String> getPartitionBases() {
        log.info("Splitting partition bases of "+partitionBases);
        TreeMap<String, String> res = new TreeMap<>();
        String[] split = partitionBases.trim().split(",");
        for (String s : split) {
            String[] atoms = s.split(":");
            res.put(atoms[0], atoms[1]);
        }
        return res;
    }


    @VisibleForTesting
    Map<String, Map<String, Path>> getCurrentMountedDevicesPvt(String lsblkResult) {
        String[] entries = lsblkResult.split("\\r?\\n|\\r");
        TreeMap<String, String> allPartitionsMap = new TreeMap<>();
        Set<String> partitionsToCopy = getPartitionBases().keySet();
        for (String entry : entries) {
            entry = entry.trim();
            if (StringUtils.hasText(entry)) {
                String[] e = entry.split("\\s+");
                String first = e[0].trim();
                String last = e[e.length - 1].trim();
                if (last.startsWith(ArchivingConstants.MEDIA_FOLDER_START)) {
                    allPartitionsMap.put(first, last);
                }
            }
        }
        Map<String, Map<String, Path>> fullPartitionsResult = new TreeMap<>();
        nextPartition:
        for (Map.Entry<String, String> partition : allPartitionsMap.entrySet()) {
            if (partition.getKey().endsWith(masterPartition)) {
                Map<String, Path> partitionGroup = new TreeMap<>();
                String diskKey = partition.getKey().substring(
                        0, partition.getKey().length() - masterPartition.length());
                for (String currentPartitionIndex : partitionsToCopy) {
                    String partitionName = diskKey + currentPartitionIndex;
                    String mountPoint = allPartitionsMap.get(partitionName);
                    if (!StringUtils.hasText(mountPoint)) {
                        continue nextPartition;
                    }
                    partitionGroup.put(currentPartitionIndex, new File(mountPoint).toPath());
                }
                fullPartitionsResult.put(diskKey, partitionGroup);
            }
        }
        return fullPartitionsResult;
    }
}
