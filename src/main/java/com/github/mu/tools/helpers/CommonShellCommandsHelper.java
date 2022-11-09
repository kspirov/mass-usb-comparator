package com.github.mu.tools.helpers;

import static com.github.mu.tools.interactive.ArchivingConstants.DEV_FOLDER_START;
import static com.github.mu.tools.interactive.ArchivingConstants.LIST_PARTITIONS_CMD;
import static com.github.mu.tools.interactive.ArchivingConstants.SYNC_CMD;
import static com.github.mu.tools.interactive.ArchivingConstants.UDISK2_SERVICE_START;
import static com.github.mu.tools.interactive.ArchivingConstants.UDISK2_SERVICE_STOP;
import static com.github.mu.tools.interactive.ArchivingConstants.UDISKCTL_UMOUNT_CMD;
import static com.github.mu.tools.interactive.ArchivingConstants.UDISKCTL_POWER_OFF_CMD;
import static com.github.mu.tools.interactive.ArchivingConstants.UMOUNT_CMD;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.github.mu.tools.interactive.ArchivingConstants;
import com.github.mu.tools.interactive.model.InteractiveModeStatus;
import com.google.common.util.concurrent.Uninterruptibles;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class CommonShellCommandsHelper {


    private static final int EXECUTE_TIMEOUT_MILLIS = 50000;

    private static final Object EXECUTION_MUTEX = new Object();

    private final String masterPartition;

    public CommonShellCommandsHelper(@Value("${master.partition}") String masterPartition) {
        this.masterPartition = masterPartition;
    }


    private String executeCommand(String command, int maxRetry,
                                InteractiveModeStatus.CopyWorkerStatus model, String displayName, String masterName)
            throws IOException {
        int retryCount = 0;
        while (true) {
            try {
                synchronized (EXECUTION_MUTEX) {
                    retryCount++;
                    log.info("Executing {}, try {}", command, retryCount);
                    if (model != null) {
                        model.setOperation("Executing " + command + " try " + retryCount);
                        model.setSourceDevice(displayName);
                        model.setOperationArguments(masterName);
                    }
                    CommandLine cmdLine = CommandLine.parse(command);
                    DefaultExecutor executor = new DefaultExecutor();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    executor.setStreamHandler(new PumpStreamHandler(baos, null));
                    ExecuteWatchdog watchdog = new ExecuteWatchdog(EXECUTE_TIMEOUT_MILLIS);
                    executor.setWatchdog(watchdog);
                    executor.setExitValue(0);
                    executor.execute(cmdLine);
                    return baos.toString();
                }
            } catch (IOException e) {
                if (retryCount == maxRetry) {
                    throw e;
                } else {
                    log.warn(e.getMessage(), e);
                    Uninterruptibles.sleepUninterruptibly(retryCount, TimeUnit.SECONDS);
                }
            }
        }
    }

    public void startUdisk2Service() throws IOException {
        executeCommand(UDISK2_SERVICE_START, 2, null, null, null);
    }

    public void stopUdisk2Service() throws IOException {
        executeCommand(UDISK2_SERVICE_STOP, 2, null, null, null);
    }



    public void unmountPartition(InteractiveModeStatus.CopyWorkerStatus model, String[] partitionList)

            throws IOException {

        String blockDevice = partitionList[0].substring(0, partitionList[0].length()-1);
        executeCommand(SYNC_CMD, 4, model, blockDevice, blockDevice);
        startUdisk2Service();
        for (String currentPartition: partitionList) {
            String fullPartitionName = DEV_FOLDER_START + currentPartition;
            log.info("Unmounting partially {}", currentPartition);
            try {
                String cmdLine = String.format(UMOUNT_CMD, fullPartitionName);
                executeCommand(cmdLine, 6, model, blockDevice, currentPartition);
            } catch (IOException e) {
                log.info("Unmounting forcefully {}", currentPartition);
                log.error(e.getMessage(), e);
                String cmdLineAlt = String.format(UDISKCTL_UMOUNT_CMD, fullPartitionName);
                executeCommand(cmdLineAlt, 2, model, blockDevice, currentPartition);
            }
        }
        String fullDeviceName = DEV_FOLDER_START + blockDevice;
        String cmdLineFinal = String.format(UDISKCTL_POWER_OFF_CMD, fullDeviceName);
        executeCommand(cmdLineFinal, 5, model, blockDevice, blockDevice);
    }


    /**
     * Returns a map by device name and value - map by device index and mount point
     *
     * @return Result like {sda: {1: media/..., 2: media/...}}
     */
    public String getCurrentMountedPartitions() {
        String line = LIST_PARTITIONS_CMD;
        try {
            return executeCommand(LIST_PARTITIONS_CMD, 2, null, null, null);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            return "";
        }
    }


    public Map<String, Map<String, Path>> getCurrentMountedDevices(String lsblkResult,
                                                                   Set<String> partitionBases) {
        if (!StringUtils.hasText(lsblkResult)) {
            return new HashMap<>();
        }
        String[] entries = lsblkResult.split("\\r?\\n|\\r");
        TreeMap<String, String> allPartitionsMap = new TreeMap<>();
        Set<String> partitionsToCopy = partitionBases;
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
